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

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import services.S3Plugin;
import tools.Utils;

@Entity
@Table(name = "media_file")
public class MediaFile {
	public final static int PAGE_SIZE = 10;
	
	@Id
	@GeneratedValue
	public long id;
	
	public String name;
	
	public long size;
	
	@Column(name="file_type")
	public String fileType;
	
	public String uuid;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="creation_datetime")
	public Date uploadedDatetime;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    public Lesson lesson;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_session_id")
    public LessonSession lessonSession;
	
	public MediaFile(){}
	
	public MediaFile(Lesson lesson, File file){
		this.uuid = Utils.uuid();
		this.uploadedDatetime = new Date();
		this.lesson = lesson;
		this.size = file.length();
		
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
