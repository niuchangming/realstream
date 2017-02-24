package controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opentok.Session;
import com.opentok.exception.OpenTokException;

import models.Account;
import models.Archive;
import models.BroadcastSession;
import models.Category;
import models.Lesson;
import models.LessonImage;
import models.LessonSession;
import models.PublishedLesson;
import models.ResponseData;
import models.User;
import play.Application;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.mvc.With;
import tools.BroadcastManager;
import tools.Constants;
import tools.Utils;
import views.html.broadcastlesson;
import views.html.createlesson;
import views.html.errorpage;
import views.html.lessonimage;
import views.html.lessonsession;
import views.html.lessondetail;
import views.html.lessonprice;
import views.html.broadcaststream;
import actions.AuthAction;

@SuppressWarnings("unchecked")
public class LessonController extends Controller{
	@Inject private FormFactory formFactory;
	@Inject private JPAApi jpaApi;
	@Inject private WSClient ws;
	@Inject private Provider<Application> application;
	
	@With(AuthAction.class)
	@Transactional
	public Result createLesson(){
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String lessonTitle = requestData.get("title");
		
		if(Utils.isBlank(lessonTitle)){
			flash("error", "Lesson title cannot be empty.");
			return redirect(routes.HomeController.index());
		}
		
		Account account = (Account) ctx().args.get("account");
		
		TypedQuery<User> userQuery = jpaApi.em()
				.createQuery("from User u where u.accountId = :accountId", User.class)
				.setParameter("accountId", account.id);
		
		User teacher = userQuery.getSingleResult();
		
		Lesson lesson = new Lesson(teacher, lessonTitle);
		jpaApi.em().persist(lesson);
		
		Query query = jpaApi.em().createQuery("from Category cg where cg.parent = null", Category.class);
		List<Category> categories = query.getResultList();
		
		return ok(createlesson.render(lesson, categories));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result editLesson(long lessonId){
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
		
		if(lesson == null){
			flash("error", "You do not have any lesson.");
			return redirect(routes.HomeController.index());
		}
		
		Query query = jpaApi.em().createQuery("from Category cg where cg.parent = null", Category.class);
		List<Category> categories = query.getResultList();
		
		return ok(createlesson.render(lesson, categories));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result saveLessonBasicInfo(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		long lessonId = Long.parseLong(requestData.get("id"));
		String lessonTitle = requestData.get("title");
		String lessonDesc = requestData.get("description");
		long categoryId = Long.parseLong(requestData.get("categoryId"));
		
		Category category = jpaApi.em().find(Category.class, categoryId);
		Lesson dbLesson = jpaApi.em().find(Lesson.class, lessonId);
		
		if(category == null){
			responseData.message = "You must set a category for this lesson.";
			responseData.code = 4000;
		}
		
		if(dbLesson == null){
			responseData.message = "The lesson cannot be found.";
			responseData.code = 4000;
		}
		
		dbLesson.updateByBasic(lessonTitle, lessonDesc, category);
		jpaApi.em().persist(dbLesson);
		
		responseData.data = dbLesson;
		
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonData = mapper.readTree(Utils.toJson(ResponseData.class, responseData, 
					"*.teacher", "*.students",  "*.lesson", "*.category", "*.lessonSessions", "*.lessonImages", "*.userLessons", "*.mediaFiles", "*.comments"));
			return ok(Json.toJson(jsonData));
		} catch (IOException e) {
			responseData.message = e.getLocalizedMessage();
			responseData.code = 4001;
		}
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result editLessonImages(long lessonId){
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
		
		if(lesson == null){
			flash().put("error", "The lesson cannot be found.");
			return redirect(routes.HomeController.index());
		}
		
		return ok(lessonimage.render(lesson));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result uploadLessonImage(){
		ResponseData responseData = new ResponseData();
		
		MultipartFormData<File> body = request().body().asMultipartFormData();
	    FilePart<File> imageFile = body.getFile("lessonImage");
	    
	    try {
		    if(imageFile != null){
		    	DynamicForm requestData = formFactory.form().bindFromRequest();
		    	long lessonId = Long.parseLong(requestData.get("lessonId"));
		    	
				Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
				
				if(lesson == null){
					responseData.message = "The lesson cannot be found.";
					responseData.code = 4000;
				}else{
					int imageCount = lesson.lessonImages == null ? 0 : lesson.lessonImages.size();
					if(imageCount >= LessonImage.IMAGE_MAX){
						responseData.message = "Lesson image count exceeds 15.";
						responseData.code = 4000;
					}else{
						LessonImage lessonImage = new LessonImage(lesson, imageFile.getFile());
						if(imageCount == 0){
							lessonImage.isCover = true;
						}
						
						jpaApi.em().persist(lessonImage);
						responseData.data = lessonImage;
						return ok(Utils.toJson(ResponseData.class, responseData, "*.lesson"));
					}
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
	public Result showLessonImage(String uuid, boolean isLarge){
		TypedQuery<LessonImage> query = jpaApi.em()
				.createQuery("from LessonImage li where li.uuid = :uuid", LessonImage.class)
				.setParameter("uuid", uuid);
		
		InputStream imageStream = null;
		try{
			LessonImage lessonImage = query.getSingleResult();
			if(isLarge){
				imageStream = lessonImage.download();
			}else{
				imageStream = lessonImage.downloadThumbnail();
			}
		}catch(NoResultException e){
			imageStream = application.get().classloader().getResourceAsStream(LessonImage.PLACEHOLDER);
		}
		return ok(imageStream);
	}
	
	@Transactional
	public Result showLessonCover(long lessonId, boolean isLarge){
		Query coverQuery = jpaApi.em()
				.createNativeQuery("select * from image where (lesson_id=:lessonId and is_cover=:isCover) or lesson_id=:lessonId limit 1", LessonImage.class)
				.setParameter("lessonId", lessonId)
				.setParameter("isCover", true);
		
		LessonImage lessonImage = null;
		InputStream imageStream = null;
		try{
			lessonImage = (LessonImage) coverQuery.getSingleResult();
		}catch(NoResultException e){
			imageStream = application.get().classloader().getResourceAsStream(LessonImage.PLACEHOLDER);
			return ok(imageStream);
		}
		
		if(isLarge){
			imageStream = lessonImage.download();
		}else{
			imageStream = lessonImage.downloadThumbnail();
		}
		
		return ok(imageStream);
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result deleteLessonImage(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
    	String imageUUID = requestData.get("imageUUID");
    	
    	TypedQuery<LessonImage> query = jpaApi.em()
				.createQuery("from LessonImage li where li.uuid = :uuid", LessonImage.class)
				.setParameter("uuid", imageUUID);
    	
    	try{
	    	LessonImage lessonImage = query.getSingleResult();
			lessonImage.deleteThumbnail();
			lessonImage.delete();
			
			jpaApi.em().remove(lessonImage);
    	}catch(NoResultException e){
			responseData.message = "Image does't exist.";
	    	responseData.code = 4000;
    	}
    	return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result updateLessonImageInfo(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String uuid = requestData.get("uuid");
    	String title = requestData.get("title");
    	String caption = requestData.get("caption");
    	boolean isCover = Integer.parseInt(requestData.get("isCover")) == 1;
    	
    	TypedQuery<LessonImage> query = jpaApi.em()
				.createQuery("from LessonImage li where li.uuid = :uuid", LessonImage.class)
				.setParameter("uuid", uuid);
    	
    	try{
    		LessonImage lessonImage = query.getSingleResult();
    		if(isCover){
    			jpaApi.em()
    			.createQuery("update LessonImage li set li.isCover = false where li.lesson = :lesson and li.isCover=:isCover")
    			.setParameter("isCover", true)
				.setParameter("lesson", lessonImage.lesson).executeUpdate();
        	}
    		
    		lessonImage.title = title;
    		lessonImage.caption = caption;
    		lessonImage.isCover = isCover;
    		jpaApi.em().persist(lessonImage);
    		
    		try {
    			responseData.data = lessonImage;
				return ok(Utils.toJson(ResponseData.class, responseData, "*.lesson"));
			} catch (JsonProcessingException e) {
				responseData.code = 4001;
				responseData.message = e.getLocalizedMessage();
			}
    	}catch(NoResultException e){
    		responseData.code = 4000;
			responseData.message = "Image does't exist.";
    	}
    	return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result lessonSession(long lessonId, int offset){
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
		
		if(lesson == null){
			flash().put("error", "The lesson cannot be found.");
			return redirect(routes.HomeController.index());
		}
		
		int totalAmount = ((Long)jpaApi.em()
				.createQuery("select count(*) from LessonSession ls where ls.lesson = :lesson")
				.setParameter("lesson", lesson).getSingleResult()).intValue();
		int pageIndex = (int) Math.ceil(offset / LessonSession.PAGE_SIZE) + 1;
		
		List<LessonSession> lessonSessions = jpaApi.em()
				.createQuery("from LessonSession ls where ls.lesson = :lesson order by ls.startDatetime asc", LessonSession.class)
				.setParameter("lesson", lesson)
				.setFirstResult(offset)
				.setMaxResults(LessonSession.PAGE_SIZE)
				.getResultList();
		
		return ok(lessonsession.render(lesson, lessonSessions, pageIndex, totalAmount));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result saveLessonSession(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		long lessonId = Long.parseLong(requestData.get("lessonId"));
		String title = requestData.get("title");
    	String brief = requestData.get("brief");
    	String duration = requestData.get("duration");
    	String startDatetime = requestData.get("startDatetime");
    	boolean interactive = Integer.parseInt(requestData.get("interactive")) == 1;
    	
    	Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
    	if(lesson == null){
    		responseData.message = "Lesson cannot be found.";
    		responseData.code = 4000;
    	}else{
    		LessonSession lessonSession = new LessonSession(lesson);
        	try {
    	    	lessonSession.title = title;
    	    	lessonSession.brief = brief;
    	    	lessonSession.duration = Integer.parseInt(duration);
    			lessonSession.startDatetime = Utils.parse(startDatetime);
    			lessonSession.endDatetime = Utils.addMinute(lessonSession.startDatetime, lessonSession.duration);
    			lessonSession.interactive = interactive;
    			jpaApi.em().persist(lessonSession);
    			
    			boolean updateLesson = false;
    			if(lesson.startDatetime == null || lesson.startDatetime.after(lessonSession.startDatetime)){
    				lesson.startDatetime = lessonSession.startDatetime;
    				updateLesson = true;
    			}
    			
    			if(lesson.endDatetime == null || lesson.endDatetime.before(lessonSession.endDatetime)){
    				lesson.endDatetime = lessonSession.endDatetime;
    				updateLesson = true;
    			}
    			
    			if(updateLesson){
    				jpaApi.em().persist(lesson);
    			}
    			
    			//update lesson session count for publish lesson object
    			if(lesson.isPublish){
    	    		jpaApi.em()
    	    		.createNativeQuery("update publish_lesson pl set pl.lesson_session_count = :lessonSessionCount where pl.lesson_id = :lessonId")
    	    		.setParameter("lessonSessionCount", lesson.lessonSessions.size() + 1)
    				.setParameter("lessonId", lesson.id).executeUpdate();
    			}
    			
    			flash().put("success", "An new lesson session created successfully.");
    			
    			return redirect(routes.LessonController.lessonSession(lessonId, 0));
    		} catch (ParseException e) {
    			responseData.code = 4001;
    			responseData.message = e.getLocalizedMessage();
    		}
    	}
    	return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result lessonSessions(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		long lessonId = Long.parseLong(requestData.get("lessonId"));
		
		List<LessonSession> lessonSessions = jpaApi.em()
				.createNativeQuery("SELECT * FROM lesson_session ls WHERE ls.lesson_id=:lesson_id ORDER BY ls.start_datetime", LessonSession.class)
				.setParameter("lesson_id", lessonId)
				.getResultList();
		
		if(lessonSessions == null){
			responseData.code = 4000;
			responseData.message = "There is no any session found for this lesson.";
		}else{
			try {
				responseData.data = lessonSessions;
				ObjectMapper mapper = new ObjectMapper();
				JsonNode jsonData = mapper.readTree(Utils.toJson(ResponseData.class, responseData, "*.lesson", "*.broadcastSessions", "*.mediaFiles"));
				return ok(Json.toJson(jsonData));
			} catch (IOException e) {
				responseData.code = 4001;
				responseData.message = e.getLocalizedMessage();
			} 
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result lessonDetail(long lessonId){
		ResponseData responseData = new ResponseData();
		
		String token = ctx().session().get(AuthAction.LOGGED_KEY);
		Account account = null;
		if(!Utils.isBlank(token)){
			account = Account.findByToken(token);
		}

		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
    	if(lesson == null){
    		responseData.message = "Lesson cannot be found.";
    		responseData.code = 4000;
    		return notFound(errorpage.render(responseData));
    	}
    	
		return ok(lessondetail.render(account, lesson));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result lessonPrice(long lessonId){
		ResponseData responseData = new ResponseData();
		
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
    	if(lesson == null){
    		responseData.message = "Lesson cannot be found.";
    		responseData.code = 4000;
    		return notFound(errorpage.render(responseData));
    	}
    	
		return ok(lessonprice.render(lesson));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result saveLessonPrice(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		Map<String, String[]> formMap = request().body().asFormUrlEncoded();
		
		long lessonId = Long.parseLong(requestData.get("lessonId"));
		double price = Double.parseDouble(requestData.get("price"));
		double offerPrice = Double.parseDouble(requestData.get("offerPrice"));
		String offerStartDate = requestData.get("offerStartDate") + " 00:00";
		String offerEndDate = requestData.get("offerEndDate") + " 00:00";
		String[] ids = formMap.get("lessonSessions[]");	
		
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
    	if(lesson == null){
    		responseData.message = "Lesson cannot be found.";
    		responseData.code = 4000;
    	}else{
    		lesson.price = price;
        	lesson.offerPrice = offerPrice;
        	try {
        		if(Utils.isBlank(offerStartDate) && Utils.isBlank(offerEndDate)){
        			lesson.offerStartDate = Utils.parse(offerStartDate);
        			lesson.offerEndDate = Utils.parse(offerEndDate);
        		}
    			
    			if(ids != null && ids.length > 0){
    				String updateQueryStr = "update LessonSession ls set ls.isTrial = true where ";
                	for(int i = 0; i < ids.length; i++){
                		if(i == 0){
                			updateQueryStr += "ls.id = " + ids[i];
                		}else{
                			updateQueryStr += " or ls.id = " + ids[i];
                		}
                	}
                	
                	jpaApi.em().createQuery("update LessonSession ls set ls.isTrial = false where ls.lesson=:lesson")
                	.setParameter("lesson", lesson).executeUpdate(); //set all lesson session isTrail to false
                	jpaApi.em().createQuery(updateQueryStr).executeUpdate();
    			}
    		} catch (ParseException e) {
    			responseData.code = 4001;
    			responseData.message = e.getLocalizedMessage();
    		}
    	}
    	return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result publishLesson(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		
		long lessonId = Long.parseLong(requestData.get("lessonId"));
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
    	if(lesson == null){
    		responseData.message = "Lesson cannot be found.";
    		responseData.code = 4000;
    	}else if(Utils.isBlank(lesson.title)){
    		responseData.message = "Title must be set.";
    		responseData.code = 5000;
    	}else if(Utils.isBlank(lesson.description)){
    		responseData.message = "Description must be set.";
    		responseData.code = 5000;
    	}else if(lesson.price == 0){
    		responseData.message = "Price must be set.";
    		responseData.code = 5000;
    	}else if(lesson.isPublish){
    		responseData.message = "The lesson already published.";
    		responseData.code = 5000;
    	}else if(lesson.lessonImages == null || lesson.lessonImages.size() == 0){
    		responseData.message = "Lesson image must be set.";
    		responseData.code = 5000;
    	}else if(lesson.lessonSessions == null || lesson.lessonSessions.size() == 0){
    		responseData.message = "There is no any lesson session in this lesson.";
    		responseData.code = 5000;
    	}else{
    		lesson.isPublish = true;
    		jpaApi.em().persist(lesson);
    		
    		PublishedLesson publishLesson = new PublishedLesson(lesson);
    		jpaApi.em().persist(publishLesson);
    		
    		responseData.message = "The lesson was published successfully.";
    	}
		
    	return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result pubishedLessonsByCategory(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String categoryId = requestData.get("categoryId");
		int offset = requestData.get("offset") == null ? 0 : Integer.parseInt(requestData.get("offset"));
		
		List<PublishedLesson> publishLessons = jpaApi.em()
				.createNativeQuery("SELECT * FROM publish_lesson pl WHERE pl.category_id = :categoryId ORDER BY pl.published_datetime DESC", PublishedLesson.class)
				.setParameter("categoryId", categoryId)
				.setFirstResult(offset)
				.setMaxResults(PublishedLesson.PAGE_SIZE)
				.getResultList();
		responseData.data = publishLessons;
		
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode jsonData = mapper.readTree(Utils.toJson(ResponseData.class, responseData, "*.lesson"));
			return ok(jsonData);
		} catch (IOException e) {
			responseData.message = e.getLocalizedMessage();
    		responseData.code = 4001;
		} 
		
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result broadcastLessonSession(long lessonSessionId){
		ResponseData responseData = new ResponseData();
		
		LessonSession lessonSession = jpaApi.em().find(LessonSession.class, lessonSessionId);
		
		if(lessonSession == null){
			responseData.code = 4000;
			responseData.message = "Lesson Session cannot be found.";
		}else{
			try {
				Map<String, String> metaData = new HashMap<String, String>();
				Account account = (Account) ctx().args.get("account");
				metaData.put("account_token", account.token);
				
				TypedQuery<User> query = jpaApi.em()
						.createQuery("from User u where u.accountId = :accountId", User.class)
						.setParameter("accountId", account.id);
				User user = query.getSingleResult();
				
				Session tokSession = BroadcastManager.getInstance().createSession();
				String token = BroadcastManager.getInstance().createToken(tokSession.getSessionId(), (System.currentTimeMillis() / 1000L) + (4 * 60 * 60), metaData);
				
				BroadcastSession broadcastSession = new BroadcastSession(lessonSession);
				broadcastSession.sessionId = tokSession.getSessionId();
				broadcastSession.token = token;
				
				jpaApi.em().persist(broadcastSession);
				return ok(broadcastlesson.render(user, lessonSession, broadcastSession));
			} catch (NoResultException | OpenTokException e) {
				responseData.code = 4001;
				responseData.message = e.getLocalizedMessage();
			}
		}
		
		return notFound(errorpage.render(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result getBroadcastInfo(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String sessionId = requestData.get("sessionId");
		
		try{
			BroadcastSession broadcastSession = jpaApi.em()
					.createQuery("from BroadcastSession bs where bs.sessionId=:sessionId", BroadcastSession.class)
					.setParameter("sessionId", sessionId)
					.getSingleResult();
			
			JsonNode result = startBroadcast(broadcastSession.sessionId);
			if(!result.has("id")){
				responseData.code = 4002;
				responseData.message = result.get("message").asText();
			}else{
				broadcastSession.broadcastId = result.get("id").asText();
				if(result.has("broadcastUrls")){
					broadcastSession.hls = result.get("broadcastUrls").get("hls").asText();
				}
				jpaApi.em().persist(broadcastSession);
				
				responseData.message = "Publishing";
				responseData.data = broadcastSession;
				
				ObjectMapper mapper = new ObjectMapper();
				JsonNode jsonData = mapper.readTree(Utils.toJson(ResponseData.class, responseData, "*.lessonSession", "*.archives"));
				
				return ok(Json.toJson(jsonData));
			}
		}catch(NoResultException | InterruptedException | ExecutionException | IOException e){
			responseData.code = 4001;
			if(e instanceof NoResultException){
				responseData.code = 4000;
			}
			responseData.message = "The lesson session does not exist.";
		}
		
		return ok(Json.toJson(responseData));
	}

	@With(AuthAction.class)
	@Transactional
    public Result joinLessonSession(long lessonSessionId) {
		ResponseData responseData = new ResponseData();
		
		BroadcastSession broadcastSession = (BroadcastSession) jpaApi.em()
				.createNativeQuery("SELECT * FROM broadcast_session bs WHERE bs.lesson_session_id=:lessonSessionId ORDER BY bs.creation_datetime DESC LIMIT 1", BroadcastSession.class)
				.setParameter("lessonSessionId", lessonSessionId).getSingleResult();
		
		if(broadcastSession != null){
			Map<String, String> metaData = new HashMap<String, String>();
			Account account = (Account) ctx().args.get("account");
			metaData.put("account_token", account.token);
			
			try {
				TypedQuery<User> userQuery = jpaApi.em()
						.createQuery("from User u where u.accountId = :accountId", User.class)
						.setParameter("accountId", account.id);
				User user = userQuery.getSingleResult();
				
				String token = BroadcastManager.getInstance()
						.createToken(broadcastSession.sessionId, (System.currentTimeMillis() / 1000L) + (4 * 60 * 60), metaData);
				
				return ok(broadcaststream.render(user, broadcastSession, token));
			} catch (OpenTokException e) {
				responseData.code = 4001;
				responseData.message = e.getLocalizedMessage();
			} catch (NoResultException e){
				responseData.code = 400;
				responseData.message = "The user does not exist.";
			}
		}else{
			responseData.code = 4000;
			responseData.message = "Broadcast Session cannot be found.";
		}
		
		return notFound(errorpage.render(responseData)); 
    }
		
	public JsonNode startBroadcast(String sessionId) throws InterruptedException, ExecutionException{
		JsonNode sessionIdJson = Json.newObject().put("sessionId", sessionId);
		
		CompletionStage<WSResponse> responsePromise = ws.url("https://api.opentok.com/v2/project/" + Constants.TOKBOX_API_KEY + "/broadcast/")
				.setHeader("X-OPENTOK-AUTH", Utils.getJWTString(10))
				.setContentType("application/json")
				.post(sessionIdJson);
		
		return responsePromise.thenApply(WSResponse::asJson).toCompletableFuture().get();
	}
	
	@Transactional
	public Result openTokCallback(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String archiveId = requestData.get("id");
		String name = requestData.get("name");
		String sessionId = requestData.get("sessionId");
		String status = requestData.get("status");
		String url = requestData.get("url");
		long size = Long.parseLong(requestData.get("size"));
		long createAt = Long.parseLong(requestData.get("createdAt"));
		long duration = Long.parseLong(requestData.get("duration"));
	
		try{
			BroadcastSession broadcastSession = jpaApi.em()
					.createQuery("from BroadcastSession bs where bs.sessionId=:sessionId", BroadcastSession.class)
					.setParameter("sessionId", sessionId)
					.getSingleResult();
			
			Archive archive = new Archive(broadcastSession);
			archive.archiveId = archiveId;
			archive.creationDateTime = new Date(createAt);
			archive.name = name;
			archive.sessionId = sessionId;
			archive.status = status;
			archive.url = url;
			archive.size = size;
			archive.duration = duration;
			
			jpaApi.em().persist(archive);
		}catch(NoResultException e){
			responseData.code = 4000;
			responseData.message = "BroadcastSession cannot be found.";
		}
		
		return ok(Json.toJson(responseData));
	}
	
}


