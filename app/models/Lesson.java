package models;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "lesson")
public class Lesson {
	
	@Transient
	public final static int PAGE_SIZE = 5;
	
	@Id
	@GeneratedValue
	@JsonIgnore
	public long id;

	public String title;

	@Lob
	public String description;
	
	@Column(columnDefinition = "double default 0")
	public double price;
	
	public double offerPrice;
	
	@Column(name="lesson_key", columnDefinition = "double default 0")
	public LessonKey lessonKey;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "offer_startdate")
	public Date offerStartDate;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "offer_enddate")
	public Date offerEndDate;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "start_datetime")
	public Date startDatetime;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "end_datetime")
	public Date endDatetime;

	@Column(name = "is_publish")
	public boolean isPublish;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	public Category category;

	@OneToMany(mappedBy = "lesson")
	public List<LessonImage> lessonImages;
	
	@OneToMany(mappedBy = "lesson")
	@OrderBy("startDatetime ASC")
	public List<LessonSession> lessonSessions;
	
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "teacher_id")
	public User teacher; 

	@OneToMany(mappedBy = "lesson")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<UserLesson> userLessons;
	
	@ManyToMany(mappedBy = "favoriteLessons")
	public List<User> users;
	 
	@OneToMany(mappedBy = "lesson")
	public List<MediaFile> mediaFiles;
	
	@OneToMany(mappedBy = "lesson")
	public List<Comment> comments;
	
	public Lesson(){}

	public Lesson(User teacher, String title) {
		this.title = title;
		this.teacher = teacher;
		this.lessonKey = LessonKey.NEW;
	}
	
	public void updateByBasic(String title, String description,  Category category){
		this.title = title;
		this.description = description;
		this.category = category;
	}
	
	public static List<Lesson> sortByUserAmount(List<Lesson> lessons){
		Collections.sort(lessons, new Comparator<Lesson>(){
			public int compare(Lesson o1, Lesson o2){
			     return Integer.compare(o2.userLessons.size(), o1.userLessons.size());
			}
		});
		return lessons;
	}

}
