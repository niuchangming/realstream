package controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import actions.AuthAction;
import models.Account;
import models.Avatar;
import models.Lesson;
import models.LessonSession;
import models.ResponseData;
import models.Role;
import models.User;
import play.Application;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import tools.Utils;
import views.html.profilepersonal;
import views.html.profilemain;
import views.html.teachersettle;
import views.html.teacheragreement;
import views.html.settlereview;
import views.html.errorpage;
import views.html.profilelessons;
import views.html.teacherdetail;
import views.html.profileregisteredlessons;
import views.html.profilefavlessons;

@SuppressWarnings("unchecked")
public class ProfileController extends Controller{
	@Inject
	private JPAApi jpaApi;
	
	@Inject
    private FormFactory formFactory;
	
	@Inject
	private Provider<Application> application;
	
	@With(AuthAction.class)
	@Transactional
	public Result personalProfile(){
		ResponseData responseData = new ResponseData();
		
		Account account = (Account) ctx().args.get("account");
		
		TypedQuery<User> query = jpaApi.em()
				.createQuery("from User u where u.accountId = :accountId", User.class)
				.setParameter("accountId", account.id);
		try{
			User user = query.getSingleResult();
			return ok(profilepersonal.render(user));
		}catch(NoResultException e){
			responseData.code = 4000;
			responseData.message = "User cannot be found.";
			return notFound(errorpage.render(responseData));
		}
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result updatePersonalProfile(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String username = requestData.get("username");
		String email = requestData.get("email");
		String wechat = requestData.get("wechat");
		String qq = requestData.get("qq");
		
		try{
			Account account = (Account) ctx().args.get("account");
			
			TypedQuery<User> query = jpaApi.em()
					.createQuery("from User u where u.accountId = :accountId", User.class)
					.setParameter("accountId", account.id);
			User user = query.getSingleResult();
			
			user.username = username;
			user.email = email;
			user.wechat = wechat;
			user.qq = qq;
			jpaApi.em().persist(user);
		}catch(NoResultException e){
			responseData.code = 4000;
			responseData.message = "User does not exist.";
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result personalMain(){
		ResponseData responseData = new ResponseData();
		try{
			Account account = (Account) ctx().args.get("account");
			
			TypedQuery<User> query = jpaApi.em()
					.createQuery("from User u where u.accountId = :accountId", User.class)
					.setParameter("accountId", account.id);
			User user = query.getSingleResult();
			
			return ok(profilemain.render(user));
		}catch(NoResultException e){
			responseData.code = 4000;
			responseData.message = "User does not exist.";
			return notFound(errorpage.render(responseData));
		}
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result uploadAvatar(){
		ResponseData responseData = new ResponseData();
		MultipartFormData<File> body = request().body().asMultipartFormData();
	    FilePart<File> avatarFile = body.getFile("avatar");
	    try {
		    if(avatarFile != null){
		    	try{
		    		Account account = (Account) ctx().args.get("account");
		    		
		    		TypedQuery<User> query = jpaApi.em()
							.createQuery("from User u where u.accountId = :accountId", User.class)
							.setParameter("accountId", account.id);
					User user = query.getSingleResult();
					
					if(user.avatars != null){
						for(Avatar avatar : user.avatars){
							avatar.deleteThumbnail();
							avatar.delete();
							jpaApi.em().remove(avatar);
						}
					}
					
					Avatar avatar = new Avatar(user, avatarFile.getFile());
					jpaApi.em().persist(avatar);
					responseData.data = avatar;
					
					return ok(Utils.toJson(ResponseData.class, responseData, "*.user"));
		    	}catch(NoResultException e){
		    		responseData.code = 4000;
					responseData.message = "User does not exist.";
		    	}
		    }else{
		    	responseData.message = "Image file cannot be empty.";
		    	responseData.code = 4000;
		    }
	    } catch (IOException e) {
	    	responseData.message = e.getLocalizedMessage();
	    	responseData.code = 4001;
		}		
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result showAvatarThumbnail(String thumbnailUUID){
		InputStream imageStream;
		if(Utils.isBlank(thumbnailUUID)){
			imageStream = application.get().classloader().getResourceAsStream(Avatar.DEFAULT_AVATAR);
		}else{
			TypedQuery<Avatar> query = jpaApi.em()
					.createQuery("from Avatar a where a.thumbnailUUID = :thumbnailUUID", Avatar.class)
					.setParameter("thumbnailUUID", thumbnailUUID);
			
			try{
				Avatar avatar = query.getSingleResult();
				imageStream = avatar.downloadThumbnail();
			}catch(NoResultException e){
				imageStream = application.get().classloader().getResourceAsStream(Avatar.DEFAULT_AVATAR);
			}
		}
		return ok(imageStream);
	}
	
	@Transactional
	public Result showAvatar(long userId, boolean isLarge){
		InputStream imageStream = null;
		
		User user = jpaApi.em().find(User.class, userId);
		if(user != null){
			if(user.avatars != null && user.avatars.size() > 0){
				if(isLarge){
					imageStream = user.avatars.get(0).download();
				}else{
					imageStream = user.avatars.get(0).downloadThumbnail();
				}
			}
		}else{
			imageStream = application.get().classloader().getResourceAsStream(Avatar.DEFAULT_AVATAR);
		}
		
		return ok(imageStream);
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result readTeacherAgreement(){
		Account account = (Account) ctx().args.get("account");
		account = jpaApi.em().find(Account.class, account.id);
		return ok(teacheragreement.render(account));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result teacherSettle(){
		Account account = (Account) ctx().args.get("account");
		account = jpaApi.em().find(Account.class, account.id);
		return ok(teachersettle.render(account));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result submitTeacherInfo(){
		ResponseData responseData = new ResponseData();
		try{
			Account account = (Account) ctx().args.get("account");
			
			TypedQuery<User> query = jpaApi.em()
					.createQuery("from User u where u.accountId = :accountId", User.class)
					.setParameter("accountId", account.id);
			User user = query.getSingleResult();
			
			DynamicForm requestData = formFactory.form().bindFromRequest();
			String realName = requestData.get("realName");
			String bestInstitution = requestData.get("institution");
			String brief = requestData.get("brief");
			
		    user.realName = realName;
		    user.bestInstitution = bestInstitution;
		    user.brief = brief;
		    user.role = Role.TEACHER;
		    
		    MultipartFormData<File> body = request().body().asMultipartFormData();
		    FilePart<File> icImageFile = body.getFile("icImage"); 
		    FilePart<File> certImageFile = body.getFile("certImage");
		    	
		    user.uploadIcImage(icImageFile.getFile());
		    user.uploadCertImage(certImageFile.getFile());
		    
		    jpaApi.em().persist(user);
		    user.initWorkExperiences(requestData.data());
		    
		    return redirect(routes.ProfileController.settleReview());
		}catch (ParseException e) {
			responseData.code = 4001;
			responseData.message = e.getLocalizedMessage();
			return notFound(errorpage.render(responseData));
		}
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result updateTeacherInfo(){
		ResponseData responseData = new ResponseData();
		try{
			Account account = (Account) ctx().args.get("account");
			
			TypedQuery<User> query = jpaApi.em()
					.createQuery("from User u where u.accountId = :accountId", User.class)
					.setParameter("accountId", account.id);
			User user = query.getSingleResult();
			
			DynamicForm requestData = formFactory.form().bindFromRequest();
			String realName = requestData.get("realName");
			String bestInstitution = requestData.get("institution");
			String brief = requestData.get("brief");
			
		    user.realName = realName;
		    user.bestInstitution = bestInstitution;
		    user.brief = brief;
		    jpaApi.em().persist(user);
		    
		    user.initWorkExperiences(requestData.data());
		}catch (ParseException | NoResultException e) {
			if(e instanceof NoResultException){
				responseData.code = 4000;
			}else{
				responseData.code = 4001;
			}
			responseData.message = e.getLocalizedMessage();
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result settleReview(){
		Account account = (Account) ctx().args.get("account");
		account = jpaApi.em().find(Account.class, account.id);
		return ok(settlereview.render(account));
	}
	
	@Transactional
	public Result teacherDetail(long userId){
		ResponseData responseData = new ResponseData();
		
		Account account = jpaApi.em().find(Account.class, userId);
		if(account != null){
			if(account.user.role != Role.TEACHER){
				responseData.code = 4000;
				responseData.message = "The user is not a teacher.";
			}
		}else{
			responseData.code = 4000;
			responseData.message = "The teacher does not exist.";
		}
		
		if(responseData.code == 0){
			return ok(teacherdetail.render(account));
		}
		
		return notFound(errorpage.render(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result myLessons(int offset){
		ResponseData responseData = new ResponseData();
		try{
			Account account = (Account) ctx().args.get("account");
			TypedQuery<User> query = jpaApi.em()
					.createQuery("from User u where u.accountId = :accountId", User.class)
					.setParameter("accountId", account.id);
			User teacher = query.getSingleResult();
			
			if(teacher.role != Role.TEACHER){
				responseData.code = 4000;
				responseData.message = "You don't have teacher permission.";
			}else{
				int totalAmount = ((Long)jpaApi.em()
						.createQuery("select count(*) from Lesson ls where ls.teacher = :teacher")
						.setParameter("teacher", teacher).getSingleResult()).intValue();
				int pageIndex = (int) Math.ceil(offset / Lesson.PAGE_SIZE) + 1;
				
				List<Lesson> lessons = jpaApi.em()
						.createQuery("from Lesson ls where ls.teacher = :teacher", Lesson.class)
						.setParameter("teacher", teacher)
						.setFirstResult(offset)
						.setMaxResults(LessonSession.PAGE_SIZE)
						.getResultList();
				
				return ok(profilelessons.render(teacher, lessons, pageIndex, totalAmount));
			}
    	}catch(NoResultException e){
    		responseData.code = 4000;
			responseData.message = "User does not exist.";
    	}
		
		return notFound(errorpage.render(responseData));
	}
	
	
	@With(AuthAction.class)
	@Transactional
	@SuppressWarnings("unchecked")
	public Result myRegisteredLessons(){
		ResponseData responseData = new ResponseData();
		try{
			Account account = (Account) ctx().args.get("account");
			TypedQuery<User> query = jpaApi.em()
					.createQuery("from User u where u.accountId = :accountId", User.class)
					.setParameter("accountId", account.id);
			
			User user = query.getSingleResult();
			
			List<Lesson> lessons = jpaApi.em()
					.createNativeQuery("select * FROM lesson ls INNER JOIN user_lesson ul ON ls.id=ul.lesson_id WHERE ul.user_id=:userId AND ul.is_deleted=:isDeleted AND ul.is_completed=:isCompleted", Lesson.class)
					.setParameter("userId", user.accountId)
					.setParameter("isDeleted", false)
					.setParameter("isCompleted", false)
					.getResultList();
				
			return ok(profileregisteredlessons.render(user, lessons));
    	}catch(NoResultException e){
    		responseData.code = 4000;
			responseData.message = "User does not exist.";
    	}
		
		return notFound(errorpage.render(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result addFavoriteLesson(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		long lessonId = Long.parseLong(requestData.get("lessonId"));
		
		try{
			Account account = (Account) ctx().args.get("account");
			TypedQuery<User> query = jpaApi.em()
					.createQuery("from User u where u.accountId = :accountId", User.class)
					.setParameter("accountId", account.id);
			
			User user = query.getSingleResult();
			Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
			if(lesson != null){
				int count = ((BigInteger)jpaApi.em()
						.createNativeQuery("select count(*) from favorite_lesson fl where fl.user_id = :userId and fl.lesson_id = :lessonId")
						.setParameter("userId", user.accountId )
						.setParameter("lessonId", lesson.id).getSingleResult()).intValue();
				if(count == 0){
					user.favoriteLessons.add(lesson);
					jpaApi.em().persist(user);
				}else{
					responseData.code = 5000;
					responseData.message = "You have add the lesson as your favourite.";
				}
			}else{
				responseData.code = 4000;
				responseData.message = "Lesson does not exist.";
			}
    	}catch(NoResultException e){
    		responseData.code = 4000;
			responseData.message = "User does not exist.";
    	}
		
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result myFavoriteLessons(int offset){
		ResponseData responseData = new ResponseData();
		try{
			Account account = (Account) ctx().args.get("account");
			TypedQuery<User> query = jpaApi.em()
					.createQuery("from User u where u.accountId = :accountId", User.class)
					.setParameter("accountId", account.id);
			
			User user = query.getSingleResult();
			int totalAmount = ((BigInteger)jpaApi.em()
					.createNativeQuery("SELECT count(*) FROM favorite_lesson fl where fl.user_id = :userId")
					.setParameter("userId", user.accountId).getSingleResult()).intValue();
			int pageIndex = (int) Math.ceil(offset / Lesson.PAGE_SIZE) + 1;
			
			List<Lesson> lessons = jpaApi.em()
					.createNativeQuery("SELECT * FROM lesson ls LEFT JOIN favorite_lesson fl ON ls.id = fl.lesson_id WHERE fl.user_id = :userId", Lesson.class)
					.setParameter("userId", user.accountId)
					.setFirstResult(offset)
					.setMaxResults(LessonSession.PAGE_SIZE)
					.getResultList();
			
			return ok(profilefavlessons.render(user, lessons, pageIndex, totalAmount));
		}catch(NoResultException e){
			responseData.code = 4000;
			responseData.message = "User does not exist.";
		}
		return notFound(errorpage.render(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result removeFavoriteLesson(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		long lessonId = Long.parseLong(requestData.get("lessonId"));
		try{
			Account account = (Account) ctx().args.get("account");
			TypedQuery<User> query = jpaApi.em()
					.createQuery("from User u where u.accountId = :accountId", User.class)
					.setParameter("accountId", account.id);
			
			User user = query.getSingleResult();
			Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
			
			if(lesson != null){
				user.favoriteLessons.remove(lesson);
				jpaApi.em().persist(user);
			}else{
				responseData.code = 4000;
				responseData.message = "Lesson does not exist.";
			}
		}catch(NoResultException e){
			responseData.code = 4000;
			responseData.message = "User does not exist.";
		}
		
		return ok(Json.toJson(responseData));
	}
	
}























