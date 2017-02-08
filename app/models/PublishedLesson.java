package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import play.db.jpa.JPA;

@Entity
@Table(name = "publish_lesson")
public class PublishedLesson {
	public final static int PAGE_SIZE = 10;
	
	@Id
	@GeneratedValue
	public long id;
		
	@Column(name="lesson_title")
	public String lessonTitle;
	
	@Column(name="lesson_price")
	public double lessonPrice;
	
	@Column(name="offer_price")
	public double offerPrice;
	
	@Column(name="lesson_id", unique=true) 
	public long lessonId;
	
	@Column(name="category_id", columnDefinition = "double default 0")
	public long categoryId;
	
	@Column(name="lesson_key_name")
	public String lessonKeyName;
	
	@Column(name="lesson_session_count")
	public int lessonSessionCount;
	
	public String institution;
	
	@Column(name="teacher_name")
	public String teacherUsername;
	
	@Column(name="cover_image_uuid")
	public String coverImageUUID;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "published_datetime")
	public Date publishDatetime; 
	
	public PublishedLesson(){}
	
	public PublishedLesson(Lesson lesson){
		
		LessonImage coverImage = JPA.em()
				.createQuery("FROM LessonImage li where li.lesson=:lesson AND li.isCover=:isCover", LessonImage.class)
				.setParameter("lesson", lesson)
				.setParameter("isCover", true).getSingleResult();
		
		this.lessonId = lesson.id;
		this.coverImageUUID = coverImage.thumbnailUUID;
		this.lessonTitle = lesson.title;
		this.lessonPrice = lesson.price;
		this.offerPrice = lesson.offerPrice;
		this.categoryId = lesson.category.id;
		this.lessonKeyName = lesson.lessonKey.getName();
		this.lessonSessionCount = lesson.lessonSessions == null ? 0 : lesson.lessonSessions.size();
		this.teacherUsername = lesson.teacher.username;
		this.institution = lesson.teacher.bestInstitution;
	}
	
}
