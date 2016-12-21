package services;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;import com.amazonaws.services.s3.model.Region;

import play.Application;

@Singleton
public class S3Plugin {
	
	public static final String AWS_S3_BUCKET = "aws.s3.bucket";
    public static final String AWS_ACCESS_KEY = "aws.access.key";
    public static final String AWS_SECRET_KEY = "aws.secret.key";
    private final Application application;
    public static AmazonS3 amazonS3;
    public static String s3Bucket;
    
    @Inject
    public S3Plugin(Application application){
    	this.application = application;
    	initS3Config();
    }
    
    public void initS3Config(){
		String accessKey = application.configuration().getString(AWS_ACCESS_KEY);
        String secretKey = application.configuration().getString(AWS_SECRET_KEY);
        s3Bucket = application.configuration().getString(AWS_S3_BUCKET);

        if ((accessKey != null) && (secretKey != null)) {
            AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
            amazonS3 = new AmazonS3Client(awsCredentials);
            amazonS3.setRegion(com.amazonaws.regions.Region.getRegion(Regions.AP_SOUTHEAST_1));
            
            if(!amazonS3.doesBucketExist(s3Bucket)){
            	amazonS3.createBucket(s3Bucket);
            }
        }
	}

    public boolean enabled() {
        return (application.configuration().keys().contains(AWS_ACCESS_KEY) &&
                application.configuration().keys().contains(AWS_SECRET_KEY) &&
                application.configuration().keys().contains(AWS_S3_BUCKET));
    }
}
