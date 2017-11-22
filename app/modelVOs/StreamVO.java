package modelVOs;

import models.BroadcastSession;

public class StreamVO {
	public String sessionId;
	public String hls;
	public String token;
	
	public StreamVO (BroadcastSession broadcastSession){
		this.sessionId = broadcastSession.sessionId;
		this.hls = broadcastSession.hls;
		this.token = broadcastSession.token;
	}
}
