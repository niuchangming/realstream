package actions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import models.Account;
import models.Role;
import play.db.jpa.JPAApi;
import play.mvc.Action;
import play.mvc.Http.Context;
import tools.Utils;
import play.mvc.Result;

public class AdminAction extends Action.Simple{
	@Inject private JPAApi jpaApi;
	
	@Override
	public CompletionStage<Result> call(Context ctx) {
		String token = ctx.session().get(AuthAction.LOGGED_KEY);
		
		if(Utils.isBlank(token)){
			ctx.flash().put("error", "You have to login.");
			return CompletableFuture.completedFuture(redirect(controllers.routes.HomeController.adminLogin()));
		}else{
			Account account = jpaApi.withTransaction(entityManager -> {
				return Account.findByToken(token);
			});
			
	        if (account != null && account.user.role == Role.ADMIN) {
	        	ctx.args.put("account", account);
	        	return delegate.call(ctx);
	        }else{
	        	ctx.flash().put("error", "Your account has no such permission.");
	        	return CompletableFuture.completedFuture(redirect(controllers.routes.HomeController.adminLogin()));
	        }
		}
	}

}
