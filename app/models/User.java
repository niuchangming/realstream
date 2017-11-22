package models;

import java.io.File;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.JoinColumn;
import org.hibernate.annotations.Parameter;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;

import play.db.jpa.JPA;
import services.S3Plugin;
import tools.Utils;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name="user")
public class User{
	@Id
	@GenericGenerator(name = "generator", strategy = "foreign", parameters = @Parameter(name = "property", value = "account"))
	@GeneratedValue(generator = "generator")
	@Column(name = "account_id", unique = true, nullable = false)
	public long accountId;
	
	@OneToOne(fetch = FetchType.LAZY)
	@PrimaryKeyJoinColumn
	public Account account;
	
	@OneToMany(mappedBy = "user")
	public List<Avatar> avatars;
	
	@Column(name="user_name")
	public String username;
	
	public String email;
	
	public String wechat;
	
	public String qq;
	
	public int credit; // 学分，之后可用于消费打折
	
	public int coin;
	
	public Role role;
	
	@Column(name="real_name")
	public String realName;
	
	@Column(name="ic")
	public String ic;
	
	@Column(name="certificate_uuid")
	public String certificateUUID;
	
	@Column(name="best_institution")
	public String bestInstitution;
	
	@Lob
	public String brief;
	
	@OneToMany(mappedBy = "user")
	public List<WorkExperience> workExperiences;
	
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "favorite_lesson", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "lesson_id"))
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Lesson> favoriteLessons;
	
	@OneToMany(mappedBy = "teacher")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Lesson> teacherLessons;
	
	@OneToMany(mappedBy = "user")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<UserLesson> userLessons;

	@OneToMany(mappedBy = "user")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Payment> payments;
	
	@OneToMany(mappedBy = "user")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<Comment> comments;
	
	@OneToMany(mappedBy = "user")
	@LazyCollection(LazyCollectionOption.EXTRA)
	public List<ChatMessage> chatMessages;
	
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "event_id") 
    public Event event;
	
	public User(){}
	
	public User(Account account){
		this.account = account;
		this.credit = 0;
		this.coin = 0;
		this.role = Role.STUDENT;
	}
	
	public void initWorkExperiences(Map<String, String> data) throws ParseException{
		Iterator<String> iterator = data.keySet().iterator();
		Map<Integer, String> fromDateMap = new HashMap<>();
	    Map<Integer, String> toDateMap = new HashMap<>();
	    Map<Integer, String> workExperienceMap = new HashMap<>();
	    
	    String key;
	    while(iterator.hasNext()){
	    	key = iterator.next();
	    	if(key.contains("fromDate")){
	    		int startIdx = key.indexOf("[") + 1;
	    		int endIdx = key.indexOf("]");
	    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
	    		fromDateMap.put(pos, data.get(key));
	    	}
	    	
	    	if(key.contains("toDate")){
	    		int startIdx = key.indexOf("[") + 1;
	    		int endIdx = key.indexOf("]");
	    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
	    		toDateMap.put(pos, data.get(key));
	    	}
	    	
	    	if(key.contains("workExperience")){
	    		int startIdx = key.indexOf("[") + 1;
	    		int endIdx = key.indexOf("]");
	    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
	    		workExperienceMap.put(pos, data.get(key));
	    	}
	    }
	    
	    int size =  Math.max(Math.max(fromDateMap.size(), toDateMap.size()), workExperienceMap.size());
	    
	    //if update teacher info, we have to delete all work experience first and then add the new uploaded data.
	    if(this.workExperiences != null && this.workExperiences.size() > 0){
	    	for(WorkExperience workExp : this.workExperiences){
	    		JPA.em().remove(workExp);
	    	}
	    }
	    
	    for(int i = 0; i < size; i++){
	    	WorkExperience workExperience = new WorkExperience(this);
	    	workExperience.brief = workExperienceMap.get(i);
	    	workExperience.fromDate = Utils.parse(fromDateMap.get(i) + " 00:00");
	    	workExperience.toDate = Utils.parse(toDateMap.get(i) + " 00:00");
	    	
	    	JPA.em().persist(workExperience);
	    }
	}
	
	public void uploadCertImage(File file){
		if (S3Plugin.amazonS3 == null) {
            throw new RuntimeException("S3 Could not save");
        }else {
        	this.certificateUUID = Utils.uuid();
            PutObjectRequest putObjectRequest = new PutObjectRequest(S3Plugin.s3Bucket, this.certificateUUID, file);
            putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead);
            S3Plugin.amazonS3.putObject(putObjectRequest); 
        }
	}
	
}
