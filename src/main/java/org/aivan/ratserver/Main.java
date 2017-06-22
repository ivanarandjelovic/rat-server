package org.aivan.ratserver;

import ratpack.server.RatpackServer;

public class Main {

  public static void main(String[] args) throws Exception {
    RatpackServer.start(spec -> spec
      .handlers(chain -> chain
          .prefix("api", pchain -> pchain
            .all(ctx -> ctx
              .byMethod(method -> method
                .get(() -> ctx.render("Received GET request"))
                .post(() -> ctx.render("Received POST request"))
                .put(() -> ctx.render("Received PUT request"))
                .delete(() -> ctx.render("Received DELETE request"))
              )
            )
          )
      )
    );
  }
}
