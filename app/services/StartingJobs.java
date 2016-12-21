package services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import models.Category;
import play.Application;
import play.db.jpa.JPAApi;

@Singleton
public class StartingJobs {
	
	
	@Inject
	public StartingJobs(JPAApi jpaApi) {
		jpaApi.withTransaction(() -> {
			Query query = jpaApi.em().createQuery("select count(*) from Category");
			
			Long count;
			try{
				count = (Long)query.getSingleResult();
			}catch(NoResultException e){
				count = 0L;
			}
			
			if(count == 0){
				List<Category> categories = Category.initCategoryByJson();
				
				for(Category category : categories){
			    	if(category.children != null){
			    		for(Category childCate : category.children){
			    			childCate.parent = category;
			    			jpaApi.em().persist(childCate);
			    		}
			    	}
			    	jpaApi.em().persist(category);
			    }
			}
		   
		});
	}

	protected String readFile(File file){
		int read, size = 1024 * 1024;
		char[] buffer = new char[size];
		String text = "";
		
		try {
		    FileReader fr = new FileReader(file);
		    BufferedReader br = new BufferedReader(fr);

		    while(true) {
		        read = br.read(buffer, 0, size);
		        text += new String(buffer, 0, read);

		        if(read < size) {
		            break;
		        }
		    }
		    
		    if(br != null){
		    	br.close();
		    }
		} catch(Exception ex) {
		    ex.printStackTrace();
		}
		
		return text;
	}
	
}