package org.aivan.ratserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aivan.ratserver.model.User;
import org.aivan.ratserver.model.UserRepository;
import ratpack.exec.Blocking;
import ratpack.exec.Promise;
import ratpack.func.Block;
import ratpack.handling.Context;
import ratpack.server.RatpackServer;
import ratpack.spring.Spring;

import java.util.HashMap;
import java.util.Map;

public class Main {
	private static final Map<String, String> NOT_FOUND = new HashMap<String, String>() {
		{
			put("status", "404");
			put("message", "NOT FOUND");
		}
	};
	private static final Map<String, String> NO_EMAIL = new HashMap<String, String>() {
		{
			put("status", "400");
			put("message", "NO EMAIL ADDRESS SUPPLIED");
		}
	};

	public static void main(String[] args) throws Exception {
		RatpackServer.start(spec -> spec.registry(Spring.spring(SpringBootConfig.class)).handlers(
				chain -> chain.prefix("api/users", pchain -> pchain.prefix(":username", uchain -> uchain.all(ctx -> {
					// extract the "username" path variable
					String username = ctx.getPathTokens().get("username");
					// pull the UserRepository out of the registry
					UserRepository userRepository = ctx.get(UserRepository.class);
					// pull the Jackson ObjectMapper out of the registry
					ObjectMapper mapper = ctx.get(ObjectMapper.class);
					// construct a "promise" for the requested user object. This
					// will
					// be subscribed to within the respective handlers,
					// according to what
					// they must do. The promise uses the "blocking" fixture to
					// ensure
					// the DB call doesn't take place on a "request taking"
					// thread.
					Promise<User> userPromise = Blocking.get(() -> userRepository.findByUsername(username));
					ctx.byMethod(method -> method
							.get(() ->
								// the .then() block will "subscribe" to the result,
								// allowing
								// us to send the user domain object back to the client
								userPromise.then(user -> sendUser(ctx, user)))
							.put(() -> {
								// Read the JSON from the request
								ctx.getRequest().getBody().then(request -> {
									String json = request.getText();
									// Parse out the JSON body into a Map
									Map<String, String> body = mapper.readValue(json, new TypeReference<Map<String, String>>() {
									});
									// Check to make sure the request body contained an
									// "email" address
									if (body.containsKey("email")) {
										userPromise
												// map the new email address on to the
												// user entity
												.map(user -> {
													user.setEmail(body.get("email"));
													return user;
												})
												// and use the blocking thread pool to
												// save the updated details
												.blockingMap(userRepository::save)
												// finally, send the updated user entity
												// back to the client
												.then(u1 -> sendUser(ctx, u1));
									} else {
										// bad request; we didn't get an email address
										ctx.getResponse().status(400);
										ctx.getResponse().send(mapper.writeValueAsBytes(NO_EMAIL));
									}
								});
							})
							.delete(() -> userPromise
								// make the DB delete call in a blocking thread
								.blockingMap(user -> {
									userRepository.delete(user);
									return null;
								})
								// then send a 204 back to the client
								.then(user -> {
									ctx.getResponse().status(204);
									ctx.getResponse().send();
								}))
							);
				})).all(ctx -> {
					// pull the UserRepository out of the registry
					UserRepository userRepository = ctx.get(UserRepository.class);
					// pull the Jackson ObjectMapper out of the registry
					ObjectMapper mapper = ctx.get(ObjectMapper.class);
					ctx.byMethod(method -> method
							.post(saveOrUpdateUser(ctx, userRepository, mapper))
							.get(getAllUsers(ctx, userRepository, mapper)));
				}))));
	}

	private static Block getAllUsers(Context ctx, UserRepository userRepository, ObjectMapper mapper) {
		return () ->
		// make the DB call, on a blocking thread, to list all users
		Blocking.get(userRepository::findAll)
				// and render the user list back to the client
				.then(users -> {
					ctx.getResponse().contentType("application/json");
					ctx.getResponse().send(mapper.writeValueAsBytes(users));
				});
	}

	private static Block saveOrUpdateUser(Context ctx, UserRepository userRepository, ObjectMapper mapper) {
		return () -> {
			// read the JSON request body...
			ctx.getRequest().getBody().then(body -> {
				String json = body.getText();
				// ... and convert it into a user entity
				User user = mapper.readValue(json, User.class);
				// save the user entity on a blocking thread and
				// render the user entity back to the client
				Blocking.get(() -> userRepository.save(user)).then(u1 -> sendUser(ctx, u1));
			});
		};
	}

	private static void notFound(Context context) {
		ObjectMapper mapper = context.get(ObjectMapper.class);
		context.getResponse().status(404);
		try {
			context.getResponse().send(mapper.writeValueAsBytes(NOT_FOUND));
		} catch (JsonProcessingException e) {
			context.getResponse().send();
		}
	}

	private static void sendUser(Context context, User user) {
		if (user == null) {
			notFound(context);
		}

		ObjectMapper mapper = context.get(ObjectMapper.class);
		context.getResponse().contentType("application/json");
		try {
			context.getResponse().send(mapper.writeValueAsBytes(user));
		} catch (JsonProcessingException e) {
			context.getResponse().status(500);
			context.getResponse().send("Error serializing user to JSON");
		}
	}
}