package models;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name="lesson_session")
public class BroadcastSession {
	
	@Id
	@GeneratedValue	
	@JsonIgnore
	public long id;
	
	@Column(name="session_id")
	public String sessionId;
	
	@Lob
	public String token;
	
	@Column(name="broadcast_id")
	public String broadcastId;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="creation_datetime")
	public Date creationDateTime;
	
	public long duration;
	
}
