package controllers;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import com.fasterxml.jackson.core.JsonProcessingException;
import models.Account;
import models.Lesson;
import models.ResponseData;
import models.User;
import actions.AuthAction;
import play.cache.CacheApi;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.*;
import tools.Utils;
import views.html.*;

public class HomeController extends Controller {
	private CacheApi cache;

    @Inject
    public HomeController(CacheApi cache) {
        this.cache = cache;
    }
	
	@Transactional
    public Result index() {
		String token = session().get(AuthAction.LOGGED_KEY);
		Account account = null;
		if(!Utils.isBlank(token)){
			TypedQuery<Account> query = JPA.em().createQuery("from Account ac where ac.token = :token", Account.class)
		            .setParameter("token", token);
			
			try{
				account = query.getSingleResult();
			}catch(NoResultException e){
				e.printStackTrace();
			}
		}
		
		List<Lesson> lessons = JPA.em().createQuery("from Lesson", Lesson.class).getResultList();
		
		return ok(index.render(account, lessons));
    }
	
	@Transactional
    public Result login(int dialCode, String mobile, String password) {
		ResponseData responseData = new ResponseData();
		try{
			TypedQuery<Account> query = JPA.em().createQuery("from Account ac where ac.mobile = :mobile and ac.dialCode = :dialCode", Account.class)
		            .setParameter("mobile", mobile)
		            .setParameter("dialCode", dialCode);
			try{
				Account account = query.getSingleResult();
				
				if(!Utils.md5(password).equals(account.password)){
					responseData.message = "The passowrd is incorrect.";
					responseData.code = 4000;
				}else{
					session(AuthAction.LOGGED_KEY, account.token);
					responseData.data = account;
					return ok(Utils.toJson(ResponseData.class, responseData, "*.user", "*.password", "*.token"));
				}
			}catch(NoResultException e){
				responseData.message = "Account does not exist.";
				responseData.code = 4000;
			}
		}catch(JsonProcessingException e){
			responseData.message = e.getLocalizedMessage();
			responseData.code = 4001;
		}
		
		return ok(Json.toJson(responseData));
    }
	
	@Transactional
	public Result signup(int dialCode, String mobile, String password){
		ResponseData responseData = new ResponseData();
		try {
			if(Utils.isBlank(mobile) || dialCode == 0){
				responseData.message = "The mobile number is incorrect.";
				responseData.code = 4000;
			}
			
			if(notExists(dialCode, mobile)){
				Account account = new Account(dialCode, mobile, password);
				JPA.em().persist(account);
				
				User user = new User(account);
				JPA.em().persist(user);

				account.user = user;
				
				int verifyCode = Utils.genernateActiveCode();
				String mobileWithDialCode = "+" + account.dialCode + account.mobile;
				String msg = "Your active code: " + verifyCode;
				CompletableFuture.supplyAsync(() -> Utils.sendSMS(mobileWithDialCode, msg));
				cache.set(mobileWithDialCode, verifyCode, 2 * 60);
				
				session(AuthAction.LOGGED_KEY, account.token);
				
				responseData.data = account;
				return ok(Utils.toJson(ResponseData.class, responseData, "*.user", "*.password", "*.token"));
			}else{
				responseData.code = 4000;
				responseData.message = "The mobile number was already registered.";
			}
		} catch (IOException e) {
			responseData.message = e.getLocalizedMessage();
			responseData.code = 4001;
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public static boolean notExists(int dialCode, String mobile){
		Query query = JPA.em().createQuery("select count(*) from Account ac where ac.dialCode = :dialCode and ac.mobile = :mobile")
	            .setParameter("dialCode", dialCode)
	            .setParameter("mobile", mobile);
		
		Long count;
		try{
			count = (Long)query.getSingleResult();
		}catch(NoResultException e){
			count = 0L;
		}
		
		if (count == 0)
			return true;

		return false;
	}
	
	@Transactional
	public Result verifyMobile(int dialCode, String mobile, int verifyCode){
		ResponseData responseData = new ResponseData();
		try{
			String mobileWithDialCode = "+" + dialCode + mobile;
			int verifyCodeCached = 0;
			if(cache != null){
				verifyCodeCached = cache.get(mobileWithDialCode);
			}
			
			if(verifyCodeCached == 0){
				responseData.message = "Verify code has expired.";
				responseData.code = 4000;
			}else if(verifyCodeCached == verifyCode){
				TypedQuery<Account> query = JPA.em().createQuery("from Account ac where ac.mobile = :mobile and ac.dialCode = :dialCode", Account.class)
			            .setParameter("mobile", mobile)
			            .setParameter("dialCode", dialCode);
				
				try{
					Account account = query.getSingleResult();
					account.active = true;
					JPA.em().persist(account);
					session(AuthAction.LOGGED_KEY, account.token);
					
					responseData.data = account;
					return ok(Utils.toJson(ResponseData.class, responseData, "*.user", "*.password", "*.token"));
				}catch(NoResultException e){
					responseData.message = "Account does not exist.";
					responseData.code = 4000;
				}
			}else{
				responseData.message = "Verify code has expired.";
				responseData.code = 4000;
			}
		}catch(JsonProcessingException e){
			responseData.message = e.getLocalizedMessage();
			responseData.code = 4001;
		}
		return ok(Json.toJson(responseData));
	}
	
	public Result logout() {
	    session().clear();
	    return redirect(controllers.routes.HomeController.index());
	}
	
}




























