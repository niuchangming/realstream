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
@Table(name = "lesson_session")
public class LessonSession {
	public final static int PAGE_SIZE = 10;
	
	@Id
	@GeneratedValue
	@JsonIgnore
	public long id;
	
	public String title;
	
	@Lob
	public String brief; 
	
	@Column(name="interactive")
	public boolean interactive;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "creation_datetime")
	public Date creationDateTime;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "start_datetime")
	public Date startDatetime;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "end_datetime")
	public Date endDatetime;
	
	public int duration;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lesson_id")
	public Lesson lesson;
	
	public LessonSession(){}
	
	public LessonSession(Lesson lesson){
		this.lesson = lesson;
	}
	
}












