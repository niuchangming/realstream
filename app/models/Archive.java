package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name="archive")
public class Archive {
	
	@Id
	@GeneratedValue	
	@JsonIgnore
	public long id;
	
	@Column(name="archive_id")
	public String archiveId;
	
	public String name;
	public long size;
	public String url;
	
	@Column(name="session_id")
	public String sessionId;
	
	public String status;
	
	public long duration;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="creation_datetime")
	public Date creationDateTime;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "broadcast_session_id")
	public BroadcastSession broadcastSession;
	
	public Archive(){}
	
	public Archive(BroadcastSession broadcastSession){
		this.broadcastSession = broadcastSession;
	} 
}










