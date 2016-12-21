package models;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name="work_experience")
public class WorkExperience{
	@Id
	@GeneratedValue	
	@JsonIgnore
	public long id;
	
	@Column(name="from_date")
	public Date fromDate;
	
	@Column(name="to_date")
	public Date toDate;
	
	public String brief;
	
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	public User user;
	
	public WorkExperience(){}
	
	public WorkExperience(User user){
		this.user = user;
	}
	
}
