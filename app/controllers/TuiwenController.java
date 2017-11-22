package controllers;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import com.froala.editor.S3;
import com.froala.editor.s3.S3Config;
import com.google.gson.Gson;

import actions.AdminAction;
import models.Account;
import models.ResponseData;
import models.TuiWen;
import models.TuiWenType;
import play.Application;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import services.S3Plugin;
import tools.Utils;
import views.html.createtuiwen;
import views.html.tuiwen;
import views.html.tuiwens;

public class TuiwenController extends Controller{
	@Inject private Provider<Application> application;
	@Inject private FormFactory formFactory;
	@Inject private JPAApi jpaApi;
	
	@With(AdminAction.class)
	@Transactional
	public Result createTuiwen(){
		Account account = (Account) ctx().args.get("account");
		return ok(createtuiwen.render(account));
	}
	
	@With(AdminAction.class)
	@Transactional
	public Result saveTuiwen(){
		ResponseData responseData = new ResponseData();
		
		DynamicForm requestData = formFactory.form().bindFromRequest();
		String title = requestData.get("title");
		String content = requestData.get("content");
		int type = Integer.parseInt(requestData.get("type"));
		if(!Utils.isBlank(content)){
			TuiWen tuiWen = new TuiWen(title, content, TuiWenType.getTuiWenType(type));
			jpaApi.em().persist(tuiWen);
		}else{
			responseData.code = 4000;
			responseData.message = "Content cannot be empty.";
		}
		return ok(Json.toJson(responseData));
	}
	
	@Transactional
	public Result viewTuiwen(long tuiwenId){
		TuiWen tuiWen = jpaApi.em().find(TuiWen.class, tuiwenId);
		if(tuiWen == null){
			flash("error", "The article cannot be found.");
			return redirect(routes.HomeController.index());
		}
		
		Account account = (Account) ctx().args.get("account");
		return ok(tuiwen.render(account, tuiWen));
	}
	
	@Transactional
	public Result tuiwens(int type){
		List<TuiWen> tuiWens = null;
		if(type == -1){
			tuiWens = jpaApi.em().createQuery("FROM TuiWen ORDER BY uploadedDateTime desc", TuiWen.class).getResultList();
		}else{
			tuiWens = jpaApi.em().createQuery("FROM TuiWen tw WHERE tw.type=:type ORDER BY uploadedDateTime desc", TuiWen.class)
					.setParameter("type", TuiWenType.getTuiWenType(type))
					.getResultList();
		}
		
		Account account = (Account) ctx().args.get("account");
		return ok(tuiwens.render(account, tuiWens, type));
	}
	
	
	public Result getImage(){
        S3Config config = new S3Config();
        config.bucket = "ekooedu-article";
        config.region = "ap-southeast-1";
        config.acl = "public-read";
        config.accessKey = application.get().configuration().getString(S3Plugin.AWS_ACCESS_KEY);
        config.secretKey = application.get().configuration().getString(S3Plugin.AWS_SECRET_KEY);

        try {
        	Map<Object, Object> hash = S3.getHash(config);
        	String jsonResponseData = new Gson().toJson(hash);
        	return ok(jsonResponseData);
        } catch (Exception e) {
        	ResponseData responseData = new ResponseData();
        	responseData.code = 4001;
        	responseData.message = e.getLocalizedMessage();
        	return ok(Json.toJson(responseData));
        }
	}
}
