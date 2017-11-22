package controllers.api;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import controllers.HomeController;
import modelVOs.AccountVO;
import modelVOs.ProfileVO;
import models.Account;
import models.ResponseData;
import models.User;
import play.cache.CacheApi;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import tools.Utils;

public class AuthenticationController extends Controller{
	@Inject private MessagesApi messagesApi;
	@Inject private FormFactory formFactory;
	@Inject private CacheApi cache;
	
	@Transactional
    public Result login() {
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		int dialCode = Integer.parseInt(requestData.get("dialCode"));
		String mobile = requestData.get("mobile");
		String password = requestData.get("password");
		
		TypedQuery<Account> query = JPA.em().createQuery("from Account ac where ac.mobile = :mobile and ac.dialCode = :dialCode", Account.class)
		        .setParameter("mobile", mobile.replaceAll("\\s+",""))
		        .setParameter("dialCode", dialCode);
		try{
			Account account = query.getSingleResult();
			
			if(!Utils.md5(password).equals(account.password)){
				responseData.message = "The passowrd is incorrect.";
				responseData.code = 4000;
			}else{
				responseData.data = new AccountVO(account);
			}
		}catch(NoResultException e){
			responseData.message = "Account does not exist.";
			responseData.code = 4000;
		}
		
		return ok(Json.toJson(responseData));
    }
	
	@Transactional
	public Result signup(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		int dialCode = Integer.parseInt(requestData.get("dialCode"));
		String mobile = requestData.get("mobile");
		String password = requestData.get("password");
		
		if(Utils.isBlank(mobile) || dialCode == 0){
			responseData.message = "The mobile number is incorrect.";
			responseData.code = 4000;
		}else{
			mobile = mobile.replaceAll("\\s+","");
			if(HomeController.notExists(dialCode, mobile)){
				Messages messages = messagesApi.preferred(request());
				
				int verifyCode = Utils.genernateActiveCode();
				String mobileWithDialCode = dialCode + mobile;
				String msg = messages.at("message.otp_sms") + " " + verifyCode;
				
				CompletableFuture.supplyAsync(() -> Utils.sendSMS(mobileWithDialCode, msg));
				cache.set(Account.MOBILE_PREFIX + mobileWithDialCode, verifyCode, 2 * 60);
				cache.set(Account.PASSWORD_PREFIX + mobileWithDialCode, password, 2 * 60);
				
				Account account = new Account();
				account.mobile = mobile;
				account.dialCode = dialCode;
				
				responseData.data = new AccountVO(account);
			}else{
				responseData.code = 4000;
				responseData.message = "The mobile number was already registered.";
			}
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
		
		String mobileWithDialCode = dialCode + mobile;
		int verifyCodeCached = 0;
		String passwordCached = null;
		if(cache != null){
			verifyCodeCached = cache.get(Account.MOBILE_PREFIX + mobileWithDialCode);
			passwordCached = cache.get(Account.PASSWORD_PREFIX + mobileWithDialCode);
		}
		
		if(verifyCodeCached == 0 || Utils.isBlank(passwordCached)){
			responseData.message = "Verify code has expired.";
			responseData.code = 4000;
		}else if(verifyCodeCached == verifyCode){
			Account account = new Account(dialCode, mobile, passwordCached);
			JPA.em().persist(account);
				
			User user = new User(account);
			JPA.em().persist(user);
				
			responseData.data = new AccountVO(account);
		}else{
			responseData.message = "Verify code is incorrect.";
			responseData.code = 4000;
		}

		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result resendVerifyCode(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		int dialCode = Integer.parseInt(requestData.get("dialCode"));
		String mobile = requestData.get("mobile");
		String password = requestData.get("password");
		
		Messages messages = messagesApi.preferred(request());
		
		int verifyCode = Utils.genernateActiveCode();
		String mobileWithDialCode = dialCode + mobile;
		String msg = messages.at("message.otp_sms") + " " + verifyCode;
		
		CompletableFuture.supplyAsync(() -> Utils.sendSMS(mobileWithDialCode, msg));
		cache.set(Account.MOBILE_PREFIX + mobileWithDialCode, verifyCode, 2 * 60);
		cache.set(Account.PASSWORD_PREFIX + mobileWithDialCode, password, 2 * 60);
		
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
    public Result userProfile() {
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String token = requestData.get("token");
		try{
			Account account = JPA.em().createQuery("from Account ac where ac.token = :token", Account.class)
			        .setParameter("token", token)
			        .getSingleResult();
			ProfileVO profileVO = new ProfileVO(account.user);
			responseData.data = profileVO;
		}catch(NoResultException e){
			responseData.message = "Account does not exist.";
			responseData.code = 4000;
		}
		
		return ok(Json.toJson(responseData));
    }

}
