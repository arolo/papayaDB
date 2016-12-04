package fr.umlv.papayaDB.Request;

import java.util.Objects;
import java.util.function.Consumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions; 
import io.vertx.core.net.NetSocket;

class TcpRequestInterface extends AbstractRequestInterface {
	private final NetClient client;	
	private final int port;	
	private String host;
	
	

	public TcpRequestInterface(String host, int port) {
		NetClientOptions options = new NetClientOptions();
		client = getVertx().createNetClient(options);
		this.port = port;
		this.host = host;
	}
	
	
	@Override
	public void close() {
		client.close();
		super.close();
	}
	
	private void processQuery(String database, JsonObject obj, String user, String hash, Consumer<RequestReturn> callback) {
		Objects.requireNonNull(obj);
		Objects.requireNonNull(callback);
		Objects.requireNonNull(user);
		Objects.requireNonNull(hash);
		
		obj.put("db", database)
				   .put("user", user)
				   .put("password", hash);
		
		
		client.connect(port, host, connectHandler -> {
			if (connectHandler.succeeded()) {
				System.out.println("Connected !");
				NetSocket socket = connectHandler.result();
				
				socket.handler(buffer -> {
					JsonObject answer = buffer.toJsonObject();
					System.out.println("Request answer: " + answer);
					callback.accept(new RequestReturn(answer));
				});

				socket.write(obj.encode());

			} else {
				callback.accept(new RequestReturn(new JsonObject().put("status", RequestStatus.HOST_UNREACHABLE.name()).put("message", "Connection immpossible !!")));
			}
		});
	}
	

	@Override
	public void createNewDatabase(String name, String user, String hash, Consumer<RequestReturn> callback) {
		Objects.requireNonNull(name);
		Objects.requireNonNull(user);
		Objects.requireNonNull(hash);
		processQuery(name, new JsonObject().put("type", RequestType.CREATEDB.name()), user, hash, callback);
	}


	@Override
	public void deleteDatabase(String name, String user, String hash, Consumer<RequestReturn> callback) {
		Objects.requireNonNull(name);
		processQuery(name, new JsonObject().put("type", RequestType.DELETEDB.name()), user, hash, callback);
	}


	@Override
	public void updateRecord(String database, String uid, JsonObject obj, String user, String hash, Consumer<RequestReturn> callback) {
		Objects.requireNonNull(uid);
		Objects.requireNonNull(obj);
		processQuery(database, new JsonObject().put("type", RequestType.UPDATE.name())
									 			.put("oldRecord", uid)
									 			.put("newRecord", obj), user, hash, callback);
	}
	
	
	@Override
	public void deleteRecords(String database, JsonObject obj, String user, String hash, Consumer<RequestReturn> callback) {
		Objects.requireNonNull(database);
		Objects.requireNonNull(obj);
		processQuery(database, new JsonObject().put("type", RequestType.DELETE).put("parameters", obj), user, hash, callback);
	}
	

	@Override
	public void insertNewRecord(String database, JsonObject obj, String user, String hash, Consumer<RequestReturn> callback) {
		Objects.requireNonNull(database);
		Objects.requireNonNull(obj);
		processQuery(database, new JsonObject().put("type", RequestType.INSERT.name()).put("newRecord", obj), user, hash, callback);
	}


	@Override
	public void getRecords(String database, JsonObject obj, Consumer<RequestReturn> callback) {
		Objects.requireNonNull(database);
		Objects.requireNonNull(obj);
		processQuery(database, new JsonObject().put("type", RequestType.GET).put("parameters", obj), null, null, callback);
	}

	
	@Override
	public void exportDatabase(String database, Consumer<RequestReturn> callback) {
		Objects.requireNonNull(database);
		processQuery(database, new JsonObject().put("type", RequestType.EXPORTDB), null, null, callback);
	}
}