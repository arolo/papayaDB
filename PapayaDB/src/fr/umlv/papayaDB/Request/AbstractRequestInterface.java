package fr.umlv.papayaDB.Request;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx; 


public abstract class AbstractRequestInterface extends AbstractVerticle implements RequestInterface {
	private final Vertx vertx;

	public AbstractRequestInterface() {
		vertx = Vertx.vertx();
	}
	
	public Vertx getVertx() {
		return vertx;
	}
	
	public void close() {
		vertx.close();
	}
}