package org.aivan.ratserver;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ratpack.exec.Blocking;
import ratpack.exec.Promise;
import ratpack.rx.RxRatpack;
import ratpack.server.RatpackServer;
import rx.Observable;

public class Main1 {

	static Logger log = LoggerFactory.getLogger(Main1.class);

	
	public static void main(String[] args) throws Exception {
		RxRatpack.initialize(); // must be called once per JVM

		RatpackServer
				.start(serverSpec -> serverSpec.handlers(chain -> chain.prefix("api/1", prefix -> prefix.get(ctx -> {
					log.debug("get 1 start");
					
					Promise<String> promise = Blocking.get(() -> getTextWithPause(1));
					promise.then(val -> {
						log.debug("then start");
						ctx.getResponse().send(val);	
						log.debug("then end");
					});

					log.debug("get 1 end");
					
//					Observable.<String>fromCallable(new Callable<String>() {
//						@Override
//						public String call() throws Exception {
//							return ;
//						}
//					}).to(RxRatpack::promiseSingle).then(val -> {
//						ctx
//					});

				})).prefix("api/2", prefix -> prefix.get(ctx -> {
					log.debug("get 2 start");
					
					Promise<String> promise = Blocking.get(() -> getTextWithPause(2));
					promise.then(val -> {
						log.debug("then start");
						ctx.getResponse().send(val);	
						log.debug("then end");
					});

					log.debug("get 2 end");
				}))));
	}

	static String getTextWithPause(int i) {
		log.debug("getTextWithPause "+i+" start");
		try {
			Thread.sleep(1000 * i);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.debug("getTextWithPause "+i+" end");
		return i + "";
	}
}
