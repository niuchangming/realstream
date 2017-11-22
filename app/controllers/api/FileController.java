package controllers.api;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import modelVOs.MediaFileVO;
import models.Account;
import models.MediaFile;
import models.ResponseData;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

@SuppressWarnings("unchecked")
public class FileController extends Controller{
	@Inject private FormFactory formFactory;
	@Inject private JPAApi jpaApi;
	
	@Transactional
	public Result downloadFile(String uuid, boolean isDownload){
		ResponseData responseData = new ResponseData();
		try{
			TypedQuery<MediaFile> query = jpaApi.em()
					.createQuery("from MediaFile mf where mf.uuid = :uuid", MediaFile.class)
					.setParameter("uuid", uuid);
		
			MediaFile mediaFile = query.getSingleResult();
			InputStream fileStream = mediaFile.download();
			return ok(fileStream).withHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(mediaFile.name, "UTF-8"));
		}catch(UnsupportedEncodingException e){
			responseData.message = "File cannot be found.";
	    	responseData.code = 4000;
		}
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result lessonFiles(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		long lessonId = Long.parseLong(requestData.get("lessonId"));
		String token = requestData.get("token");
		
		try{
			jpaApi.em().createQuery("from Account a where a.token = :token", Account.class)
					.setParameter("token", token)
					.getSingleResult();
			
			List<MediaFile> mediaFiles = jpaApi.em()
					.createNativeQuery("SELECT * FROM media_file mf where mf.lesson_id = :lessonId", MediaFile.class)
					.setParameter("lessonId", lessonId)
					.getResultList();
			
			List<MediaFileVO> mediaFileVOs = MediaFileVO.getMediaFileVOs(mediaFiles);
			responseData.data = mediaFileVOs;
		}catch(NoResultException e){
			responseData.code = 4000;
			responseData.message = "Account does not exist.";
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result lessonSessionFiles(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		long lessonSessionId = Long.parseLong(requestData.get("lesson_session_id"));
		String token = requestData.get("token");
		
		try{
			jpaApi.em().createQuery("from Account a where a.token = :token", Account.class)
			.setParameter("token", token)
			.getSingleResult();
			
			List<MediaFile> mediaFiles = jpaApi.em()
					.createNativeQuery("SELECT * FROM media_file mf where mf.lesson_id = :lessonId", MediaFile.class)
					.setParameter("lesson_session_id", lessonSessionId)
					.getResultList();
			
			List<MediaFileVO> mediaFileVOs = MediaFileVO.getMediaFileVOs(mediaFiles);
			responseData.data = mediaFileVOs;
		}catch(NoResultException e){
			responseData.code = 4000;
			responseData.message = "Account does not exist.";
		}
		
		return ok(Json.toJson(responseData));
	}

}












