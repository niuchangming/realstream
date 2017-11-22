package controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import actions.AuthAction;
import models.Answer;
import models.AnswerImage;
import models.ChoiceImage;
import models.Lesson;
import models.LessonImage;
import models.LessonSession;
import models.QuestionImage;
import models.Question;
import models.QuestionType;
import models.ResponseData;
import play.Application;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import tools.Constants;
import tools.Utils;
import views.html.errorpage;
import views.html.lessonquestion;

@SuppressWarnings("unchecked")
public class QuestionController extends Controller{
	@Inject private FormFactory formFactory;
	@Inject private JPAApi jpaApi;
	@Inject private Provider<Application> application;
	
	@With(AuthAction.class)
	@Transactional
	public Result lessonQuestions(long lessonId, int offset){
		ResponseData responseData = new ResponseData();
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
		
		if(lesson != null){
			int totalAmount = lesson.questions.size();
			int pageIndex = (int) Math.ceil(offset / Constants.LESSON_SESSION_PAGE_SIZE) + 1;
			
			List<Question> questions = jpaApi.em()
					.createQuery("FROM Question q WHERE q.lesson = :lesson order by q.createDatetime ASC", Question.class)
					.setParameter("lesson", lesson)
					.setFirstResult(offset)
					.setMaxResults(Constants.QUESTION_PAGE_SIZE)
					.getResultList();
			
			return ok(lessonquestion.render(lesson, questions, pageIndex, totalAmount));
		}else{
			responseData.message = "The lesson cannot be found.";
    		responseData.code = 4000;
			return notFound(errorpage.render(responseData));
		}
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result createQuestion(){
		ResponseData responseData = new ResponseData();	
		
	    DynamicForm requestData = formFactory.form().bindFromRequest();
    	long lessonId = Long.parseLong(requestData.get("lessonId"));
    	long lessonSessionId = Long.parseLong(requestData.get("lessonSessionId"));
	    String content = requestData.get("content");
	    String videoUrl = requestData.get("videoUrl");
	    int quesTypeInt = Integer.parseInt(requestData.get("quesType"));
	    Map<String, String> data = requestData.data();
	    
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
		if(lesson != null){
			try {
				LessonSession lessonSession = null;
				if(lessonSessionId > 0){
					lessonSession = jpaApi.em().find(LessonSession.class, lessonSessionId);
				}
				
				Question question = new Question(lesson, lessonSession);
				question.content = content;
				question.videoUrl = videoUrl;
				question.questionType = QuestionType.getIndex(quesTypeInt);
				jpaApi.em().persist(question);
				
				String anwserJsonStr = question.initChoices(data);
				if(!Utils.isBlank(anwserJsonStr)){
					Answer answer = new Answer(question);
					answer.choiceJson = anwserJsonStr;
					jpaApi.em().persist(answer);
				}
				
				MultipartFormData<File> body = request().body().asMultipartFormData();
			    List<FilePart<File>> imageParts = body.getFiles();
			    if(imageParts != null){
			    	for(FilePart<File> fp : imageParts){
			    		String contentType = fp.getContentType();
			    		if(contentType.equals("image/png") || contentType.equals("image/jpg") || contentType.equals("image/jpeg")){
			    			if(fp.getKey().equals("choiceImage[]")){
				    			if(question.choiceImages != null){
									for(ChoiceImage img : question.choiceImages){
										img.delete();
										jpaApi.em().remove(img);
									}
								}
				    			ChoiceImage choiceImage = new ChoiceImage(question, fp.getFile());
				    			choiceImage.name = fp.getFilename();
								jpaApi.em().persist(choiceImage);
				    		}else{
								if(question.questionImages != null){
									for(QuestionImage img : question.questionImages){
										img.delete();
										jpaApi.em().remove(img);
									}
								}
								QuestionImage qImage = new QuestionImage(question, fp.getFile());
								qImage.name = fp.getFilename();
								jpaApi.em().persist(qImage);
				    		}
			    		}else{
			    			responseData.message = "Image format supports PNG/JPG only.";
					    	responseData.code = 4000;
			    		}
				    }
			    }
				flash("success", "Successfully added question.");
			} catch (ParseException | IOException e) {
				responseData.message = e.getLocalizedMessage();
				responseData.code = 4001;
			} 
		}else{
			responseData.message = "The lesson cannot be found.";
			responseData.code = 4000;
		}
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result editQuestion(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
    	long questionId = Long.parseLong(requestData.get("questionId"));
    	long lessonSessionId = Long.parseLong(requestData.get("lessonSessionId"));
	    String content = requestData.get("content");
	    String videoUrl = requestData.get("videoUrl");
	    int quesTypeInt = Integer.parseInt(requestData.get("quesType"));
	    Map<String, String> data = requestData.data();
	    
    	Question question = jpaApi.em().find(Question.class, questionId);
    	if(question != null){
    		try {
    			LessonSession lessonSession = null;
				if(lessonSessionId > 0){
					lessonSession = jpaApi.em().find(LessonSession.class, lessonSessionId);
					question.lessonSession = lessonSession;
				}else{
					question.lessonSession = null;
				}
    			question.content = content;
        		question.videoUrl = videoUrl;
        		question.questionType = QuestionType.getIndex(quesTypeInt);
        		jpaApi.em().persist(question);
        		
        		MultipartFormData<File> body = request().body().asMultipartFormData();
			    List<FilePart<File>> imageParts = body.getFiles();
			    
			    String anwserJsonStr = question.initChoices(data);
				if(!Utils.isBlank(anwserJsonStr)){
					if(question.answers.size() == 1){
						question.answers.get(0).choiceJson = anwserJsonStr;
						jpaApi.em().persist(question.answers.get(0));
					}
				}
				
			    if(imageParts != null){
			    	for(FilePart<File> fp : imageParts){
			    		String contentType = fp.getContentType();
			    		if(contentType.equals("image/png") || contentType.equals("image/jpg") || contentType.equals("image/jpeg")){
			    			if(fp.getKey().equals("choiceImage[]")){
			    				TypeReference<List<HashMap<String, Object>>> typeRef = new TypeReference<List<HashMap<String, Object>>>() {};
			    				List<HashMap<String, Object>> mapList = new ObjectMapper().readValue(anwserJsonStr, typeRef);
			    				
			    				if(question.choiceImages != null){
			    					for(ChoiceImage img : question.choiceImages){
				    					boolean exist = false;
				    					for(Map<String, Object> map : mapList){
					    					if(img.name.equals(map.get("content"))){
					    						exist = true;
					    						continue;
					    					}
					    				}
				    					if(!exist){
				    						img.delete();
				    						jpaApi.em().remove(img);
				    					}
									}
			    				}
			    				
				    			ChoiceImage choiceImage = new ChoiceImage(question, fp.getFile());
				    			choiceImage.name = fp.getFilename();
								jpaApi.em().persist(choiceImage);
				    		}else if(fp.getKey().equals("image")){
								if(question.questionImages != null){
									for(QuestionImage img : question.questionImages){
										img.delete();
										jpaApi.em().remove(img);
									}
								}
								
								QuestionImage qaImage = new QuestionImage(question, fp.getFile());
								qaImage.name = fp.getFilename();
								jpaApi.em().persist(qaImage);
				    		}
			    		}
				    }
			    }
				flash("success", "Successfully updated question.");
    		} catch (ParseException | IOException e) {
				responseData.message = e.getLocalizedMessage();
				responseData.code = 4001;
			} 
    	}else{
    		responseData.code = 4000;
    		responseData.message = "Question doesn't exist.";
    	}
    	return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result removeQuestion(){
		ResponseData responseData = new ResponseData();
		
	    DynamicForm requestData = formFactory.form().bindFromRequest();
    	long questionId = Long.parseLong(requestData.get("questionId"));
    	
    	Question question = jpaApi.em().find(Question.class, questionId);
    	if(question != null){
    		//remove all images related to this question
			List<QuestionImage> questionImages = jpaApi.em()
    				.createNativeQuery("SELECT * FROM question_image qm WHERE qm.question_id = :questionId", QuestionImage.class)
    				.setParameter("questionId", questionId)
    				.getResultList();
			
    		for(QuestionImage img : questionImages){
    			img.delete();
        		jpaApi.em().remove(img);
    		}
    		
    		//remove all choices images related to this question
    		List<ChoiceImage> choiceImages = jpaApi.em()
    				.createNativeQuery("SELECT * FROM choice_image cm WHERE cm.question_id = :questionId", ChoiceImage.class)
    				.setParameter("questionId", questionId)
    				.getResultList();
			
    		for(ChoiceImage img : choiceImages){
    			img.delete();
        		jpaApi.em().remove(img);
    		}
    		
    		//remove all answers related to this question
    		for(Answer answer : question.answers){
    			List<AnswerImage> answerImages = jpaApi.em()
        				.createNativeQuery("SELECT * FROM answer_image am WHERE am.answer_id = :answerId", AnswerImage.class)
        				.setParameter("answerId", answer.id)
        				.getResultList();
        		for(AnswerImage answerImage : answerImages){
        			answerImage.delete();
            		jpaApi.em().remove(answerImage);
        		}
        		jpaApi.em().remove(answer);
    		}
    		jpaApi.em().remove(question);
    	}else{
    		responseData.code = 4000;
    		responseData.message = "Question doesn't exist.";
    	}
    	return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result showQuestionImage(String uuid){
		TypedQuery<QuestionImage> query = jpaApi.em()
				.createQuery("from QuestionImage qi where qi.uuid = :uuid", QuestionImage.class)
				.setParameter("uuid", uuid);
		
		InputStream imageStream = null;
		try{
			QuestionImage lessonImage = query.getSingleResult();
			imageStream = lessonImage.download();
		}catch(NoResultException e){
			imageStream = application.get().classloader().getResourceAsStream(LessonImage.PLACEHOLDER);
		}
		return ok(imageStream);
	}
	
	@Transactional
	public Result showChoiceImage(String uuid){
		TypedQuery<ChoiceImage> query = jpaApi.em()
				.createQuery("from ChoiceImage ci where ci.uuid = :uuid", ChoiceImage.class)
				.setParameter("uuid", uuid);
		
		InputStream imageStream = null;
		try{
			ChoiceImage choiceImage = query.getSingleResult();
			imageStream = choiceImage.download();
		}catch(NoResultException e){
			imageStream = application.get().classloader().getResourceAsStream(ChoiceImage.PLACEHOLDER);
		}
		return ok(imageStream);
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result addExplanation(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
    	long answerId = Long.parseLong(requestData.get("answerId"));
    	String content = requestData.get("content");
    	String explain = requestData.get("explain");
    	
    	Answer answer = jpaApi.em().find(Answer.class, answerId);
    	if(answer != null){
    		answer.explanation = explain;
    		answer.content = content;
    		jpaApi.em().persist(answer);
    		
    		MultipartFormData<File> body = request().body().asMultipartFormData();
			FilePart<File> imageFile = body.getFile("anwserImage");
			if(imageFile != null){
				String contentType = imageFile.getContentType();
				if(contentType.equals("image/png") || contentType.equals("image/jpg") || contentType.equals("image/jpeg")){
					List<AnswerImage> answerImages = jpaApi.em()
	        				.createNativeQuery("SELECT * FROM answer_image am WHERE am.answer_id = :answerId", AnswerImage.class)
	        				.setParameter("answerId", answer.id)
	        				.getResultList();
					
					for(AnswerImage answerImage : answerImages){
	        			answerImage.delete();
	            		jpaApi.em().remove(answerImage);
	        		}
					
					AnswerImage answerImage = new AnswerImage(answer, imageFile.getFile());
					answerImage.name = imageFile.getFilename();
					jpaApi.em().persist(answerImage);
					
					flash("success", "Successfully added Explanation.");
				}else{
					responseData.message = "Image format supports PNG/JPG only.";
			    	responseData.code = 4000;
				}
			}
    	}else{
    		responseData.code = 4000;
    		responseData.message = "Answer doesn't exist.";
    	}
    	
    	return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result getAnswer(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
    	long answerId = Long.parseLong(requestData.get("answerId"));

    	Answer answer = jpaApi.em().find(Answer.class, answerId);
    	if(answer != null){
			try {
				responseData.data = answer;
				ObjectMapper mapper = new ObjectMapper();
				JsonNode jsonData = mapper.readTree(Utils.toJson(ResponseData.class, responseData, "*.question", "*.answer"));
				return ok(Json.toJson(jsonData));
			} catch (IOException e) {
				responseData.code = 4001;
	    		responseData.message = e.getLocalizedMessage();
			}
    	}else{
    		responseData.code = 4000;
    		responseData.message = "Anwser cannot be found.";
    	}
    	
    	return ok(Json.toJson(responseData));
	}
}































