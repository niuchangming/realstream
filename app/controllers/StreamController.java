package controllers;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opentok.Session;
import com.opentok.exception.OpenTokException;

import actions.AuthAction;
import models.Account;
import models.Archive;
import models.BroadcastSession;
import models.ChatMessage;
import models.LessonSession;
import models.ResponseData;
import models.User;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import tools.BroadcastManager;
import tools.Constants;
import tools.Utils;
import views.html.broadcastlesson;
import views.html.broadcaststream;
import views.html.errorpage;

public class StreamController extends Controller{
	@Inject private FormFactory formFactory;
	@Inject private JPAApi jpaApi;
	@Inject private WSClient ws;
	
	@With(AuthAction.class)
	@Transactional
	public Result broadcastLessonSession(long lessonSessionId){
		ResponseData responseData = new ResponseData();
		
		LessonSession lessonSession = jpaApi.em().find(LessonSession.class, lessonSessionId);
		
		if(lessonSession == null){
			responseData.code = 4000;
			responseData.message = "Lesson Session cannot be found.";
		}else{
			BroadcastSession broadcastSession;
			try{
				broadcastSession = jpaApi.em()
					.createQuery("FROM BroadcastSession bs WHERE bs.lessonSession=:lessonSession", BroadcastSession.class)
					.setParameter("lessonSession", lessonSession).getSingleResult();
			}catch(NoResultException e){
				broadcastSession = new BroadcastSession(lessonSession);
			}
					
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
				JsonNode jsonData = mapper.readTree(Utils.toJson(ResponseData.class, responseData, "*.lessonSession", "*.archives", "*.chatMessages"));
				
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
		try {
			BroadcastSession broadcastSession = (BroadcastSession) jpaApi.em()
					.createNativeQuery("SELECT * FROM broadcast_session bs WHERE bs.lesson_session_id=:lessonSessionId ORDER BY bs.creation_datetime DESC LIMIT 1", BroadcastSession.class)
					.setParameter("lessonSessionId", lessonSessionId).getSingleResult();
			
			Map<String, String> metaData = new HashMap<String, String>();
			Account account = (Account) ctx().args.get("account");
			metaData.put("account_token", account.token);
			
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
			responseData.message = "Something does not exist.";
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
		
		if(status.equals("uploaded")){
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
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result saveMessage(){
		ResponseData responseData = new ResponseData();
		try {
			DynamicForm requestData = formFactory.form().bindFromRequest();
			String sessionId = requestData.get("sessionId");
			String message = requestData.get("message");
			
			if(!Utils.isBlank(message)){
				Account account = (Account) ctx().args.get("account");
				User user = jpaApi.em()
						.createQuery("from User u where u.accountId = :accountId", User.class)
						.setParameter("accountId", account.id).getSingleResult();
				
				BroadcastSession broadcastSession = (BroadcastSession) jpaApi.em()
						.createNativeQuery("SELECT * FROM broadcast_session bs WHERE bs.session_id=:sessionId", BroadcastSession.class)
						.setParameter("sessionId", sessionId).getSingleResult();
				
				ChatMessage chatMessage = new ChatMessage(user, broadcastSession);
				chatMessage.message = message;
				jpaApi.em().persist(chatMessage);
			}
		} catch (NoResultException e){
			responseData.code = 400;
			responseData.message = "Something does not exist.";
		}
		return ok(Json.toJson(responseData));
	}

}
