package filters;

import akka.stream.Materializer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;
import javax.inject.*;
import play.mvc.*;
import play.mvc.Http.RequestHeader;


/**
 * This is a simple filter that adds a header to all requests. It's
 * added to the application's list of filters by the
 * {@link Filters} class.
 */
@Singleton
public class SSLFilter extends Filter {

    private final Executor exec;

    /**
     * @param mat This object is needed to handle streaming of requests
     * and responses.
     * @param exec This class is needed to execute code asynchronously.
     * It is used below by the <code>thenAsyncApply</code> method.
     */
    @Inject
    public SSLFilter(Materializer mat, Executor exec) {
        super(mat);
        this.exec = exec;
    }

    @Override
    public CompletionStage<Result> apply(
        Function<RequestHeader, CompletionStage<Result>> next,
        RequestHeader requestHeader) {
    	
    	if(!requestHeader.secure()){
    		return CompletableFuture.completedFuture(Results.movedPermanently("https://" + requestHeader.host() + requestHeader.uri()));
    	}else{
    		return next.apply(requestHeader).thenApplyAsync(
    	            result -> result,
    	            exec
    	        );
    	}
    }

}


