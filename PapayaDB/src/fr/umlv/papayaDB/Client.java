package fr.umlv.papayaDB;

import java.util.Objects;
import java.util.function.Consumer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.core.http.HttpClient;


public class Client {
	private final HttpClient client;
	private final String host;
	private final int port;
	Vertx vertx = Vertx.vertx();				// To delete later
	
	
	public Client(String host, int port){
		client = vertx.createHttpClient();		// client = getVertx().createHtt...
		this.port = port;
		this.host = host;
	}
	
	public void close(){
		client.close();
	}
	
//	public void request(String request, Consumer<Answer> answer){
//		Objects.requireNonNull(answer);
//		client.getNow(port, host, "/"+request, response ->{
//			response.bodyHandler(bodyBuffer -> {answer.accept(new Answer(bodyBuffer.toJsonObject()));
//			});
//		});
//	}
}
