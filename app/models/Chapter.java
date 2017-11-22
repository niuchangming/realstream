package models;

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

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "chapter")
public class Chapter {
	@Id
	@GeneratedValue
	public long id;
	
	@Column(name="chapter_index")
	public int chapterIndex;
	
	public String title;
	
	@Lob
	public String brief;
	
	@OneToMany(mappedBy = "chapter")
	@OrderBy("startDatetime ASC")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<LessonSession> lessonSessions;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lesson_id")
	public Lesson lesson;
	
	public Chapter(){}
	
	public Chapter(Lesson lesson){
		this.lesson = lesson;
	}
}
