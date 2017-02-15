package controllers;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import actions.AuthAction;
import modelVOs.CommentVO;
import models.Account;
import models.Comment;
import models.Lesson;
import models.LessonSession;
import models.ResponseData;
import models.User;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import tools.Utils;

public class CommentController extends Controller{
	
	@Inject private FormFactory formFactory;
	@Inject private JPAApi jpaApi;
	@Inject private WSClient ws;
	
	@With(AuthAction.class)
	@Transactional
	public Result createComment(){
		ResponseData responseData = new ResponseData();
		
		Account account = (Account) ctx().args.get("account");
		TypedQuery<User> userQuery = jpaApi.em()
				.createQuery("from User u where u.accountId = :accountId", User.class)
				.setParameter("accountId", account.id);
		User user = userQuery.getSingleResult();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		long lessonId = Long.parseLong(requestData.get("lessonId"));
		String commentStr = requestData.get("comment");
		int point = Integer.parseInt(requestData.get("point"));
	
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
		if(user == null){
			responseData.code = 4000;
			responseData.message = "The user cannot be found";
		}else if(lesson == null){
			responseData.code = 4000;
			responseData.message = "The lesson cannot be found";
		}else{
			Comment comment = new Comment(user, lesson);
			comment.comment = commentStr;
			comment.point = point;
			jpaApi.em().persist(comment);
			
			responseData.data = comment;
			
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonData;
			try {
				jsonData = mapper.readTree(Utils.toJson(ResponseData.class, responseData, "*.lesson", "*.user"));
				return ok(Json.toJson(jsonData));
			} catch (IOException e) {
				responseData.code = 4001;
				responseData.message = e.getLocalizedMessage();
			}
		}
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	@SuppressWarnings("unchecked")
	public Result commentsByLesson(){
		ResponseData responseData = new ResponseData();

		DynamicForm requestData = formFactory.form().bindFromRequest();
		long lessonId = Long.parseLong(requestData.get("lessonId"));
		try {
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
		
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonData = mapper.readTree(Utils.toJson(ResponseData.class, responseData, ""));
			return ok(Json.toJson(jsonData));
		} catch (IOException | NumberFormatException | ParseException e) {
			responseData.message = e.getLocalizedMessage();
			responseData.code = 4001;
		}
	
		return ok(Json.toJson(responseData));
	}
	
}


















