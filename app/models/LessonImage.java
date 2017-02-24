package models;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.annotation.JsonIgnore;

import net.coobird.thumbnailator.Thumbnails;
import services.S3Plugin;
import tools.Utils;

@Entity
@DiscriminatorValue("lesson")
public class LessonImage extends Image{
	@Transient
	@JsonIgnore
	public static final String PLACEHOLDER = "public/images/image_placeholder.png";
	
	@Transient
	@JsonIgnore
	public static final int IMAGE_MAX = 15;
	
	public String title;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lesson_id")
	public Lesson lesson;
	
	@Column(name="thumbnail_uuid")
	public String thumbnailUUID;
	
	@Column(name="is_cover", columnDefinition = "boolean default false")
	public boolean isCover;

	@Lob
	public String caption; 
	
	public LessonImage(){}
	
	public LessonImage(Lesson lesson, File file) throws IOException{
		super(file);
		this.lesson = lesson;
		this.isCover = false;
		this.thumbnailUUID = Utils.uuid();
		uploadThumbnail(file);
	}
	
	public void uploadThumbnail(File file) throws IOException{
		File thumbnailFile = generateThumbnail(file);
		if (S3Plugin.amazonS3 == null) {
            throw new RuntimeException("S3 Could not save");
        }else {
            PutObjectRequest putObjectRequest = new PutObjectRequest(S3Plugin.s3Bucket, this.thumbnailUUID, thumbnailFile);
            putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead);
            S3Plugin.amazonS3.putObject(putObjectRequest); 
        }
		thumbnailFile.delete();
	}
	
	public InputStream downloadThumbnail(){
		S3Object s3Object = S3Plugin.amazonS3.getObject(new GetObjectRequest(S3Plugin.s3Bucket, this.thumbnailUUID));
		InputStream stream = s3Object.getObjectContent();
		return stream;
	}
	
	public void deleteThumbnail(){
		S3Plugin.amazonS3.deleteObject(S3Plugin.s3Bucket, this.thumbnailUUID);
	}
	
	public File generateThumbnail(File file) throws IOException {
		File thumbnailFile = new File(Utils.uuid() + ".jpg");
		if(!thumbnailFile.exists()){
			thumbnailFile.createNewFile();
		}
		
		Thumbnails.of(file)
	    .size(300, 300)
	    .outputFormat("jpg")
	    .toFile(thumbnailFile);
		
		return thumbnailFile;
	}
	
}
