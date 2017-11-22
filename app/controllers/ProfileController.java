package controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import actions.AuthAction;
import modelVOs.AchievementVO;
import modelVOs.PaymentVO;
import modelVOs.StudentVO;
import models.Account;
import models.Avatar;
import models.Lesson;
import models.ResponseData;
import models.Role;
import models.User;
import play.Application;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPA;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import tools.Constants;
import tools.Utils;
import views.html.profilepersonal;
import views.html.profilemain;
import views.html.teachersettle;
import views.html.teacheragreement;
import views.html.settlereview;
import views.html.errorpage;
import views.html.profilelessons;
import views.html.profilestudents;
import views.html.teacherdetail;
import views.html.profilepayment;
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
			
			AchievementVO achieventmentVO = new AchievementVO(user);
			return ok(profilepersonal.render(user, achieventmentVO));
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
			AchievementVO achieventmentVO = new AchievementVO(user);
			return ok(profilemain.render(user, achieventmentVO));
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
					avatar.name = avatarFile.getFilename();
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
			}else{
				imageStream = application.get().classloader().getResourceAsStream(Avatar.DEFAULT_AVATAR);
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
			String ic = requestData.get("icNo");
			String bestInstitution = requestData.get("institution");
			String brief = requestData.get("brief");
					
		    user.realName = realName;
		    user.ic = ic;
		    user.bestInstitution = bestInstitution;
		    user.brief = brief;
		    user.role = Role.TEACHER;
		    if(Utils.isBlank(user.username)){
		    	user.username = realName;
		    }
		    
		    MultipartFormData<File> body = request().body().asMultipartFormData();
		    FilePart<File> avatarImageFile = body.getFile("avatarImage"); 
		    FilePart<File> certImageFile = body.getFile("certImage");
		    
		    if(user.avatars != null){
				for(Avatar avatar : user.avatars){
					avatar.deleteThumbnail();
					avatar.delete();
					jpaApi.em().remove(avatar);
				}
			}
			
			Avatar avatar = new Avatar(user, avatarImageFile.getFile());
			avatar.name = avatarImageFile.getFilename();
			jpaApi.em().persist(avatar);
		    
		    user.uploadCertImage(certImageFile.getFile());
		    
		    jpaApi.em().persist(user);
		    user.initWorkExperiences(requestData.data());
		    
		    return redirect(routes.ProfileController.settleReview());
		}catch (ParseException | IOException e) {
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
				int pageIndex = (int) Math.ceil(offset / Constants.LESSON_PAGE_SIZE) + 1;
				
				List<Lesson> lessons = jpaApi.em()
						.createQuery("from Lesson ls where ls.teacher = :teacher", Lesson.class)
						.setParameter("teacher", teacher)
						.setFirstResult(offset)
						.setMaxResults(Constants.LESSON_PAGE_SIZE)
						.getResultList();
				
				AchievementVO achieventmentVO = new AchievementVO(teacher);
				return ok(profilelessons.render(teacher, lessons, pageIndex, totalAmount, achieventmentVO));
			}
    	}catch(NoResultException e){
    		responseData.code = 4000;
			responseData.message = "User does not exist.";
    	}
		
		return notFound(errorpage.render(responseData));
	}
	
	
	@With(AuthAction.class)
	@Transactional
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
				
			AchievementVO achieventmentVO = new AchievementVO(user);
			return ok(profileregisteredlessons.render(user, lessons, achieventmentVO));
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
			int pageIndex = (int) Math.ceil(offset / Constants.LESSON_PAGE_SIZE) + 1;
			
			List<Lesson> lessons = jpaApi.em()
					.createNativeQuery("SELECT * FROM lesson ls INNER JOIN favorite_lesson fl ON ls.id = fl.lesson_id WHERE fl.user_id = :userId", Lesson.class)
					.setParameter("userId", user.accountId)
					.setFirstResult(offset)
					.setMaxResults(Constants.LESSON_PAGE_SIZE)
					.getResultList();
			
			AchievementVO achieventmentVO = new AchievementVO(user);
			
			return ok(profilefavlessons.render(user, lessons, pageIndex, totalAmount, achieventmentVO));
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
	
	@With(AuthAction.class)
	@Transactional
	public Result myStudents(int offset){
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
				int totalAmount = ((BigInteger)JPA.em()
						.createNativeQuery("SELECT COUNT(u.account_id) FROM lesson ls INNER JOIN user_lesson ul ON ls.id = ul.lesson_id INNER JOIN user u ON u.account_id=ul.user_id WHERE ls.teacher_id = :teacherId")
						.setParameter("teacherId", teacher.accountId)
						.getSingleResult()).intValue();
				int pageIndex = (int) Math.ceil(offset / Constants.STUDENT_PAGE_SIZE) + 1;
				
				List<Object[]> resultList = jpaApi.em()
						.createNativeQuery("SELECT u.account_id, u.user_name, ls.title FROM lesson ls INNER JOIN user_lesson ul ON ls.id = ul.lesson_id INNER JOIN user u ON u.account_id=ul.user_id WHERE ls.teacher_id = :teacherId")
						.setParameter("teacherId", teacher.accountId)
						.setFirstResult(offset)
						.setMaxResults(Constants.STUDENT_PAGE_SIZE)
						.getResultList();
				
				List<StudentVO> studentVOs = null;
				if(resultList != null){
					studentVOs = new ArrayList<>();
					for(Object[] result : resultList){
						StudentVO studentVO = new StudentVO(Long.parseLong(result[0].toString()), result[1] == null ? null : result[1].toString(), result[2] == null ? null : result[2].toString(), 0);
						studentVOs.add(studentVO);
					}
				}
				
				AchievementVO achieventmentVO = new AchievementVO(teacher);
				return ok(profilestudents.render(teacher, studentVOs, pageIndex, totalAmount, achieventmentVO));
			}
    	}catch(NoResultException e){
    		responseData.code = 4000;
			responseData.message = "User does not exist.";
    	}
		
		return notFound(errorpage.render(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result paymentHistory(int offset){
		ResponseData responseData = new ResponseData();
		try{
			Account account = (Account) ctx().args.get("account");
			TypedQuery<User> query = jpaApi.em()
					.createQuery("from User u where u.accountId = :accountId", User.class)
					.setParameter("accountId", account.id);
			User user = query.getSingleResult();
			
			int totalAmount = user.payments.size();
			int pageIndex = (int) Math.ceil(offset / Constants.PAYMENT_PAGE_SIZE) + 1;
			
			List<Object[]> resultList = jpaApi.em()
					.createNativeQuery("SELECT ls.id, ls.title, p.transaction_id, p.amount, p.currency, p.pay_datetime, p.status FROM payment p LEFT JOIN lesson ls ON p.lesson_id=ls.id WHERE p.user_id= :userId")
					.setParameter("userId",user.accountId)
					.setFirstResult(offset)
					.setMaxResults(Constants.PAYMENT_PAGE_SIZE)
					.getResultList();
			
			List<PaymentVO> paymentVOs = null;
			if(resultList != null){
				paymentVOs = new ArrayList<>();
				for(Object[] result : resultList){
					//因为是 LEFT JOIN, 所以lesson的ID和title啃呢个为空
					PaymentVO paymentVO = new PaymentVO(Long.parseLong(result[0] == null ? "0" : result[0].toString()), result[1]==null ? "" : result[1].toString(), 
							result[2].toString(), Double.parseDouble(result[3].toString()), result[4].toString(), result[5].toString(), result[6].toString());
					paymentVOs.add(paymentVO);
				}
			}
			
			AchievementVO achieventmentVO = new AchievementVO(user);
			return ok(profilepayment.render(user, paymentVOs, pageIndex, totalAmount, achieventmentVO));
		}catch(NoResultException e){
			responseData.code = 4000;
			responseData.message = "User does not exist.";
    	}
		
		return notFound(errorpage.render(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result closingBroadcastVideo(){
		DynamicForm requestData = formFactory.form().bindFromRequest();
		long lessonSessionId = Long.parseLong(requestData.get("lessonSessionId"));
		
		
		return ok();
	}
}























