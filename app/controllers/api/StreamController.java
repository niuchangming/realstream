package controllers.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.NoResultException;

import com.opentok.exception.OpenTokException;

import modelVOs.ChatMessageVO;
import modelVOs.StreamVO;
import models.Account;
import models.BroadcastSession;
import models.ChatMessage;
import models.ResponseData;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.mvc.Controller;
import play.mvc.Result;
import tools.BroadcastManager;
import tools.Utils;

public class StreamController extends Controller{
	@Inject private JPAApi jpaApi;
	@Inject private WSClient ws;
	@Inject private FormFactory formFactory;
	
	@Transactional
    public Result joinLessonSession() {
		ResponseData responseData = new ResponseData();
		
		try {
			DynamicForm requestData = formFactory.form().bindFromRequest();
			long lessonSessionId = Long.parseLong(requestData.get("lessonSessionId"));
			String token = requestData.get("token");
			
			Account account = jpaApi.em().createQuery("from Account a where a.token = :token", Account.class)
					.setParameter("token", token)
					.getSingleResult();
			
			BroadcastSession broadcastSession = (BroadcastSession) jpaApi.em()
					.createNativeQuery("SELECT * FROM broadcast_session bs WHERE bs.lesson_session_id=:lessonSessionId ORDER BY bs.creation_datetime DESC LIMIT 1", BroadcastSession.class)
					.setParameter("lessonSessionId", lessonSessionId).getSingleResult();
			
			Map<String, String> metaData = new HashMap<String, String>();
			metaData.put("account_token", account.token);
			
			//替换原来到token为此用户的opentok session的token
			broadcastSession.token = BroadcastManager.getInstance()
					.createToken(broadcastSession.sessionId, (System.currentTimeMillis() / 1000L) + (4 * 60 * 60), metaData);
			
			StreamVO streamVO = new StreamVO(broadcastSession);
			responseData.data = streamVO;
		} catch (NoResultException e){
			responseData.code = 400;
			responseData.message = "Something does not exist.";
		} catch (OpenTokException e) {
			e.printStackTrace();
		}
		return ok(Json.toJson(responseData));
    }
	
	@Transactional
	public Result saveMessage(){
		ResponseData responseData = new ResponseData();
		try {
			DynamicForm requestData = formFactory.form().bindFromRequest();
			String sessionId = requestData.get("sessionId");
			String token = requestData.get("token");
			String message = requestData.get("message");
			
			if(!Utils.isBlank(message)){
				Account account = jpaApi.em().createQuery("from Account a where a.token = :token", Account.class)
						.setParameter("token", token)
						.getSingleResult();
				
				BroadcastSession broadcastSession = (BroadcastSession) jpaApi.em()
						.createNativeQuery("SELECT * FROM broadcast_session bs WHERE bs.session_id=:sessionId", BroadcastSession.class)
						.setParameter("sessionId", sessionId).getSingleResult();
				
				ChatMessage chatMessage = new ChatMessage(account.user, broadcastSession);
				chatMessage.message = message;
				jpaApi.em().persist(chatMessage);
			}
		} catch (NoResultException e){
			responseData.code = 400;
			responseData.message = "Something does not exist.";
		}
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result lessonSessionChatMessages(){
		ResponseData responseData = new ResponseData();
		try {
			DynamicForm requestData = formFactory.form().bindFromRequest();
			String lessonLessionId = requestData.get("lessonSessionId");
			String token = requestData.get("token");
			try {
				jpaApi.em().createQuery("from Account a where a.token = :token", Account.class).setParameter("token", token).getSingleResult();
			} catch (NoResultException e){
				responseData.code = 400;
				responseData.message = "Account does not exist.";
			}
		
			BroadcastSession broadcastSession = (BroadcastSession) jpaApi.em()
					.createNativeQuery("SELECT * FROM broadcast_session bs WHERE bs.lesson_session_id=:lessonSessionId ORDER BY bs.creation_datetime DESC LIMIT 1", BroadcastSession.class)
					.setParameter("lessonSessionId", lessonLessionId).getSingleResult();
				
			List<ChatMessageVO> chatMessageVOs = ChatMessageVO.getChatMessageVOs(broadcastSession.chatMessages);
			responseData.data = chatMessageVOs;
		} catch (NoResultException e){
			responseData.code = 400;
			responseData.message = "Broadcast Session does not exist.";
		}
		return ok(Json.toJson(responseData));
	}
	
	
	
//	public String startBroadcast(String sessionId){
//		ObjectNode sessionIdJson = Json.newObject();
//		sessionIdJson.put("sessionId", sessionId);
//		
//		String broadcastId = "";
//		
//		WSRequest request = ws.url("https://api.opentok.com/v2/project/" + Constants.TOKBOX_API_KEY + "/broadcast")
//				.setHeader("X-OPENTOK-AUTH", Utils.getJWTString(5))
//				.setContentType("application/json")
//				.setMethod("POST")
//				.setRequestTimeout(10 * 1000)
//				.setBody(sessionIdJson.asText());
//		
//		CompletionStage<JsonNode> responsePromise = request.get().thenApply(WSResponse::asJson);
//		try {
//			JsonNode jsonData = responsePromise.toCompletableFuture().get();
//			
//			System.out.println("-------------> " + jsonData.toString());
//		} catch (InterruptedException | ExecutionException e) {
//			e.printStackTrace();
//		}
//		
//		return broadcastId;
//	}
}
