package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name="chat_message")
public class ChatMessage {
	@Id
	@GeneratedValue	
	@JsonIgnore
	public long id;
	
	@Lob
	public String message;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="send_datetime")
	public Date sendDatetime;

	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") 
    public User user;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "broadcast_session_id")
	public BroadcastSession broadcastSession;
	
	public ChatMessage(User user, BroadcastSession broadcastSession){
		this.user = user;
		this.broadcastSession = broadcastSession;
		this.sendDatetime = new Date();
	}
}
