package controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opentok.Session;
import com.opentok.exception.OpenTokException;

import models.Account;
import models.BroadcastSession;
import models.Category;
import models.Lesson;
import models.LessonImage;
import models.LessonSession;
import models.ResponseData;
import models.User;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
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
	@Inject
    private FormFactory formFactory;
	
	@Inject
	private JPAApi jpaApi;
	
	@Inject
	private WSClient ws;
	
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
		
		User user = userQuery.getSingleResult();
		
		Lesson lesson = new Lesson(user, lessonTitle);
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
			return ok(Utils.toJson(ResponseData.class, responseData, "*.user", "*.lesson", "*.category", "*.lessonSessions", "*.lessonImages"));
		} catch (JsonProcessingException e) {
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
				}
				
				int imageCount = lesson.lessonImages.size();
				if(imageCount >= LessonImage.IMAGE_MAX){
					responseData.message = "Lesson image count exceeds 15.";
					responseData.code = 4000;
				}
				
				LessonImage lessonImage = new LessonImage(lesson, imageFile.getFile());
				if(imageCount == 0){
					lessonImage.isCover = true;
				}
				
				jpaApi.em().persist(lessonImage);
				
				responseData.data = lessonImage;
				
				return ok(Utils.toJson(ResponseData.class, responseData, "*.lesson"));
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
	public Result showLessonThumbnail(String thumbnailUUID){
		ResponseData responseData = new ResponseData();
		TypedQuery<LessonImage> query = jpaApi.em()
				.createQuery("from LessonImage li where li.thumbnailUUID = :thumbnailUUID", LessonImage.class)
				.setParameter("thumbnailUUID", thumbnailUUID);
		
		try{
			LessonImage lessonImage = query.getSingleResult();
			InputStream imageStream = lessonImage.downloadThumbnail();
			return ok(imageStream);
		}catch(NoResultException e){
			responseData.message = "Image cannot be found.";
	    	responseData.code = 4000;
		}
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result showLessonCover(long lessonId, boolean isLarge){
		ResponseData responseData = new ResponseData();
		Query coverQuery = jpaApi.em()
				.createNativeQuery("select * from image where lesson_id=:lessonId and is_cover=:isCover", LessonImage.class)
				.setParameter("lessonId", lessonId)
				.setParameter("isCover", true);
		
		try{
			LessonImage lessonImage;
			try{
				lessonImage = (LessonImage) coverQuery.getSingleResult();
			}catch(NoResultException e){
				Query lessonImageQuery = jpaApi.em()
						.createNativeQuery("select * from image where lesson_id=:lessonId limit 1", LessonImage.class)
						.setParameter("lessonId", lessonId);
				lessonImage = (LessonImage) lessonImageQuery.getSingleResult();
			}
			InputStream imageStream;
			if(isLarge){
				imageStream = lessonImage.download();
			}else{
				imageStream = lessonImage.downloadThumbnail();
			}
			return ok(imageStream);
		}catch(NoResultException e){
			responseData.message = "Image cannot be found.";
	    	responseData.code = 4000;
		}
		return ok(Json.toJson(responseData));
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
    	}
    	
    	LessonSession lessonSession = new LessonSession(lesson);
    	try {
	    	lessonSession.title = title;
	    	lessonSession.brief = brief;
	    	lessonSession.duration = Integer.parseInt(duration);
			lessonSession.startDatetime = Utils.parse(startDatetime);
			lessonSession.endDatetime = Utils.addMinute(lessonSession.startDatetime, lessonSession.duration);
			lessonSession.interactive = interactive;
			jpaApi.em().persist(lessonSession);
			
			flash().put("success", "An new lesson session created successfully.");
			
			return redirect(routes.LessonController.lessonSession(lessonId, 0));
		} catch (ParseException e) {
			responseData.code = 4001;
			responseData.message = e.getLocalizedMessage();
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
    			lesson.offerStartDate = Utils.parse(offerStartDate);
    			lesson.offerEndDate = Utils.parse(offerEndDate);
    			
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
            	
    		} catch (ParseException e) {
    			responseData.code = 4001;
    			responseData.message = e.getLocalizedMessage();
    		}
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
				Session tokSession = BroadcastManager.getInstance().createSession();
				String token = BroadcastManager.getInstance().createToken(tokSession, (System.currentTimeMillis() / 1000L) + (4 * 60 * 60));
				
				BroadcastSession broadcastSession = new BroadcastSession(lessonSession);
				broadcastSession.sessionId = tokSession.getSessionId();
				broadcastSession.token = token;
				
				jpaApi.em().persist(broadcastSession);
				return ok(broadcastlesson.render(broadcastSession));
			} catch (OpenTokException e) {
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
				JsonNode jsonData = mapper.readTree(Utils.toJson(ResponseData.class, responseData, "*.lessonSession"));
				
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
    public Result joinLesson(long broadcastSessionId) {
		ResponseData responseData = new ResponseData();
		
		BroadcastSession broadcastSession = jpaApi.em().find(BroadcastSession.class, broadcastSessionId);
		if(broadcastSession == null){
			responseData.code = 4000;
			responseData.message = "Broadcast Session cannot be found.";
		}else{
			return ok(broadcaststream.render(broadcastSession));
		}
		
		return notFound(errorpage.render(responseData)); 
    }
	
	public JsonNode startBroadcast(String sessionId) throws InterruptedException, ExecutionException{
		JsonNode sessionIdJson = Json.newObject().put("sessionId", sessionId);
		
		CompletionStage<WSResponse> responsePromise = ws.url("https://api.opentok.com/v2/project/" + Constants.TOKBOX_API_KEY + "/broadcast/")
				.setHeader("X-OPENTOK-AUTH", Utils.getJWTString(5))
				.setContentType("application/json")
				.post(sessionIdJson);
		
		return responsePromise.thenApply(WSResponse::asJson).toCompletableFuture().get();
	}
}


