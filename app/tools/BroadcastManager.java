package tools;

import com.opentok.ArchiveMode;
import com.opentok.MediaMode;
import com.opentok.OpenTok;
import com.opentok.Role;
import com.opentok.Session;
import com.opentok.SessionProperties;
import com.opentok.TokenOptions;
import com.opentok.exception.OpenTokException;

public class BroadcastManager {
	
	public static BroadcastManager broadcastManager;
	
	public static BroadcastManager getInstance(){
		if(broadcastManager == null){
			broadcastManager = new BroadcastManager();
		}
		return broadcastManager;
	}
	
	public Session createSession() throws OpenTokException{
		OpenTok opentok = new OpenTok(Constants.TOKBOX_API_KEY, Constants.TOKBOX_SECRET);
		Session tokSession = opentok.createSession(new SessionProperties.Builder()
					  .mediaMode(MediaMode.ROUTED)
					  .archiveMode(ArchiveMode.ALWAYS)
					  .build());
		
		return tokSession;
	}
	
	public String createToken(Session session, long expire) throws OpenTokException{
		String token = session.generateToken(new TokenOptions.Builder()
				  .role(Role.PUBLISHER)
				  .expireTime(expire)
				  .build());
		return token;
	}	

}
