package controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import actions.AuthAction;
import models.Lesson;
import models.LessonSession;
import models.MediaFile;
import models.ResponseData;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import tools.Utils;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import views.html.errorpage;
import views.html.filemanagement;

public class FileController extends Controller{
	@Inject private FormFactory formFactory;
	@Inject private JPAApi jpaApi;
	
	@With(AuthAction.class)
	@Transactional
	public Result fileManagement(long lessonId, int offset){
		ResponseData responseData = new ResponseData();
		
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
		if(lesson == null){
			responseData.message = "Lesson cannot be found.";
    		responseData.code = 4000;
    		return notFound(errorpage.render(responseData));
		}
		
		int totalAmount = ((Long)jpaApi.em()
				.createQuery("select count(*) from MediaFile mf where mf.lesson = :lesson")
				.setParameter("lesson", lesson).getSingleResult()).intValue();
		int pageIndex = (int) Math.ceil(offset / MediaFile.PAGE_SIZE) + 1;
		
		List<MediaFile> mediaFiles = jpaApi.em()
				.createQuery("from MediaFile mf where mf.lesson = :lesson ", MediaFile.class)
				.setParameter("lesson", lesson)
				.setFirstResult(offset)
				.setMaxResults(LessonSession.PAGE_SIZE)
				.getResultList();
		return ok(filemanagement.render(lesson, mediaFiles, pageIndex, totalAmount));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result uploadMediaFile(){
		ResponseData responseData = new ResponseData();	
		
		MultipartFormData<File> body = request().body().asMultipartFormData();
	    FilePart<File> filePart = body.getFile("file");
	    if(filePart != null){
	    	DynamicForm requestData = formFactory.form().bindFromRequest();
	    	long lessonId = Long.parseLong(requestData.get("lessonId"));
	    	long lessonSessionId =  Long.parseLong(requestData.get("lessonSessionId"));
	    	
			Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
			
			LessonSession lessonSession = null;
			if(lessonSessionId != -1){
				lessonSession = jpaApi.em().find(LessonSession.class, lessonSessionId);
			}
			
			if(lesson == null){
				responseData.message = "The lesson cannot be found.";
				responseData.code = 4000;
			}else{
				String contentType = filePart.getContentType();
				
				MediaFile mediaFile = new MediaFile(lesson, filePart.getFile());
				mediaFile.name = filePart.getFilename();
				
				if(contentType.equals("application/msword")){
					mediaFile.fileType = "DOC";
			    }else if(contentType.equals("application/pdf")){
			    	mediaFile.fileType = "PDF";
			    }else if(contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")){
			    	mediaFile.fileType = "XLSX";
			    }else if(contentType.equals("application/vnd.ms-excel")){
			    	mediaFile.fileType = "XLS";
			    }else if(contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")){
			    	mediaFile.fileType = "DOCX";
			    }else if(contentType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")){
			    	mediaFile.fileType = "PPTX";
			    }else{
			    	responseData.message = "Doesn't support this format.";
			    	responseData.code = 4000;
			    	return ok(Json.toJson(responseData));
			    }
				
				if(lessonSession == null){
					mediaFile.lessonSession = lessonSession;
				}
				
				jpaApi.em().persist(mediaFile);
				
				responseData.data = mediaFile;
				
				ObjectMapper mapper = new ObjectMapper();
				JsonNode jsonData;
				try {
					jsonData = mapper.readTree(Utils.toJson(ResponseData.class, responseData, "*.lesson"));
					return ok(Json.toJson(jsonData));
				} catch (IOException e) {
					responseData.message = e.getLocalizedMessage();
			    	responseData.code = 4001;
				}
			}
	    }else{
	    	responseData.message = "File cannot be empty.";
	    	responseData.code = 4000;
	    }
	    
	    return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result downloadFile(String mediaFileUUID){
		ResponseData responseData = new ResponseData();
		TypedQuery<MediaFile> query = jpaApi.em()
				.createQuery("from MediaFile mf where mf.uuid = :uuid", MediaFile.class)
				.setParameter("uuid", mediaFileUUID);
		
		try{
			MediaFile mediaFile = query.getSingleResult();
			InputStream fileStream = mediaFile.download();
			return ok(fileStream).withHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(mediaFile.name, "UTF-8"));
		}catch(NoResultException | UnsupportedEncodingException e){
			responseData.message = "Image cannot be found.";
	    	responseData.code = 4000;
		}
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result deleteFile(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
    	String uuid = requestData.get("uuid");
    	
    	TypedQuery<MediaFile> query = jpaApi.em()
				.createQuery("from MediaFile mf where mf.uuid = :uuid", MediaFile.class)
				.setParameter("uuid", uuid);
    	
    	try{
	    	MediaFile mediaFile = query.getSingleResult();
	    	mediaFile.delete();
			
			jpaApi.em().remove(mediaFile);
    	}catch(NoResultException e){
			responseData.message = "File does't exist.";
	    	responseData.code = 4000;
    	}
    	return ok(Json.toJson(responseData));
	}
}













