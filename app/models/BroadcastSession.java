package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name="broadcast_session")
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
	
	public String hls;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="creation_datetime")
	public Date creationDateTime;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lesson_session_id")
	public LessonSession lessonSession;
	
	@OneToMany(mappedBy = "broadcastSession")
	public List<Archive> archives;
	
	public BroadcastSession(){}
	
	public BroadcastSession(LessonSession lessonSession){
		this.lessonSession = lessonSession;
		this.creationDateTime = new Date();
	}
	
}
