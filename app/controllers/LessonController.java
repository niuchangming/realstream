package controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.math.BigInteger;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import models.Account;
import models.Advisory;
import models.Category;
import models.Chapter;
import models.Lesson;
import models.LessonImage;
import models.LessonSession;
import models.Offer;
import models.Payment;
import models.ResponseData;
import models.Subject;
import models.User;
import models.UserLesson;
import play.Application;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.mvc.With;
import tools.Constants;
import tools.Utils;
import views.html.createlesson;
import views.html.errorpage;
import views.html.lessonimage;
import views.html.lessonsession;
import views.html.lessondetail;
import views.html.lessonprice;
import views.html.lessons;
import views.html.lessonstudents;
import actions.AuthAction;
import modelVOs.StudentVO;

@SuppressWarnings("unchecked")
public class LessonController extends Controller{
	@Inject private FormFactory formFactory;
	@Inject private JPAApi jpaApi;
	@Inject private Provider<Application> application;
	
	@With(AuthAction.class)
	@Transactional
	public Result createLesson(){
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String lessonTitle = requestData.get("title");
		
		if(Utils.isBlank(lessonTitle)){
			flash("error", "Lesson title cannot be empty.");
			return redirect(routes.HomeController.index());
		}
		
		Account account = (Account) ctx().args.get("account");
		
		TypedQuery<User> userQuery = jpaApi.em()
				.createQuery("from User u where u.accountId = :accountId", User.class)
				.setParameter("accountId", account.id);
		
		User teacher = userQuery.getSingleResult();
		
		Lesson lesson = new Lesson(teacher, lessonTitle);
		jpaApi.em().persist(lesson);
		
		Query query = jpaApi.em().createQuery("from Category cg where cg.parent = null", Category.class);
		List<Category> categories = query.getResultList();
		
		return ok(createlesson.render(lesson, categories));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result editLesson(long lessonId){
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
		
		if(lesson == null){
			flash("error", "You do not have any lesson.");
			return redirect(routes.HomeController.index());
		}
		
		Query query = jpaApi.em().createQuery("from Category cg where cg.parent = null", Category.class);
		List<Category> categories = query.getResultList();
		
		return ok(createlesson.render(lesson, categories));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result saveLessonBasicInfo(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		long lessonId = Long.parseLong(requestData.get("id"));
		String lessonTitle = requestData.get("title");
		String lessonDesc = requestData.get("description");
		long categoryId = Long.parseLong(requestData.get("categoryId"));
		String subject = requestData.get("subject");
		int maxCount = Integer.parseInt(requestData.get("maxCount"));
		String interactiveStr = requestData.get("interactive");
		boolean interactive = false;
		
		Category category = jpaApi.em().find(Category.class, categoryId);
		Lesson dbLesson = jpaApi.em().find(Lesson.class, lessonId);
		
		if(category == null || dbLesson == null){
			responseData.message = "The lesson cannot be found or category is empty.";
			responseData.code = 4000;
		}else{
			if(!Utils.isBlank(interactiveStr)){
				interactive = Integer.parseInt(interactiveStr) == 1;
			}
			dbLesson.updateByBasic(lessonTitle, lessonDesc, category, Subject.valueOf(subject), interactive, maxCount);
			jpaApi.em().persist(dbLesson);
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result editLessonImages(long lessonId){
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
		
		if(lesson == null){
			flash().put("error", "The lesson cannot be found.");
			return redirect(routes.HomeController.index());
		}
		
		return ok(lessonimage.render(lesson));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result uploadLessonImage(){
		ResponseData responseData = new ResponseData();
		
		MultipartFormData<File> body = request().body().asMultipartFormData();
	    FilePart<File> imageFile = body.getFile("lessonImage");
	    
	    try {
		    if(imageFile != null){
		    	DynamicForm requestData = formFactory.form().bindFromRequest();
		    	long lessonId = Long.parseLong(requestData.get("lessonId"));
		    	
				Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
				
				if(lesson == null){
					responseData.message = "The lesson cannot be found.";
					responseData.code = 4000;
				}else{
					int imageCount = lesson.lessonImages.size();
					if(imageCount >= LessonImage.IMAGE_MAX){
						responseData.message = "Lesson image count exceeds 15.";
						responseData.code = 4000;
					}else{
						LessonImage lessonImage = new LessonImage(lesson, imageFile.getFile());
						lessonImage.name = imageFile.getFilename();
						if(imageCount == 0){
							lessonImage.isCover = true;
						}
						
						jpaApi.em().persist(lessonImage);
						responseData.data = lessonImage;
						return ok(Utils.toJson(ResponseData.class, responseData, "*.lesson"));
					}
				}
		    }else{
		    	responseData.message = "Image file cannot be empty.";
		    	responseData.code = 4000;
		    }
	    } catch (IOException e) {
	    	responseData.message = e.getLocalizedMessage();
	    	responseData.code = 4001;
		}		
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result showLessonImage(String uuid, boolean isLarge){
		TypedQuery<LessonImage> query = jpaApi.em()
				.createQuery("from LessonImage li where li.uuid = :uuid", LessonImage.class)
				.setParameter("uuid", uuid);
		
		InputStream imageStream = null;
		try{
			LessonImage lessonImage = query.getSingleResult();
			if(isLarge){
				imageStream = lessonImage.download();
			}else{
				imageStream = lessonImage.downloadThumbnail();
			}
		}catch(NoResultException e){
			imageStream = application.get().classloader().getResourceAsStream(LessonImage.PLACEHOLDER);
		}
		return ok(imageStream);
	}
	
	@Transactional
	public Result showLessonCover(long lessonId, boolean isLarge){
		Query coverQuery = jpaApi.em()
				.createNativeQuery("select * from image where (lesson_id=:lessonId and is_cover=:isCover) or lesson_id=:lessonId limit 1", LessonImage.class)
				.setParameter("lessonId", lessonId)
				.setParameter("isCover", true);
		
		LessonImage lessonImage = null;
		InputStream imageStream = null;
		try{
			lessonImage = (LessonImage) coverQuery.getSingleResult();
		}catch(NoResultException e){
			imageStream = application.get().classloader().getResourceAsStream(LessonImage.PLACEHOLDER);
			return ok(imageStream);
		}
		
		if(isLarge){
			imageStream = lessonImage.download();
		}else{
			imageStream = lessonImage.downloadThumbnail();
		}
		
		return ok(imageStream);
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result deleteLessonImage(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
    	String imageUUID = requestData.get("imageUUID");
    	
    	TypedQuery<LessonImage> query = jpaApi.em()
				.createQuery("from LessonImage li where li.uuid = :uuid", LessonImage.class)
				.setParameter("uuid", imageUUID);
    	
    	try{
	    	LessonImage lessonImage = query.getSingleResult();
			lessonImage.deleteThumbnail();
			lessonImage.delete();
			
			jpaApi.em().remove(lessonImage);
    	}catch(NoResultException e){
			responseData.message = "Image does't exist.";
	    	responseData.code = 4000;
    	}
    	return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result updateLessonImageInfo(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String uuid = requestData.get("uuid");
    	String title = requestData.get("title");
    	String caption = requestData.get("caption");
    	boolean isCover = Integer.parseInt(requestData.get("isCover")) == 1;
    	
    	TypedQuery<LessonImage> query = jpaApi.em()
				.createQuery("from LessonImage li where li.uuid = :uuid", LessonImage.class)
				.setParameter("uuid", uuid);
    	
    	try{
    		LessonImage lessonImage = query.getSingleResult();
    		if(isCover){
    			jpaApi.em()
    			.createQuery("update LessonImage li set li.isCover = false where li.lesson = :lesson and li.isCover=:isCover")
    			.setParameter("isCover", true)
				.setParameter("lesson", lessonImage.lesson).executeUpdate();
        	}
    		
    		lessonImage.title = title;
    		lessonImage.caption = caption;
    		lessonImage.isCover = isCover;
    		jpaApi.em().persist(lessonImage);
    		
    		try {
    			responseData.data = lessonImage;
				return ok(Utils.toJson(ResponseData.class, responseData, "*.lesson"));
			} catch (JsonProcessingException e) {
				responseData.code = 4001;
				responseData.message = e.getLocalizedMessage();
			}
    	}catch(NoResultException e){
    		responseData.code = 4000;
			responseData.message = "Image does't exist.";
    	}
    	return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result lessonSession(long lessonId, int offset){
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
		
		if(lesson == null){
			flash().put("error", "The lesson cannot be found.");
			return redirect(routes.HomeController.index());
		}
		
		int totalAmount = ((Long)jpaApi.em()
				.createQuery("select count(*) from LessonSession ls where ls.lesson = :lesson")
				.setParameter("lesson", lesson).getSingleResult()).intValue();
		int pageIndex = (int) Math.ceil(offset / Constants.LESSON_SESSION_PAGE_SIZE) + 1;
		
		List<LessonSession> lessonSessions = jpaApi.em()
				.createQuery("from LessonSession ls where ls.lesson = :lesson order by ls.startDatetime asc", LessonSession.class)
				.setParameter("lesson", lesson)
				.setFirstResult(offset)
				.setMaxResults(Constants.LESSON_SESSION_PAGE_SIZE)
				.getResultList();
		
		return ok(lessonsession.render(lesson, lessonSessions, pageIndex, totalAmount));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result saveLessonSession(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		long lessonId = Long.parseLong(requestData.get("lessonId"));
		long chapterId = Long.parseLong(requestData.get("chapterId"));
		String title = requestData.get("title");
    	String brief = requestData.get("brief");
    	String duration = requestData.get("duration");
    	String startDatetime = requestData.get("startDatetime");
    	
    	Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
    	if(lesson == null){
    		responseData.message = "Lesson cannot be found.";
    		responseData.code = 4000;
    	}else{
    		LessonSession lessonSession = new LessonSession(lesson);
        	try {
    	    	lessonSession.title = title;
    	    	lessonSession.brief = brief;
    	    	lessonSession.duration = Integer.parseInt(duration);
    			lessonSession.startDatetime = Utils.parse(startDatetime);
    			lessonSession.endDatetime = Utils.addMinute(lessonSession.startDatetime, lessonSession.duration);
    			
    			if(chapterId > 0){
    				Chapter chapter = jpaApi.em().find(Chapter.class, chapterId);
    				if(chapter != null){
    					lessonSession.chapter = chapter;
    				}
    			}
    			
    			jpaApi.em().persist(lessonSession);
    			
    			//根据lessonSession来设置lesson的开始时间 
    			boolean updateLesson = false;
    			if(lesson.startDatetime == null || lesson.startDatetime.after(lessonSession.startDatetime)){
    				lesson.startDatetime = lessonSession.startDatetime;
    				updateLesson = true;
    			}
    			
    			//根据lessonSession来设置lesson的结束时间
    			if(lesson.endDatetime == null || lesson.endDatetime.before(lessonSession.endDatetime)){
    				lesson.endDatetime = lessonSession.endDatetime;
    				updateLesson = true;
    			}
    			
    			if(updateLesson){
    				jpaApi.em().persist(lesson);
    			}
    			
    			flash().put("success", "An new lesson session created successfully.");
    		} catch (ParseException e) {
    			responseData.code = 4001;
    			responseData.message = e.getLocalizedMessage();
    		}
    	}
    	return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result editLessonSession(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		long lessonSessionId = Long.parseLong(requestData.get("lessonSessionId"));
		long chapterId = Long.parseLong(requestData.get("chapterId"));
		String title = requestData.get("title");
		String brief = requestData.get("brief");
		String duration = requestData.get("duration");
    	String startDatetime = requestData.get("startDatetime");
    	
		LessonSession lessonSession = jpaApi.em().find(LessonSession.class, lessonSessionId);
		try {
			if(lessonSession != null){
				lessonSession.title = title;
				lessonSession.brief = brief;
				lessonSession.duration = Integer.parseInt(duration);
				lessonSession.startDatetime = Utils.parse(startDatetime);
				lessonSession.endDatetime = Utils.addMinute(lessonSession.startDatetime, lessonSession.duration);
				
				if(chapterId > 0){
    				Chapter chapter = jpaApi.em().find(Chapter.class, chapterId);
    				if(chapter != null){
    					lessonSession.chapter = chapter;
    				}
    			}else{
    				lessonSession.chapter = null;
    			}
				
				jpaApi.em().persist(lessonSession);
				
				flash().put("success", "Lesson session updated.");
			}else{
				responseData.code = 4000;
				responseData.message = "Lesson Session doesn't exist.";
			}
		}catch (ParseException e) {
			responseData.code = 4001;
			responseData.message = e.getLocalizedMessage();
		}
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result lessonSessions(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		long lessonId = Long.parseLong(requestData.get("lessonId"));
		
		List<LessonSession> lessonSessions = jpaApi.em()
				.createNativeQuery("SELECT * FROM lesson_session ls WHERE ls.lesson_id=:lesson_id ORDER BY ls.start_datetime", LessonSession.class)
				.setParameter("lesson_id", lessonId)
				.getResultList();
		
		if(lessonSessions == null){
			responseData.code = 4000;
			responseData.message = "There is no any session found for this lesson.";
		}else{
			try {
				responseData.data = lessonSessions;
				ObjectMapper mapper = new ObjectMapper();
				JsonNode jsonData = mapper.readTree(Utils.toJson(ResponseData.class, responseData, "*.lesson", "*.chapter", "*.broadcastSessions", "*.mediaFiles", "*.question", "*.answer", "*.parent", "*.lessonSession"));
				return ok(Json.toJson(jsonData));
			} catch (IOException e) {
				responseData.code = 4001;
				responseData.message = e.getLocalizedMessage();
			} 
		}
		
		return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result createChapter(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		long lessonId = Long.parseLong(requestData.get("lessonId"));
		String title = requestData.get("title");
		String brief = requestData.get("brief");
		
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
		if(lesson != null){
			Chapter chapter = new Chapter(lesson);
			chapter.title = title;
			chapter.brief = brief;
			chapter.chapterIndex = lesson.chapters.size() + 1;
			jpaApi.em().persist(chapter);
		}else{
			responseData.code = 4000;
			responseData.message = "Lesson cannot be found.";
		}
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result lessonDetail(long lessonId){
		ResponseData responseData = new ResponseData();
		
		String token = ctx().session().get(AuthAction.LOGGED_KEY);
		Account account = null;
		if(!Utils.isBlank(token)){
			account = Account.findByToken(token);
		}

		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
    	if(lesson == null){
    		responseData.message = "Lesson cannot be found.";
    		responseData.code = 4000;
    		return notFound(errorpage.render(responseData));
    	}
    	
    	List<LessonSession> lessonSessionsWithoutChapter = jpaApi.em()
    			.createNativeQuery("SELECT * FROM lesson_session ls WHERE ls.lesson_id = :lessonId AND (ls.chapter_id IS NULL OR ls.chapter_id = 0)", LessonSession.class)
    			.setParameter("lessonId", lessonId)
    			.getResultList();
    	
    	DynamicForm requestData = formFactory.form().bindFromRequest();
		String transactionId = requestData.get("out_trade_no");
		if(!Utils.isBlank(transactionId)){
			String tradeStatus = requestData.get("trade_status");
			try {
				Payment paymentObj = jpaApi.em().createQuery("FROM Payment py WHERE py.transactionId=:transactionId", Payment.class)
						.setParameter("transactionId", transactionId).getSingleResult();
				paymentObj.notifyId = requestData.get("notify_id");
				paymentObj.status = tradeStatus;
				jpaApi.em().persist(paymentObj);
				
				UserLesson userLesson = new UserLesson(paymentObj.user, paymentObj.lesson);
				jpaApi.em().persist(userLesson);
			} catch (NoResultException e) {
				responseData.code = 4000;
				responseData.message = e.getLocalizedMessage();
			}
		}
    	
		return ok(lessondetail.render(account, lesson, lessonSessionsWithoutChapter));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result lessonPrice(long lessonId){
		ResponseData responseData = new ResponseData();
		
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
    	if(lesson == null){
    		responseData.message = "Lesson cannot be found.";
    		responseData.code = 4000;
    		return notFound(errorpage.render(responseData));
    	}
    	
		return ok(lessonprice.render(lesson));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result saveLessonPrice(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		Map<String, String[]> formMap = request().body().asFormUrlEncoded();
		
		long lessonId = Long.parseLong(requestData.get("lessonId"));
		double price = Double.parseDouble(requestData.get("price"));
		double offerPrice = Double.parseDouble(requestData.get("offerPrice"));
		String offerStartDate = requestData.get("offerStartDate");
		String offerEndDate = requestData.get("offerEndDate");
		String reason = requestData.get("reason");
		String[] ids = formMap.get("lessonSessions[]");	
		
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
    	if(lesson == null){
    		responseData.message = "Lesson cannot be found.";
    		responseData.code = 4000;
    	}else{
    		lesson.price = price;
    		
        	try {
        		if(offerPrice > 0){
        			Offer offer;
        			if(lesson.offers.size() > 0){
        				offer = lesson.offers.get(0);
        			}else{
        				offer = new Offer(lesson);
        			}
        			offer.offerPrice = offerPrice;
        			offer.reason = reason;
        			if(!Utils.isBlank(offerStartDate) && !Utils.isBlank(offerEndDate)){
        				offer.offerStartDate = Utils.parse(offerStartDate + " 00:00");
        				offer.offerEndDate = Utils.parse(offerEndDate + " 00:00");
        				jpaApi.em().persist(offer);
            		}else{
            			responseData.code = 5000;
            			responseData.message = "Offer Start and End Date cannot be empty.";
            		}
        		}
        		
        		jpaApi.em().createQuery("update LessonSession ls set ls.isTrial = false where ls.lesson=:lesson")
            	.setParameter("lesson", lesson).executeUpdate(); //set all lesson session isTrail to false
        		
        		if(ids != null && ids.length > 0){
    				String updateQueryStr = "update LessonSession ls set ls.isTrial = true where ";
                	for(int i = 0; i < ids.length; i++){
                		if(i == 0){
                			updateQueryStr += "ls.id = " + ids[i];
                		}else{
                			updateQueryStr += " or ls.id = " + ids[i];
                		}
                	}
                	jpaApi.em().createQuery(updateQueryStr).executeUpdate();
    			}
    		} catch (ParseException e) {
    			responseData.code = 4001;
    			responseData.message = e.getLocalizedMessage();
    		}
    	}
    	return ok(Json.toJson(responseData));
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result studentsOfLesson(long lessonId, int offset){
		ResponseData responseData = new ResponseData();
		
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
		if(lesson != null){
			int totalAmount = lesson.userLessons.size();
			int pageIndex = (int) Math.ceil(offset / Constants.LESSON_SESSION_PAGE_SIZE) + 1;
			
			List<Object[]> resultList = jpaApi.em()
					.createNativeQuery("SELECT u.account_id, u.user_name, ul.progress FROM user u INNER JOIN user_lesson ul ON u.account_id = ul.user_id WHERE ul.lesson_id = :lessonId")
					.setParameter("lessonId", lessonId)
					.setFirstResult(offset)
					.setMaxResults(Constants.LESSON_SESSION_PAGE_SIZE)
					.getResultList();
			
			List<StudentVO> studentVOs = null;
			if(resultList != null){
				studentVOs = new ArrayList<>();
				for(Object[] result : resultList){
					StudentVO studentVO = new StudentVO(Long.parseLong(result[0].toString()), result[1] == null ? "" : result[1].toString(), null, Double.parseDouble(result[2].toString()));
					studentVOs.add(studentVO);
				}
			}
			return ok(lessonstudents.render(lesson, studentVOs, pageIndex, totalAmount));
		}else{
			responseData.code = 4000;
			responseData.message = "The lesson does't exist.";
			return notFound(errorpage.render(responseData));
		}
	}
	
	@With(AuthAction.class)
	@Transactional
	public Result publishLesson(){
		ResponseData responseData = new ResponseData();
		DynamicForm requestData = formFactory.form().bindFromRequest();
		
		long lessonId = Long.parseLong(requestData.get("lessonId"));
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
    	if(lesson == null){
    		responseData.message = "Lesson cannot be found.";
    		responseData.code = 4000;
    	}else if(Utils.isBlank(lesson.title)){
    		responseData.message = "Title must be set.";
    		responseData.code = 5000;
    	}else if(Utils.isBlank(lesson.description)){
    		responseData.message = "Description must be set.";
    		responseData.code = 5000;
    	}else if(lesson.price == 0){
    		responseData.message = "Price must be set.";
    		responseData.code = 5000;
    	}else if(lesson.isPublish){
    		responseData.message = "The lesson already published.";
    		responseData.code = 5000;
    	}else if(lesson.lessonImages == null || lesson.lessonImages.size() == 0){
    		responseData.message = "Lesson image must be set.";
    		responseData.code = 5000;
    	}else if(lesson.lessonSessions == null || lesson.lessonSessions.size() == 0){
    		responseData.message = "There is no any lesson session in this lesson.";
    		responseData.code = 5000;
    	}else{
    		lesson.isPublish = true;
    		lesson.publishDatetime = new Date();
    		jpaApi.em().persist(lesson);
    		
    		responseData.message = "The lesson was published successfully.";
    	}
		
    	return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result publishedLessons(int offset, long categoryId, String lessonKeys){
		String whereCategoryClause = "";
		if(categoryId > 0){
			whereCategoryClause += " ls.category_id="+ categoryId;
		}
		
		String whereKeyClause = "";
		if(!Utils.isBlank(lessonKeys)){
			for(int i = 0; i < lessonKeys.length(); i++){
				int index = Character.getNumericValue(lessonKeys.charAt(i));
				if(index != 0){
					whereKeyClause += " (ls.lesson_key=" + index + ") OR";
				}
			}
		}
		
		if(!Utils.isBlank(whereKeyClause)){
			whereKeyClause = " AND " + whereKeyClause.substring(0, whereKeyClause.length() - 2);
		}
		
		String whereClause = " WHERE ls.end_datetime > :datetime AND ls.is_publish = :isPublish";
		if(!Utils.isBlank(whereCategoryClause) || !Utils.isBlank(whereKeyClause)){
			whereClause += " AND " + whereCategoryClause + whereKeyClause;
 		}
		
		if(whereClause.contains("AND  AND")){
			whereClause = whereClause.replace("AND  AND", "AND");
		}
		
		List<Lesson> lessonList = jpaApi.em()
				.createNativeQuery("SELECT * FROM lesson ls" + whereClause + " ORDER BY ls.end_datetime DESC", Lesson.class)
				.setParameter("datetime", new Date())
				.setParameter("isPublish", true)
				.setFirstResult(offset)
				.setMaxResults(Constants.ALL_LESSON_PAGE_SIZE).getResultList();
		
		Query cateQuery = jpaApi.em().createQuery("from Category cg where cg.parent = null", Category.class);
		List<Category> categories = cateQuery.getResultList();
		
		String token = session().get(AuthAction.LOGGED_KEY);
		Account account = null;
		if(!Utils.isBlank(token)){
			TypedQuery<Account> query = jpaApi.em()
					.createQuery("from Account ac where ac.token = :token", Account.class)
		            .setParameter("token", token);
			try{
				account = query.getSingleResult();
			}catch(NoResultException e){
				e.printStackTrace();
			}
		}
		
		int totalAmount = ((BigInteger)jpaApi.em()
				.createNativeQuery("SELECT COUNT(*) from lesson ls" + whereClause)
				.setParameter("datetime", new Date())
				.setParameter("isPublish", true)
				.getSingleResult()).intValue();
		int pageIndex = (int) Math.ceil(offset / Constants.ALL_LESSON_PAGE_SIZE) + 1;
		
		return ok(lessons.render(account, lessonList, categories, pageIndex, totalAmount, categoryId, lessonKeys));
	}
	
	@Transactional
	public Result advisoryLesson(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String userId = requestData.get("userId");
		long lessonId = Long.parseLong(requestData.get("lessonId"));
		String content = requestData.get("content");
		String phone = requestData.get("phone");
		String email = requestData.get("email");
		String name = requestData.get("name");
		
		Lesson lesson = jpaApi.em().find(Lesson.class, lessonId);
		if(lesson != null){
			Advisory advisory = new Advisory(lesson);
			if(!Utils.isBlank(userId)){
				User user = jpaApi.em().find(User.class, Long.parseLong(userId));
				advisory.user = user;
			}
			advisory.name = name;
			advisory.phone = phone;
			advisory.content = content;
			advisory.email = email;
			jpaApi.em().persist(advisory);
		}else{
			responseData.code = 4000;
			responseData.message = "Lesson cannot be found.";
		}
		
		return ok(Json.toJson(responseData));
	}
	
}


