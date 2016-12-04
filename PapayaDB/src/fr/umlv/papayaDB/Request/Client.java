package fr.umlv.papayaDB.Request;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.rxjava.core.Vertx;


public class Client extends AbstractVerticle{
	public static void main(String[] args) {
		RequestInterface client = RequestInterface.newHttpQueryInterface("localhost", 8080);
		setSSLWithKeystore("keystore.jks");
		
		Vertx.vertx().createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true))
						.getNow(8080, "localhost", "/get/testDb/limit/8", resp -> {
								System.out.println("Got response " + resp.statusCode());
								resp.bodyHandler(body -> System.out.println("Got data " + body.toString("ISO-8859-1")));
						});
	}
	
	public static void setSSLWithKeystore(String keystorePath) {
		System.setProperty("javax.net.ssl.keyStorePassword", "papayadb");
		System.setProperty("javax.net.ssl.trustStore", keystorePath);
		System.setProperty("javax.net.ssl.keyStore", keystorePath);
		System.setProperty("javax.net.ssl.trustStoreType", "jks");
	}
}