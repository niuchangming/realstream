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
@Table(name = "offer")
public class Offer {
	
	@Id
	@GeneratedValue
	@JsonIgnore
	public long id;
	
	@Column(name="offer_price", columnDefinition = "double default 0")
	public double offerPrice;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "offer_startdate")
	public Date offerStartDate;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "offer_enddate")
	public Date offerEndDate;
	
	public String reason;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lesson_id")
	public Lesson lesson;
	
	public Offer(){}
	
	public Offer(Lesson lesson){
		this.lesson = lesson;
	}
	
}


















