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

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name="comment")
public class Comment {
	@Id
	@GeneratedValue	
	@JsonIgnore
	public long id;
	
	public String comment;
	
	public int point;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "create_datetime")
	public Date createDatetime;
	
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id") 
    public User user;
	
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "lesson_id") 
    public Lesson lesson;
	
	public Comment(){}
	
	public Comment(User user, Lesson lesson){
		this.user = user;
		this.lesson = lesson;
		this.createDatetime = new Date();
	}
}
















