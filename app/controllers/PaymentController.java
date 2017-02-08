package controllers;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import com.fasterxml.jackson.databind.JsonNode;
import com.paypal.api.payments.Amount;
import com.paypal.api.payments.CreditCard;
import com.paypal.api.payments.Details;
import com.paypal.api.payments.FundingInstrument;
import com.paypal.api.payments.Payer;
import com.paypal.api.payments.Transaction;
import com.paypal.base.Constants;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;

import actions.AuthAction;
import models.Account;
import models.Lesson;
import models.Payment;
import models.ResponseData;
import models.User;
import models.UserLesson;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.libs.ws.WSAuthScheme;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import tools.Utils;
import views.html.errorpage;
import views.html.paymentreview;

public class PaymentController extends Controller{
	@Inject private JPAApi jpaApi;
	@Inject private FormFactory formFactory;
	@Inject private WSClient ws;

	@With(AuthAction.class)
	@Transactional
	public Result paymentReview(long lessonId){
		ResponseData responseData = new ResponseData();
		
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
    	if(lesson == null){
    		responseData.message = "Lesson cannot be found.";
    		responseData.code = 4000;
    		return notFound(errorpage.render(responseData));
    	}
    	
    	Account account = (Account) ctx().args.get("account");
		TypedQuery<User> userQuery = jpaApi.em()
				.createQuery("from User u where u.accountId = :accountId", User.class)
				.setParameter("accountId", account.id);
		User user = userQuery.getSingleResult();
		
		try{
			UserLesson userlesson = jpaApi.em()
					.createQuery("from UserLesson us where us.user = :user and us.lesson = :lesson", UserLesson.class)
					.setParameter("user", user)	
					.setParameter("lesson", lesson)
					.getSingleResult();
			
			if(userlesson.isDeleted){
				responseData.code = 5000;
				responseData.message = "The lesson already in your deleted lesson list.";
			}else if(userlesson.isCompleted){
				responseData.code = 5000;
				responseData.message = "You completed this lesson.";
			}else{
				responseData.code = 5000;
				responseData.message = "You already added this lesson.";
			}
			flash("warning", responseData.message);
			return redirect(routes.LessonController.lessonDetail(lessonId));
		}catch(NoResultException e){
			return ok(paymentreview.render(lesson));
		}
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result submitCardInfo(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		long lessonId = Long.parseLong(requestData.get("lessonId"));
		
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
		if(lesson == null){
			responseData.code = 4000;
			responseData.message = "Lesson cannot be found.";
		}else{
			Account account = (Account) ctx().args.get("account");
			TypedQuery<User> userQuery = jpaApi.em()
					.createQuery("from User u where u.accountId = :accountId", User.class)
					.setParameter("accountId", account.id);
			User user = userQuery.getSingleResult();
			
			int count = ((Long)jpaApi.em()
					.createQuery("select count(*) from UserLesson ul where ul.lesson = :lesson and ul.user = :user")
					.setParameter("lesson", lesson)
					.setParameter("user", user).getSingleResult()).intValue();
			
			if(count == 0){
				CompletionStage<WSResponse> responsePromise = ws.url("https://api.sandbox.paypal.com/v1/oauth2/token")
						.setContentType("application/x-www-form-urlencoded")
						.setAuth(Payment.CLIENT_ID , Payment.SECRET, WSAuthScheme.BASIC)
						.setQueryParameter("grant_type", "client_credentials")
						.post("");
				
				try {
					JsonNode jsonResult = responsePromise.thenApply(WSResponse::asJson).toCompletableFuture().get();
					String accessToken = jsonResult.get("access_token").asText();
					if(!Utils.isBlank(accessToken)){
						com.paypal.api.payments.Payment paymentResult = submitCard(accessToken, requestData, lesson);
						if(paymentResult != null){
							Payment paymentObj = new Payment(user);
							
							CreditCard creditCard = paymentResult.getPayer().getFundingInstruments().get(0).getCreditCard();
							Transaction transcation = paymentResult.getTransactions().get(0);
							paymentObj.transactionId = paymentResult.getId();
							paymentObj.firstName = creditCard.getFirstName();
							paymentObj.lastName = creditCard.getLastName();
							paymentObj.state = paymentResult.getState();
							paymentObj.cardType = creditCard.getType();
							paymentObj.cardNumber = creditCard.getNumber();
							paymentObj.expMonth = creditCard.getExpireMonth();
							paymentObj.expYear = creditCard.getExpireYear();
							paymentObj.amount = transcation.getAmount().getTotal();
							paymentObj.currency = transcation.getAmount().getCurrency();
							jpaApi.em().persist(paymentObj);
							
							UserLesson userLesson = new UserLesson(user, lesson);
							jpaApi.em().persist(userLesson);
						}else{
							responseData.message = "Failed to pay.";
							responseData.code = 4001;
						}
					}else{
						responseData.message = "Failed to get payment token.";
						responseData.code = 4001;
					}
				} catch (Exception e) {
					responseData.message = e.getLocalizedMessage();
					responseData.code = 4001;
				}
			}else{
				responseData.message = "You have already add this lesson.";
				responseData.code = 5000;
			}
		}
		return ok(Json.toJson(responseData));
	}
	
	public com.paypal.api.payments.Payment submitCard(String accessToken, DynamicForm requestData, Lesson lesson) throws PayPalRESTException{
		String firstName = requestData.get("firstName");
		String lastName = requestData.get("lastName");
		String cardNumber = requestData.get("cardNumber");
		String cardType = requestData.get("cardType");
		int expMonth = Integer.parseInt(requestData.get("expMonth"));
		int expYear = Integer.parseInt(requestData.get("expYear"));
		String cvv = requestData.get("cvv");
		
		CreditCard creditCard = new CreditCard();
		creditCard.setCvv2(cvv);
		creditCard.setExpireMonth(expMonth);
		creditCard.setExpireYear(expYear);
		creditCard.setFirstName(firstName);
		creditCard.setLastName(lastName);
		creditCard.setNumber(cardNumber);
		creditCard.setType(cardType);
		
		Amount amount = new Amount();
		amount.setCurrency("USD");
		
		DecimalFormat df = new DecimalFormat("#.00");  
		Date now = new Date();
		if(lesson.offerPrice > 0 && (lesson.offerStartDate == null || lesson.offerEndDate == null) || (lesson.offerPrice > 0 && !lesson.offerStartDate.before(now) && !lesson.offerEndDate.after(now))){
			amount.setTotal(df.format(lesson.offerPrice));
		}else{
			amount.setTotal(df.format(lesson.price));
		}
		
		Transaction transaction = new Transaction();
		transaction.setAmount(amount);
		
		List<Transaction> transactions = new ArrayList<Transaction>();
		transactions.add(transaction);
		
		FundingInstrument fundingInstrument = new FundingInstrument();
		fundingInstrument.setCreditCard(creditCard);
		
		List<FundingInstrument> fundingInstrumentList = new ArrayList<FundingInstrument>();
		fundingInstrumentList.add(fundingInstrument);
		
		Payer payer = new Payer();
		payer.setFundingInstruments(fundingInstrumentList);
		if(cardType.equals("paypal")){
			payer.setPaymentMethod("paypal");
		}else{
			payer.setPaymentMethod("credit_card");
		}
		
		com.paypal.api.payments.Payment payment = new com.paypal.api.payments.Payment();
		payment.setIntent("sale");
		payment.setPayer(payer);
		payment.setTransactions(transactions);
		com.paypal.api.payments.Payment createdPayment = null;
		
		APIContext apiContext = new APIContext(Payment.CLIENT_ID, Payment.SECRET, Constants.SANDBOX);
		createdPayment = payment.create(apiContext);
		
		if(createdPayment.getState().equals("approved")){
			return createdPayment;
		}
		
		return null;
	}
}










