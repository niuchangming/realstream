package models;

import java.io.File;
import java.io.InputStream;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.annotation.JsonIgnore;

import services.S3Plugin;
import tools.Utils;

@Entity
@Table(name="image")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="type", discriminatorType = DiscriminatorType.STRING)
public class Image {
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
	
	public Image(){}
	
	public Image(File file){
		this.uuid = Utils.uuid();
		this.uploadedDateTime = new Date();
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










