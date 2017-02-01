package models;

import java.io.File;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import org.hibernate.annotations.Parameter;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;

import play.db.jpa.JPA;
import services.S3Plugin;
import tools.Utils;

import org.hibernate.annotations.GenericGenerator;

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
	
	public int point;
	
	public int credit;
	
	public Role role;
	
	@Column(name="real_name")
	public String realName;
	
	@Column(name="ic_uuid")
	public String icUUID;
	
	@Column(name="certificate_uuid")
	public String certificateUUID;
	
	@Column(name="best_institution")
	public String bestInstitution;
	
	@Lob
	public String brief;
	
	@OneToMany(mappedBy = "user")
	public List<WorkExperience>  workExperiences;
	
	@OneToMany(mappedBy = "teacher")
	public List<Lesson> teacherLessons;
	
	@OneToMany(mappedBy = "user")
	public List<UserLesson> userLessons;
	
	public User(){}
	
	public User(Account account){
		this.account = account;
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
	    
	    for(int i = 0; i < size; i++){
	    	WorkExperience workExperience = new WorkExperience(this);
	    	workExperience.brief = workExperienceMap.get(i);
	    	workExperience.fromDate = Utils.parse(fromDateMap.get(i) + " 00:00");
	    	workExperience.toDate = Utils.parse(toDateMap.get(i) + " 00:00");
	    	
	    	JPA.em().persist(workExperience);
	    }
	}
	
	public void uploadIcImage(File file){
		this.icUUID = Utils.uuid();
		if (S3Plugin.amazonS3 == null) {
            throw new RuntimeException("S3 Could not save");
        }else {
        	this.icUUID = Utils.uuid();
            PutObjectRequest putObjectRequest = new PutObjectRequest(S3Plugin.s3Bucket, this.icUUID, file);
            putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead);
            S3Plugin.amazonS3.putObject(putObjectRequest); 
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
