package models;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "event")
public class Event {
	@Id
	@GeneratedValue
	public long id;
	
	public String title;
	
	@Lob
	public String description;
	
	@Column(name="is_expired")
	public boolean isExpired;
	
	@OneToMany(mappedBy = "event")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<EventImage> webImages;
	
	@OneToMany(mappedBy = "event")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<EventImage> appImages;
	
	public String link;
	
	@OneToMany(mappedBy = "event")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<User> teachers;
	
	@OneToMany(mappedBy = "event")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Lesson> lessons;
	
	public Event(){}
	
}
