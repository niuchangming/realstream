package controllers;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import javax.inject.Inject;
import javax.persistence.TypedQuery;

import actions.AdminAction;
import actions.AuthAction;
import models.Account;
import models.Avatar;
import models.Event;
import models.EventImage;
import models.Lesson;
import models.ResponseData;
import models.Role;
import models.User;
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
import views.html.createvent;
import views.html.errorpage;

public class EventController extends Controller{
	@Inject private JPAApi jpaApi;
	@Inject private FormFactory formFactory;
	
	@With(AdminAction.class)
	@Transactional
	public Result creatEvent(){
		Account account = (Account) ctx().args.get("account");
		return ok(createvent.render(account));
	}
	
	
	@With(AuthAction.class)
	@Transactional
	public Result saveEvent(){
		ResponseData responseData = new ResponseData();
		try{
			DynamicForm requestData = formFactory.form().bindFromRequest();
			String title = requestData.get("title");
			String description = requestData.get("description");
			String link = requestData.get("link");
			String lessonIdStr = requestData.get("lessonId");
			String teacherIdStr = requestData.get("teacherId");
					
		    MultipartFormData<File> body = request().body().asMultipartFormData();
		    FilePart<File> mobileImageFile = body.getFile("mobileImage"); 
		    FilePart<File> webImageFile = body.getFile("webImage");
		    
		    if(Utils.isBlank(title) || Utils.isBlank(description) || mobileImageFile == null || webImageFile == null){
				responseData.code = 4000;
				responseData.message = "Some data are missing.";
			}else{
				Event event = new Event();
				event.title = title;
				event.description = description;
				event.link = link;
				jpaApi.em().persist(event);
				
				EventImage mobileImage = new EventImage(event, mobileImageFile.getFile());
				mobileImage.name = mobileImageFile.getFilename();
				jpaApi.em().persist(mobileImage);
				
				EventImage webImage = new EventImage(event, webImageFile.getFile());
				webImage.name = webImageFile.getFilename();
				jpaApi.em().persist(webImage);
				
				if(!Utils.isBlank(lessonIdStr)){
			    	long lessonId = Integer.parseInt(teacherIdStr);
			    	Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
			    	lesson.event = event;
			    	jpaApi.em().persist(lesson);
			    }
				
				if(!Utils.isBlank(teacherIdStr)){
			    	long teacherId = Integer.parseInt(teacherIdStr);
			    	User teacher = jpaApi.em().find(User.class, teacherId);
			    	teacher.event = event;
			    	jpaApi.em().persist(teacher);
			    }
			}
		}catch (IOException e) {
			responseData.code = 4001;
			responseData.message = e.getLocalizedMessage();
		}
		return ok(Json.toJson(responseData));
	}
	
}
