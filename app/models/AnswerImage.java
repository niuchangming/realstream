package models;

import java.io.File;
import java.io.InputStream;
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
import javax.persistence.Transient;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.annotation.JsonIgnore;

import services.S3Plugin;
import tools.Utils;

@Entity
@Table(name="answer_image")
public class AnswerImage {
	@Transient
	@JsonIgnore
	public static final String PLACEHOLDER = "public/images/image_placeholder.png";
	
	@Id
	@GeneratedValue
	@JsonIgnore
	public long id;
	
	@Column(name="uuid")
	public String uuid;
	
	public String name;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="uploaded_datetime", nullable = false)
	public Date uploadedDateTime;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "answer_id")
	public Answer answer;
	
	public AnswerImage(){}
	public AnswerImage(Answer answer, File file){
		this.uuid = Utils.uuid();
		this.uploadedDateTime = new Date();
		this.answer = answer;
		upload(file);
	}
	
	public void upload(File file){
		if (S3Plugin.amazonS3 == null) {
            throw new RuntimeException("S3 Could not save");
        }else {
            PutObjectRequest putObjectRequest = new PutObjectRequest(S3Plugin.s3Bucket, this.uuid, file);
            putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead);
            S3Plugin.amazonS3.putObject(putObjectRequest); 
        }
	}
	
	public InputStream download(){
		S3Object s3Object = S3Plugin.amazonS3.getObject(new GetObjectRequest(S3Plugin.s3Bucket, this.uuid));
		InputStream stream = s3Object.getObjectContent();
		return stream;
	}

	public void delete(){
		S3Plugin.amazonS3.deleteObject(S3Plugin.s3Bucket, this.uuid);
	}

}
