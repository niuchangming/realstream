package controllers.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import controllers.HomeController;
import modelVOs.AccountVO;
import modelVOs.PaymentVO;
import models.Account;
import models.ResponseData;
import models.User;
import play.cache.CacheApi;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPA;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import tools.Constants;
import tools.Utils;

@SuppressWarnings("unchecked")
public class ProfileController extends Controller{
	@Inject private JPAApi jpaApi;
	@Inject private FormFactory formFactory;
	@Inject private MessagesApi messagesApi;
	@Inject private CacheApi cache;
	
	@Transactional
	public Result updateName(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String token = requestData.get("token");
		String name = requestData.get("name");
		try {
			Account account = jpaApi.em().createQuery("from Account a where a.token = :token", Account.class)
			.setParameter("token", token)
			.getSingleResult();
			
			if(!Utils.isBlank(name)){
				User user = account.user;
				user.username = name;
				jpaApi.em().persist(user);
			}else{
				responseData.message = "Name cannot be empty.";
				responseData.code = 4000;
			}
		} catch(NoResultException e){
			responseData.message = "Account doesn't exist.";
			responseData.code = 4000;
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result updateMobile(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String token = requestData.get("token");
		String mobile = requestData.get("mobile");
		int dialCode = Integer.parseInt(requestData.get("dialCode"));
		
		try {
			mobile = mobile.replaceAll("\\s+","");
			
			jpaApi.em().createQuery("from Account a where a.token = :token", Account.class)
			.setParameter("token", token)
			.getSingleResult();
			
			if(HomeController.notExists(dialCode, mobile)){
				Messages messages = messagesApi.preferred(request());
				
				int verifyCode = Utils.genernateActiveCode();
				String mobileWithDialCode = dialCode + mobile;
				String msg = messages.at("message.otp_sms") + " " + verifyCode;
				
				CompletableFuture.supplyAsync(() -> Utils.sendSMS(mobileWithDialCode, msg));
				cache.set(Account.MOBILE_PREFIX + mobileWithDialCode, verifyCode, 2 * 60);
				
				Account accountTmp = new Account();
				accountTmp.mobile = mobile;
				accountTmp.dialCode = dialCode;
				
				responseData.data = new AccountVO(accountTmp);
			}else{
				responseData.code = 4000;
				responseData.message = "The mobile number was already registered.";
			}
		} catch(NoResultException e){
			responseData.message = "Account doesn't exist.";
			responseData.code = 4000;
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result verifyMobile(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		int dialCode = Integer.parseInt(requestData.get("dialCode"));
		int verifyCode = Integer.parseInt(requestData.get("verifyCode"));
		String mobile = requestData.get("mobile");
		String token = requestData.get("token"); 
		
		try{
			mobile = mobile.replaceAll("\\s+","");
			
			Account account = jpaApi.em().createQuery("from Account a where a.token = :token", Account.class)
			.setParameter("token", token)
			.getSingleResult();
			
			String mobileWithDialCode = dialCode + mobile;
			int verifyCodeCached = 0;
			if(cache != null){
				verifyCodeCached = cache.get(Account.MOBILE_PREFIX + mobileWithDialCode);
			}
				
			if(verifyCodeCached == 0){
				responseData.message = "Verify code has expired.";
				responseData.code = 4000;
			}else if(verifyCodeCached == verifyCode){
				account.mobile = mobile;
				account.dialCode = dialCode;
				JPA.em().persist(account);
				responseData.data = new AccountVO(account);
			}else{
				responseData.message = "Verify code is incorrect.";
				responseData.code = 4000;
			}
		}catch(NoResultException e){
			responseData.message = "Account doesn't exist.";
			responseData.code = 4000;
		}
	
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result paymentHistory(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String token = requestData.get("token"); 
		int offset = Integer.parseInt(requestData.get("offset"));
		
		try{
			Account account = jpaApi.em().createQuery("from Account a where a.token = :token", Account.class)
					.setParameter("token", token)
					.getSingleResult();
			
			List<Object[]> resultList = jpaApi.em()
					.createNativeQuery("SELECT ls.id, ls.title, p.transaction_id, p.amount, p.currency, p.pay_datetime, p.status FROM payment p LEFT JOIN lesson ls ON p.lesson_id=ls.id WHERE p.user_id= :userId")
					.setParameter("userId", account.id)
					.setFirstResult(offset)
					.setMaxResults(Constants.PAYMENT_PAGE_SIZE)
					.getResultList();
			
			if(resultList != null){
				List<PaymentVO> paymentVOs = new ArrayList<>();
				for(Object[] result : resultList){
					//因为是 LEFT JOIN, 所以lesson的ID和title可能为空
					PaymentVO paymentVO = new PaymentVO(Long.parseLong(result[0] == null ? "0" : result[0].toString()), result[1]==null ? "" : result[1].toString(), 
							result[2].toString(), Double.parseDouble(result[3].toString()), result[4].toString(), result[5].toString(), result[5].toString());
					paymentVOs.add(paymentVO);
				}
				responseData.data = paymentVOs;
			}
		}catch(NoResultException e){
			responseData.code = 4000;
			responseData.message = "User does not exist.";
    	}
		
		return ok(Json.toJson(responseData));
	}

}
