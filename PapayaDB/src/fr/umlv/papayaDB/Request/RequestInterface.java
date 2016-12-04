package fr.umlv.papayaDB.Request; 

import java.util.function.Consumer;

import io.vertx.core.json.JsonObject;

public interface RequestInterface {
	
	public void createNewDatabase(String name, String user, String hash, Consumer<RequestReturn> callback);
	public void deleteDatabase(String name, String user, String hash, Consumer<RequestReturn> callback);
	public void exportDatabase(String database, Consumer<RequestReturn> callback);
	
	public void updateRecord(String database, String uid, JsonObject obj, String user, String hash, Consumer<RequestReturn> callback);
	public void deleteRecords(String database, JsonObject obj, String user, String hash, Consumer<RequestReturn> callback);
	public void insertNewRecord(String database, JsonObject obj, String user, String hash, Consumer<RequestReturn> callback);
	public void getRecords(String database, JsonObject obj, Consumer<RequestReturn> callback);
	
	public default void close() {}
	
	public static RequestInterface newHttpQueryInterface(String host, int port) {
		return new HttpRequestInterface(host, port);
	}
	
	public static RequestInterface newTcpQueryInterface(String host, int port) {
		return new TcpRequestInterface(host, port);
	}
}