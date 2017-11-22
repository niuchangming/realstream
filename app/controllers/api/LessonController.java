package controllers.api;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.NoResultException;

import com.fasterxml.jackson.core.JsonProcessingException;

import modelVOs.LessonSessionVO;
import modelVOs.LessonVO;
import models.Account;
import models.Lesson;
import models.Category;
import models.LessonSession;
import models.ResponseData;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import tools.Constants;
import tools.Utils;

@SuppressWarnings("unchecked")
public class LessonController extends Controller{
	@Inject private JPAApi jpaApi;
	@Inject private FormFactory formFactory;
	
	@Transactional
	public Result lessons(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		int offset = Integer.parseInt(requestData.get("offset"));
		int pageSize = Integer.parseInt(requestData.get("page_size"));
		
		List<Lesson> lessons = jpaApi.em()
				.createQuery("FROM Lesson ls WHERE ls.isPublish=:isPublish AND ls.endDatetime > :endDatetime", Lesson.class)
				.setParameter("isPublish", true)
				.setParameter("endDatetime", new Date())
				.setFirstResult(offset)
				.setMaxResults(pageSize > 0 ? pageSize : Constants.LESSON_SESSION_PAGE_SIZE)
				.getResultList();
		
		List<LessonVO> lessonVOs = LessonVO.getLessonVOs(lessons);
		responseData.data = lessonVOs;
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result lessonsByCategory(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		int offset = Integer.parseInt(requestData.get("offset"));
		int pageSize = Integer.parseInt(requestData.get("page_size"));
		long categoryId = Long.parseLong(requestData.get("category_id"));
		
		Category category = jpaApi.em().find(Category.class, categoryId);
		if(category != null){
			List<Lesson> lessons = jpaApi.em()
					.createQuery("FROM Lesson ls WHERE ls.isPublish=:isPublish AND ls.endDatetime > :endDatetime AND ls.category=:category", Lesson.class)
					.setParameter("isPublish", true)
					.setParameter("endDatetime", new Date())
					.setParameter("category", category)
					.setFirstResult(offset)
					.setMaxResults(pageSize > 0 ? pageSize : Constants.LESSON_SESSION_PAGE_SIZE)
					.getResultList();
			
			List<LessonVO> lessonVOs = LessonVO.getLessonVOs(lessons);
			responseData.data = lessonVOs;
		}else{
			responseData.code = 4000;
			responseData.message = "Category does not exist.";
		}
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result lessonEnrollStatus(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		long lessonId = Long.parseLong(requestData.get("lessonId"));
		String token = requestData.get("token");
		try{
			int lessonCount = ((BigInteger)jpaApi.em()
					.createNativeQuery("SELECT COUNT(*) FROM user_lesson ul LEFT JOIN account ac ON ul.user_id=ac.id WHERE ac.token=:token AND ul.lesson_id=:lessonId")
					.setParameter("token", token)
					.setParameter("lessonId", lessonId).getSingleResult()).intValue();
			
			responseData.data = lessonCount;
    	}catch(NoResultException e){
    		responseData.code = 4000;
			responseData.message = "User does not exist.";
    	}
		
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result lessonSessions(){
		ResponseData responseData = new ResponseData();
		try {
			DynamicForm requestData = formFactory.form().bindFromRequest();
			long lessonId = Long.parseLong(requestData.get("lessonId"));
			
			List<LessonSession> lessonSessions = jpaApi.em()
					.createNativeQuery("select * from lesson_session ls where ls.lesson_id = :lessonId order by ls.start_datetime asc", LessonSession.class)
					.setParameter("lessonId", lessonId)
					.getResultList();
			
			List<LessonSessionVO> lessonSessionVOs = LessonSessionVO.getLessonSessionVOs(lessonSessions);
			responseData.data = lessonSessionVOs;
			return ok(Utils.toJson(ResponseData.class, responseData));
		} catch (JsonProcessingException e) {
			responseData.code = 4001;
			responseData.message = e.getLocalizedMessage();
		}
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result timetable(){
		ResponseData responseData = new ResponseData();
		try {
			DynamicForm requestData = formFactory.form().bindFromRequest();
			String token = requestData.get("token");
			
			Account account = jpaApi.em().createQuery("from Account a where a.token = :token", Account.class)
					.setParameter("token", token)
					.getSingleResult();
			
			List<Lesson> lessons = jpaApi.em()
					.createNativeQuery("select * FROM lesson ls INNER JOIN user_lesson ul ON ls.id=ul.lesson_id WHERE ul.user_id=:userId AND ul.is_deleted=:isDeleted AND ul.is_completed=:isCompleted", Lesson.class)
					.setParameter("userId", account.id)
					.setParameter("isDeleted", false)
					.setParameter("isCompleted", false)
					.getResultList();
			
			List<LessonVO> lessonVOs = LessonVO.getLessonVOs(lessons);
			responseData.data = lessonVOs;
		} catch(NoResultException e){
			responseData.code = 4000;
			responseData.message = "Account does not exist.";
		}
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result favoriteLessons(){
		ResponseData responseData = new ResponseData();
		try{
			DynamicForm requestData = formFactory.form().bindFromRequest();
			String token = requestData.get("token");
			
			Account account = jpaApi.em().createQuery("from Account a where a.token = :token", Account.class)
					.setParameter("token", token)
					.getSingleResult();
			
			List<LessonVO> favoriteLessons = LessonVO.getLessonVOs(account.user.favoriteLessons);
			responseData.data = favoriteLessons;
		}catch(NoResultException e){
			responseData.code = 4000;
			responseData.message = "Account does not exist.";
		}
		return ok(Json.toJson(responseData));
	}
    
}


