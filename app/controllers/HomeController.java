package controllers;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import com.fasterxml.jackson.core.JsonProcessingException;

import models.Account;
import models.Category;
import models.Lesson;
import models.ResponseData;
import models.Role;
import models.User;
import actions.AuthAction;
import play.cache.CacheApi;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPA;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.*;
import tools.Utils;
import views.html.*;

@SuppressWarnings("unchecked")
public class HomeController extends Controller {
	@Inject private CacheApi cache;
	@Inject private JPAApi jpaApi;
	@Inject private MessagesApi messagesApi;
	@Inject private FormFactory formFactory;

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
		
		List<Lesson> lessons = jpaApi.em()
				.createNativeQuery("SELECT * FROM (SELECT *, @row_number \\:= CASE WHEN @category_id=category_id "
						+ "THEN @row_number+1 ELSE 1 END AS row_number, "
						+ "@category_id \\:= category_id FROM lesson, "
						+ "(SELECT @row_number \\:= 0,@category_id \\:= '') AS u "
						+ "WHERE is_publish = 1 AND end_datetime > now() "
						+ "ORDER BY category_id) as v "
						+ "WHERE row_number <= 8;", Lesson.class)
				.getResultList();
		
		Map<Category, List<Lesson>> lessonMap = lessons.stream().collect(Collectors.groupingBy(s -> s.category));
		Map<Category, List<Lesson>> lessonMapSorted = new TreeMap<Category, List<Lesson>>(
				  new Comparator<Category>() {

					  @Override
					  public int compare(Category c1, Category c2) {
						  return (int)(c1.id - c2.id);
					  }
					  
				  });
		lessonMapSorted.putAll(lessonMap);
		
		List<Category> categories = JPA.em().createQuery("from Category where parent is null", Category.class).getResultList();
		return ok(index.render(account, lessonMapSorted, categories));
    }
	
	@Transactional
    public Result login(int dialCode, String mobile, String password) {
		ResponseData responseData = new ResponseData();
		try{
			TypedQuery<Account> query = JPA.em().createQuery("from Account ac where ac.mobile = :mobile and ac.dialCode = :dialCode", Account.class)
		            .setParameter("mobile", mobile.replaceAll("\\s+",""))
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
				Messages messages = messagesApi.preferred(request());
				
				mobile = mobile.replaceAll("\\s+","");
				
				int verifyCode = Utils.genernateActiveCode();
				String mobileWithDialCode = dialCode + mobile;
				String msg = messages.at("message.otp_sms") + " " + verifyCode;
				CompletableFuture.supplyAsync(() -> Utils.sendSMS(mobileWithDialCode, msg));
				cache.set(Account.MOBILE_PREFIX + mobileWithDialCode, verifyCode, 2 * 60);
				cache.set(Account.PASSWORD_PREFIX + mobileWithDialCode, password, 2 * 60);
				
				Account account = new Account();
				account.mobile = mobile;
				account.dialCode = dialCode;
				
				responseData.data = account;
				return ok(Utils.toJson(ResponseData.class, responseData));
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
				
			session(AuthAction.LOGGED_KEY, account.token);
		}else{
			responseData.message = "Verify code is incorrect.";
			responseData.code = 4000;
		}
		return ok(Json.toJson(responseData));
	}
	
	public Result logout() {
	    session().clear();
	    return redirect(controllers.routes.HomeController.index());
	}
	
	public Result comingSoon(){
		return ok(comingsoon.render());
	}
	
	@Transactional
	public Result privacy(){
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
		return ok(privacy.render(account));
	}
	
	@Transactional
	public Result termOfUse(){
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
		return ok(termofuse.render(account));
	}
	
	@Transactional
	public Result about(){
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
		return ok(about.render(account));
	}
	
	@Transactional
	public Result tutors(){
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
		
		List<User> teachers = jpaApi.em()
				.createQuery("FROM User u WHERE u.role = :role", User.class)
				.setParameter("role", models.Role.TEACHER)
				.getResultList();
		return ok(tutors.render(account, teachers));
	}
	
	@Transactional
	public Result becomeTutor(){
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
		return ok(becometutor.render(account));
	}
	
	@Transactional
	public Result externalLinks(){
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
		return ok(externalinks.render(account));
	}
	
	@Transactional
	public Result helpTutorRes(){
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
		return ok(helptutorreg.render(account));
	}
	
	@Transactional
	public Result helpCreatePublishLesson(){
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
		return ok(helpcreatepublishlesson.render(account));
	}
	
	@Transactional
	public Result helpStartSession(){
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
		return ok(helpstartsession.render(account));
	}
	
	@Transactional
	public Result helpRegister(){
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
		return ok(helpregister.render(account));
	}
	
	@Transactional
	public Result adminLogin(){
		return ok(adminlogin.render());
	}
	
	@Transactional
    public Result adminLoginSubmit() {
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
				if(account.user.role == Role.ADMIN){
					session(AuthAction.LOGGED_KEY, account.token);
					responseData.data = account;
					return redirect(routes.TuiwenController.createTuiwen());
				}else{
					responseData.message = "You don't have such permission.";
					responseData.code = 4000;
				}
			}
		}catch(NoResultException e){
			responseData.message = "Account does not exist.";
			responseData.code = 4000;
		}
		
		return notFound(errorpage.render(responseData));
    }
	
	@Transactional
	public Result changeLanguage(String lang){
		ctx().changeLang(lang);
		return index();
	}
	
}





















