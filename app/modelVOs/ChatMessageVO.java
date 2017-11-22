package modelVOs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.ChatMessage;

public class ChatMessageVO {
	public String message;
	public Date sendDatetime; 
	public String username;
	public String thumbnailUUID;
	
	public ChatMessageVO(ChatMessage chatMessage){
		this.message = chatMessage.message;
		this.sendDatetime = chatMessage.sendDatetime;
		this.username = chatMessage.user.username;
		if(chatMessage.user.avatars != null && chatMessage.user.avatars.size() > 0){
			this.thumbnailUUID = chatMessage.user.avatars.get(0).thumbnailUUID;
		}
	}
	
	public static List<ChatMessageVO> getChatMessageVOs(List<ChatMessage> chatMessages){
		List<ChatMessageVO> chatMessageVOs = new ArrayList<ChatMessageVO>(chatMessages.size());
		for(ChatMessage chatMessage : chatMessages){
			ChatMessageVO chatMessageVO = new ChatMessageVO(chatMessage);
			chatMessageVOs.add(chatMessageVO);
		}
		return chatMessageVOs;
	}
}
