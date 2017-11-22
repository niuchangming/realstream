package controllers.api;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import modelVOs.CommentVO;
import models.Account;
import models.Comment;
import models.Lesson;
import models.ResponseData;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import tools.Utils;

@SuppressWarnings("unchecked")
public class CommentController extends Controller{
	@Inject private JPAApi jpaApi;
	@Inject private FormFactory formFactory;

	@Transactional
	public Result commentsByLesson(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		long lessonId = Long.parseLong(requestData.get("lessonId"));
		String token = requestData.get("token");
		try {
			jpaApi.em().createQuery("from Account a where a.token = :token", Account.class)
			.setParameter("token", token)
			.getSingleResult();
			
			Query query = jpaApi.em()
					.createNativeQuery("SELECT cm.comment, cm.point, cm.create_datetime, im.thumbnail_uuid, ur.user_name FROM comment cm INNER JOIN image im ON cm.user_id = im.user_id INNER JOIN user ur ON cm.user_id=ur.account_id WHERE cm.lesson_id=:lessonId ORDER BY cm.create_datetime")
					.setParameter("lessonId", lessonId);
			
			List<CommentVO> commentVOs = null;
			List<Object[]> resultList = query.getResultList();
			if(resultList != null){
				commentVOs = new ArrayList<>();
				for(Object[] result : resultList){
					CommentVO commentVO = new CommentVO(result[0].toString(), Integer.parseInt(result[1].toString()), Utils.parse(result[2].toString()), result[3].toString(), result[4].toString());
					commentVOs.add(commentVO);
				}
			}
			responseData.data = commentVOs;
		} catch (NumberFormatException | ParseException e) {
			responseData.message = e.getLocalizedMessage();
			responseData.code = 4001;
		}catch(NoResultException e){
			responseData.message = "Account doesn't exist.";
			responseData.code = 4000;
		}
	
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result createComment(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		long lessonId = Long.parseLong(requestData.get("lessonId"));
		String commentStr = requestData.get("comment");
		int point = Integer.parseInt(requestData.get("point"));
		String token = requestData.get("token");
		
		try{
			Account account = jpaApi.em().createQuery("from Account a where a.token = :token", Account.class)
					.setParameter("token", token)
					.getSingleResult();
		
			Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
			if(lesson == null){
				responseData.code = 4000;
				responseData.message = "The lesson cannot be found";
			}else{
				Comment comment = new Comment(account.user, lesson);
				comment.comment = commentStr;
				comment.point = point;
				jpaApi.em().persist(comment);
			}
		}catch (NoResultException e) {
			responseData.message = "Account doesn't exist.";
			responseData.code = 4001;;
		}
		return ok(Json.toJson(responseData));
	}
}
