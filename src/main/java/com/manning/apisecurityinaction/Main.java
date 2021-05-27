package com.manning.apisecurityinaction;

import static spark.Spark.after;
import static spark.Spark.afterAfter;
import static spark.Spark.exception;
import static spark.Spark.internalServerError;
import static spark.Spark.notFound;
import static spark.Spark.post;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.dalesbred.Database;
import org.dalesbred.result.EmptyResultException;
import org.h2.jdbcx.JdbcConnectionPool;
import org.json.JSONException;
import org.json.JSONObject;

import com.manning.apisecurityinaction.controller.SpaceController;

import spark.Request;
import spark.Response;

public class Main {

  public static void main(String... args) throws Exception {
    var dataSource = JdbcConnectionPool.create("jdbc:h2:mem:natter", "natter", "password");
    var database = Database.forDataSource(dataSource);
    createTables(database);

    dataSource = JdbcConnectionPool.create("jdbc:h2:mem:natter", "natter_api_user", "password");
    database = Database.forDataSource(dataSource);

    var spaceController = new SpaceController(database);
    post("/spaces", spaceController::createSpace);
    after((request, response) -> response.type("application/json"));
    internalServerError(new JSONObject().put("error", "internal server error").toString());
    notFound(new JSONObject().put("error", "not found").toString());
    exception(IllegalArgumentException.class, Main::badRequest);
    exception(JSONException.class, Main::badRequest);
    exception(EmptyResultException.class, (e, req, res) -> res.status(404));
    
    afterAfter((request, response) -> {
        response.header("Server", "");
        response.header("X-XSS-Protection", "0");
  });
  }
  
  private static void badRequest(Exception ex, Request request, Response response) {
      response.status(400);
      response.body("{\"error\": \"" + ex.getMessage() + "\"}");
  }

  private static void createTables(final Database database) throws URISyntaxException, IOException {
    var path = Paths.get(Main.class.getResource("/schema.sql").toURI());
    database.update(Files.readString(path));
  }
}