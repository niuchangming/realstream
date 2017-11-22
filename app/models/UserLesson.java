package models;

import java.util.Date;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "user_lesson")
public class UserLesson {
	@Id
	@GeneratedValue
	public long id;
	
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id") 
    public User user;
	
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "lesson_id")
    public Lesson lesson;
    
    @Column(name="is_deleted", columnDefinition = "boolean default false")
    public boolean isDeleted;
    
    @Column(name="is_completed", columnDefinition = "boolean default false")
    public boolean isCompleted;
    
    @Column(columnDefinition = "double default 0")
    public double progress;
    
    @Temporal(TemporalType.TIMESTAMP)
	@Column(name="completion_datetime")
    public Date completionDatetime;
    
    public UserLesson(){}
    
    public UserLesson(User user, Lesson lesson){
    	this.user = user;
    	this.lesson = lesson;
    	
    }
    
}












