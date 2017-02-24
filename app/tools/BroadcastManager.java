package tools;

import java.util.Iterator;
import java.util.Map;

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
	
	public String createToken(String sessionId, long expire, Map<String, String> metaData) throws OpenTokException{
		String connectionMetadata = "";
		if(metaData != null){
			Iterator<String> iterator = metaData.keySet().iterator();
			while(iterator.hasNext()){
				String key = iterator.next();
				connectionMetadata += key + "=" + metaData.get(key) + ",";
			}
		}
		
		if(!Utils.isBlank(connectionMetadata) && connectionMetadata.length() > 0){
			connectionMetadata = connectionMetadata.substring(0, connectionMetadata.length() - 1);
		}
		
		OpenTok opentok = new OpenTok(Constants.TOKBOX_API_KEY, Constants.TOKBOX_SECRET);
		
		TokenOptions tokenOpts = new TokenOptions.Builder()
		          .role(Role.PUBLISHER)
		          .expireTime(expire)
		          .data(connectionMetadata)
		          .build();
		
		String token = opentok.generateToken(sessionId, tokenOpts);
		return token;
	}	

}
