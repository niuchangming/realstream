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
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "lesson_session")
public class LessonSession {
	@Id
	@GeneratedValue
	public long id;
	
	public String title;
	
	@Lob
	public String brief; 
	
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
	
	@Column(name="is_trial", columnDefinition = "boolean default false")
	public boolean isTrial;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chapter_id")
	public Chapter chapter;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lesson_id")
	public Lesson lesson;
	
	@OneToMany(mappedBy = "lessonSession")
	public List<BroadcastSession> broadcastSessions;
	
	@OneToMany(mappedBy = "lessonSession")
	@OrderBy("uploadedDatetime ASC")
	public List<MediaFile> mediaFiles;
	
	@OneToMany(mappedBy = "lessonSession")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Question> questions;
	
	public LessonSession(){}
	
	public LessonSession(Lesson lesson){
		this.lesson = lesson;
		this.creationDateTime = new Date();
	}
	
}












