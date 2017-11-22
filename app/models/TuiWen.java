package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "tuiwen")
public class TuiWen {
	@Id
	@GeneratedValue	
	public long id;
	
	public String title;
	
	@Lob
	public String content;
	
	public TuiWenType type; 
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="uploaded_datetime", nullable = false)
	public Date uploadedDateTime;

	public TuiWen(){}
	
	public TuiWen(String title, String content, TuiWenType type){
		this.type = type;
		this.content = content;
		this.title = title;
		this.uploadedDateTime = new Date();
	}
}
