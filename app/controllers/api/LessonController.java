package controllers.api;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.persistence.TypedQuery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import models.BroadcastSession;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;
import tools.Constants;
import tools.Utils;

public class LessonController extends Controller{
	private final JPAApi jpaApi;
	private final WSClient ws;
	
	@Inject
	public LessonController(WSClient ws, JPAApi api) {
	    this.jpaApi = api;
	    this.ws = ws;
	}
	
	@Transactional
    public Result joinLesson() {
    	
    	String sessionId = session("tokbox_session");
    	
//    	return CompletableFuture.supplyAsync(() -> jpaApi.withTransaction("default", true, ()-> {
//    		
//    		TypedQuery<LessonSession> query = jpaApi.em().createQuery("from LessonSession ls where ls.sessionId = :sessionId", LessonSession.class)
//    	            .setParameter("sessionId", sessionId);
//			LessonSession lessonSession = query.getResultList().get(0);
//			
//	        return lessonSession;
//    		
//    	})).thenApply(lessonSession -> ok(Json.toJson(lessonSession)));
    	
    	
    	
    	TypedQuery<BroadcastSession> query = jpaApi.em().createQuery("from LessonSession ls where ls.sessionId = :sessionId", BroadcastSession.class)
	            .setParameter("sessionId", sessionId);
		BroadcastSession lessonSession = query.getResultList().get(0);
		
		if(Utils.isBlank(lessonSession.broadcastId)){
			String broadcastId = startBroadcast(lessonSession.sessionId);
			lessonSession.broadcastId = broadcastId;
			jpaApi.em().persist(lessonSession);
		}
		
		return ok(Json.toJson(lessonSession));
    }
	
	public String startBroadcast(String sessionId){
		ObjectNode sessionIdJson = Json.newObject();
		sessionIdJson.put("sessionId", sessionId);
		
		String broadcastId = "";
		
		WSRequest request = ws.url("https://api.opentok.com/v2/project/" + Constants.TOKBOX_API_KEY + "/broadcast")
				.setHeader("X-OPENTOK-AUTH", Utils.getJWTString(5))
				.setContentType("application/json")
				.setMethod("POST")
				.setRequestTimeout(30 * 1000)
				.setBody(sessionIdJson.asText());
		
		CompletionStage<JsonNode> responsePromise = request.get().thenApply(WSResponse::asJson);
		try {
			JsonNode jsonData = responsePromise.toCompletableFuture().get();
			
			System.out.println("-------------> " + jsonData.toString());
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		
		return broadcastId;
		
	}
    
}


