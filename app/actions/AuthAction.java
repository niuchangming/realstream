package actions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import models.Account;
import play.db.jpa.JPAApi;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;
import tools.Utils;

public class AuthAction extends Action.Simple{
	public static final String LOGGED_KEY = "logged";
	
	@Inject
	private JPAApi jpaApi;
	
	@Override
	public CompletionStage<Result> call(Context ctx) {
		String token = ctx.session().get(LOGGED_KEY);
		
		if(Utils.isBlank(token)){
			ctx.flash().put("error", "You have to login.");
			return CompletableFuture.completedFuture(redirect(controllers.routes.HomeController.index()));
		}else{
			Account account = jpaApi.withTransaction(entityManager -> {
				return Account.findByToken(token);
			});
			
	        if (account != null) {
	        	ctx.args.put("account", account);
	        	return delegate.call(ctx);
	        }else{
	        	ctx.flash().put("error", "You have to login.");
	        	return CompletableFuture.completedFuture(redirect(controllers.routes.HomeController.index()));
	        }
		}
	}
	
}
