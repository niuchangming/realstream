package controllers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import com.fasterxml.jackson.databind.JsonNode;
import com.paypal.api.payments.Amount;
import com.paypal.api.payments.CreditCard;
import com.paypal.api.payments.FundingInstrument;
import com.paypal.api.payments.Item;
import com.paypal.api.payments.ItemList;
import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payer;
import com.paypal.api.payments.PaymentExecution;
import com.paypal.api.payments.RedirectUrls;
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
import tools.MD5;
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
				CompletionStage<WSResponse> responsePromise = ws.url(Constants.REST_LIVE_ENDPOINT + "v1/oauth2/token")
						.setContentType("application/x-www-form-urlencoded")
						.setAuth(Payment.PAYPAL_CLIENT_ID , Payment.PAYPAL_SECRET, WSAuthScheme.BASIC)
						.setQueryParameter("grant_type", "client_credentials")
						.post("");
				
				try {
					JsonNode jsonResult = responsePromise.thenApply(WSResponse::asJson).toCompletableFuture().get();
					String accessToken = jsonResult.get("access_token").asText();
					if(!Utils.isBlank(accessToken)){
						com.paypal.api.payments.Payment paymentResult = submitCard(accessToken, requestData, lesson);
						if(paymentResult != null){
							Payment paymentObj = new Payment(user, lesson);
							
							Transaction transcation = paymentResult.getTransactions().get(0);
							paymentObj.transactionId = paymentResult.getId();
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
		if(lesson.offers.size() > 0){
			if(lesson.offers.get(0).offerPrice > 0 
					&& (lesson.offers.get(0).offerStartDate == null 
					|| lesson.offers.get(0) == null) 
					|| (lesson.offers.get(0).offerPrice > 0 && !lesson.offers.get(0).offerStartDate.before(now) && !lesson.offers.get(0).offerEndDate.after(now))){
				amount.setTotal(df.format(lesson.offers.get(0).offerPrice));
			}else{
				amount.setTotal(df.format(lesson.price));
			}
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
		
		APIContext apiContext = new APIContext(Payment.PAYPAL_CLIENT_ID, Payment.PAYPAL_SECRET, Constants.LIVE);
		createdPayment = payment.create(apiContext);
		
		if(createdPayment.getState().equals("approved")){
			return createdPayment;
		}
		
		return null;
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result requestPaypal(long lessonId) throws PayPalRESTException{
		ResponseData responseData = new ResponseData();
		
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
		if(lesson != null){
			DecimalFormat df = new DecimalFormat("#.00");  
			
			Amount amount = new Amount();
			amount.setCurrency("USD");
			amount.setTotal(df.format(lesson.price));
			
			Date now = new Date();
			if(lesson.offers.size() > 0){
				if(lesson.offers.get(0).offerPrice > 0 
						&& (lesson.offers.get(0).offerStartDate == null 
						|| lesson.offers.get(0).offerEndDate == null) 
						|| (lesson.offers.get(0).offerPrice > 0 && !lesson.offers.get(0).offerStartDate.before(now) && !lesson.offers.get(0).offerEndDate.after(now))){
					amount.setTotal(df.format(lesson.offers.get(0).offerPrice));
				}
			}
			
			Item item = new Item();
			item.setName(lesson.title).setQuantity("1").setCurrency(amount.getCurrency()).setPrice(amount.getTotal());
			ItemList itemList = new ItemList();
			List<Item> items = new ArrayList<Item>();
			items.add(item);
			itemList.setItems(items);
			
			Transaction transaction = new Transaction();
			transaction.setAmount(amount);
			transaction.setItemList(itemList);
			transaction.setDescription("Above is the lesson transaction description.");
			
			List<Transaction> transactions = new ArrayList<Transaction>();
			transactions.add(transaction);
			
			Payer payer = new Payer();
			payer.setPaymentMethod("paypal");
			
			Account account = (Account) ctx().args.get("account");
			RedirectUrls redirectUrls = new RedirectUrls();
			redirectUrls.setCancelUrl("https://ekooedu.com/");
			redirectUrls.setReturnUrl("https://ekooedu.com/payment/execute/paypal?userId=" + account.id + "&lessonId=" + lessonId);

			com.paypal.api.payments.Payment payment = new com.paypal.api.payments.Payment();
			payment.setIntent("sale");
			payment.setPayer(payer);
			payment.setTransactions(transactions);
			payment.setRedirectUrls(redirectUrls);
			
			APIContext apiContext = new APIContext(Payment.PAYPAL_CLIENT_ID, Payment.PAYPAL_SECRET, Constants.LIVE);
			com.paypal.api.payments.Payment createdPayment = payment.create(apiContext);
			if(createdPayment.getState().equals("created")){
				Iterator<Links> links = createdPayment.getLinks().iterator();
				while (links.hasNext()) {
					Links link = links.next();
					if (link.getRel().equalsIgnoreCase("approval_url")) {
						return redirect(link.getHref());
					}
				}
			}else{
				responseData.code = 4000;
				responseData.message = "Something goes wrong with paypal flow.";
			}
		}else{
			responseData.code = 4000;
			responseData.message = "The lesson does not exist.";
		}
		
		return notFound(errorpage.render(responseData));
	}
	
	@Transactional
	public Result executePaypal(long userId, long lessonId){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String PayerID = requestData.get("PayerID");
		String paymentId = requestData.get("paymentId");
		
		com.paypal.api.payments.Payment payment = new com.paypal.api.payments.Payment();
		payment.setId(paymentId);
		
		PaymentExecution paymentExecution = new PaymentExecution();
		paymentExecution.setPayerId(PayerID);
		
		try {
			APIContext apiContext = new APIContext(Payment.PAYPAL_CLIENT_ID, Payment.PAYPAL_SECRET, Constants.LIVE);
			com.paypal.api.payments.Payment createdPayment = payment.execute(apiContext, paymentExecution);
			
			if(createdPayment.getState().equals("approved")){
				User user = jpaApi.em().find(User.class, userId);
				Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);

				if(user == null){
					responseData.code = 4000;
					responseData.message = "User does not exist.";
				}else if(lesson == null){
					responseData.code = 4000;
					responseData.message = "Lesson does not exist.";
				}else{
					Payment paymentObj = new Payment(user, lesson);
					paymentObj.transactionId = createdPayment.getId();
					paymentObj.method = "PayPal";
					paymentObj.status = createdPayment.getState();
					paymentObj.amount = createdPayment.getTransactions().get(0).getAmount().getTotal();
					paymentObj.currency = createdPayment.getTransactions().get(0).getAmount().getCurrency();
					jpaApi.em().persist(paymentObj);
					
					UserLesson userLesson = new UserLesson(user, lesson);
					jpaApi.em().persist(userLesson);
					
					flash("success", "Payment completed successfully.");
					return redirect(routes.LessonController.lessonDetail(lessonId));
				}
			}else{
				responseData.code = 4000;
				responseData.message = "Payment failed.";
			}
		} catch (PayPalRESTException e) {
			responseData.code = 4001;
			responseData.message = e.getLocalizedMessage();
		}
		
		return notFound(errorpage.render(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result requestAlipay(long lessonId){
		ResponseData responseData = new ResponseData();
		
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
		if(lesson != null){
			DecimalFormat df = new DecimalFormat("#.00"); 
			String transactionId = "PAY-" + Utils.gen(24);

			String totalFee = df.format(lesson.price);
			Date now = new Date();
			if(lesson.offers.size() > 0){
				if(lesson.offers.get(0).offerPrice > 0 
						&& (lesson.offers.get(0).offerStartDate == null 
						|| lesson.offers.get(0).offerEndDate == null) 
						|| (lesson.offers.get(0).offerPrice > 0 && !lesson.offers.get(0).offerStartDate.before(now) && !lesson.offers.get(0).offerEndDate.after(now))){
					totalFee = df.format(lesson.offers.get(0).offerPrice);
				}
			}
			
			Map<String, String> reqParams = new HashMap<String, String>();
			try {
				reqParams.put("subject", lesson.title);
				reqParams.put("out_trade_no", transactionId);
				reqParams.put("currency", "SGD");
				reqParams.put("total_fee", totalFee);
				reqParams.put("partner", Payment.ALIPAY_PARTNER_ID);
				reqParams.put("notify_url", "https://ekooedu.com/payment/execute/alipay");
				reqParams.put("return_url", "https://ekooedu.com/lesson?lessonId=" + lesson.id);
				reqParams.put("sendFormat", "normal");
				reqParams.put("_input_charset", "UTF-8");
				reqParams.put("service", "create_forex_trade");
				
		        String sign = MD5.md5(Utils.createLinkString(reqParams, Payment.ALIPAY_CODE));
				
		        String alipayLink = Payment.ALIPAY_GATEWAY_NEW;
		        List<String> keys = new ArrayList<String>(reqParams.keySet());
		        for (int i = 0; i < keys.size(); i++) {
		            alipayLink = alipayLink + keys.get(i) + "=" + URLEncoder.encode((String) reqParams.get(keys.get(i)), "utf-8") + "&";
		        }
		        alipayLink = alipayLink + "sign=" + sign + "&sign_type=MD5";
		        responseData.data = alipayLink;
		        
		        Account account = (Account) ctx().args.get("account");
				TypedQuery<User> query = jpaApi.em()
						.createQuery("from User u where u.accountId = :accountId", User.class)
						.setParameter("accountId", account.id);
				User user = query.getSingleResult();
				
		        Payment paymentObj = new Payment(user, lesson);
				paymentObj.transactionId = transactionId;
				paymentObj.method = "AliPay";
				paymentObj.status = "Pending";
				paymentObj.amount = totalFee;
				paymentObj.currency = "SGD";
				jpaApi.em().persist(paymentObj);
				
		        return ok(Json.toJson(responseData));
			} catch (UnsupportedEncodingException | NoResultException e) {
				responseData.code = 4001;
				responseData.message = e.getLocalizedMessage();
			}
		}else{
			responseData.code = 4000;
			responseData.message = "The lesson does not exist.";
		}
		return notFound(errorpage.render(responseData));
	}
	
	@Transactional
	public Result executeAlipay(){
		ResponseData responseData = new ResponseData(); 
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String transactionId = requestData.get("out_trade_no");
		String tradeStatus = requestData.get("trade_status");
		
		try {
			Payment paymentObj = jpaApi.em().createQuery("FROM Payment py WHERE py.transactionId=:transactionId", Payment.class)
					.setParameter("transactionId", transactionId).getSingleResult();
			paymentObj.notifyId = requestData.get("notify_id");
			paymentObj.status = tradeStatus;
			jpaApi.em().persist(paymentObj);
			
			UserLesson userLesson = new UserLesson(paymentObj.user, paymentObj.lesson);
			jpaApi.em().persist(userLesson);
			
			CompletionStage<WSResponse> responsePromise = ws.url(Payment.ALIPAY_GATEWAY_NEW)
					.setQueryParameter("partner", Payment.ALIPAY_PARTNER_ID)
					.setQueryParameter("notify_id", paymentObj.notifyId)
					.setQueryParameter("service", "notify_verify")
					.get();
			
			JsonNode jsonResult = responsePromise.thenApply(WSResponse::asJson).toCompletableFuture().get();
			System.out.println("--------> " + jsonResult.toString());
		} catch (InterruptedException | NoResultException | ExecutionException e) {
			responseData.code = 4001;
			responseData.message = e.getLocalizedMessage();
		}
		
		return ok(Json.toJson(responseData));
	}
	
}


























