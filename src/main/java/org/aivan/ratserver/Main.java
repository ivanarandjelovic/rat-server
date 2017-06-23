package org.aivan.ratserver;

import org.aivan.ratserver.spring.SpringBootConfig;
import ratpack.server.RatpackServer;
import ratpack.spring.Spring;

public class Main {

    public static void main(String[] args) throws Exception {
        RatpackServer.start(spec -> spec
                .registry(Spring.spring(SpringBootConfig.class))
                .handlers(chain -> chain
                        .prefix("api/users", pchain -> pchain
                                .prefix(":username", uchain -> uchain
                                        .all(ctx -> {
                                            String username = ctx.getPathTokens().get("username");
                                            ctx.byMethod(method -> method
                                                    .get(() -> ctx.render("Received request for user: " + username))
                                                    .put(() -> {
                                                        String json = ctx.getRequest().getBody().toString();
                                                        ctx.render("Received update request for user: " + username + ", JSON: " + json);
                                                    })
                                                    .delete(() -> ctx.render("Received delete request for user: " + username))
                                            );
                                        })
                                )
                                .all(ctx -> ctx
                                        .byMethod(method -> method
                                                .post(() -> {
                                                    String json = ctx.getRequest().getBody().toString();
                                                    ctx.render("Received request to create a new user with JSON: " + json);
                                                })
                                                .get(() -> ctx.render("Received request to list all users"))
                                        )
                                )
                        )
                )
    );
    }
}