package fr.umlv.papayaDB.Request;

import java.util.Objects; 
import java.util.function.Consumer;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;

class HttpRequestInterface extends AbstractRequestInterface {
	private final HttpClient client;
	private final int port;			
	private final String host;	
	

	
	public HttpRequestInterface(String host, int port) {
		client = getVertx().createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));
		this.host = host;
		this.port = port;
	}
	
	@Override
	public void close() {
		client.close();
		super.close();
	}
	
	public void getRequest(String request, Consumer<RequestReturn> cons) {
		Objects.requireNonNull(cons);
		client.getNow(port, host, request, resp -> {
			System.out.println("Answer " + resp.statusCode());
			System.out.println("[getRequest] Answer " + resp.statusCode());
			resp.bodyHandler(body -> {
				cons.accept(new RequestReturn(body.toJsonObject()));
			});
		});
	}
	
	public void followingRequest(String query, JsonObject content, String user, String hash, Consumer<RequestReturn> cons) {
		Objects.requireNonNull(cons);
		query = query + "/auth/[" + user + ";" + hash + "]";
		HttpClientRequest request = client.post(port, host, query, resp -> {
			System.out.println("Answer " + resp.statusCode());
			System.out.println("[followingRequest] Answer " + resp.statusCode());
			resp.bodyHandler(bodyResponse -> {
				cons.accept(new RequestReturn(bodyResponse.toJsonObject()));
			});
		});
		if(content == null) {
			request.end();
		} else {
			request.end(content.toString());
		}
		
	}
	
	public void removeRequest(String query, String user, String hash, Consumer<RequestReturn> cons) {
		Objects.requireNonNull(cons);
		query = query + "/auth/[" + user + ";" + hash + "]";
		client.delete(port, host, query, resp -> {
			System.out.println("Answer " + resp.statusCode());
			System.out.println("[removeRequest] Answer " + resp.statusCode());
			resp.bodyHandler(bodyResponse -> {
				cons.accept(new RequestReturn(bodyResponse.toJsonObject()));
			});
		}).end();
	}

	@Override
	public void createNewDatabase(String name, String user, String hash, Consumer<RequestReturn> cons) {
		Objects.requireNonNull(name);
		followingRequest("/createdb/" + name, null, user, hash, cons);
	}

	@Override
	public void deleteDatabase(String name, String user, String hash, Consumer<RequestReturn> cons) {
		Objects.requireNonNull(name);
		removeRequest("/deletedb/" + name, user, hash, cons);
	}

	@Override
	public void exportDatabase(String db, Consumer<RequestReturn> cons) {
		Objects.requireNonNull(db);
		getRequest("/exportall/" + db ,cons);
	}

	@Override
	public void updateRecord(String db, String uid, JsonObject obj, String user, String hash, Consumer<RequestReturn> cons) {
		Objects.requireNonNull(db);
		Objects.requireNonNull(uid);
		Objects.requireNonNull(obj);
		followingRequest("/update/" + db, new JsonObject().put("uid", uid).put("record", obj), user, hash, cons);
	}

	@Override
	public void deleteRecords(String db, JsonObject obj, String user, String hash, Consumer<RequestReturn> cons) {
		Objects.requireNonNull(db);
		Objects.requireNonNull(obj);
		StringBuilder sb = new StringBuilder("/delete/" + db);
		for (String key: obj.fieldNames()) {
			sb.append("/" + RequestParameter.getParameterKey(RequestType.GET, key).get().valueToString(key, obj.getJsonObject(key)));
		}
		removeRequest(sb.toString(), user, hash, cons);
	}

	@Override
	public void insertNewRecord(String db, JsonObject obj, String user, String hash, Consumer<RequestReturn> cons) {
		Objects.requireNonNull(db);
		Objects.requireNonNull(obj);
		followingRequest("/insert/" + db, obj, user, hash, cons);
	}

	@Override
	public void getRecords(String db, JsonObject obj, Consumer<RequestReturn> cons) {
		Objects.requireNonNull(db);
		Objects.requireNonNull(obj); 
		StringBuilder sb = new StringBuilder("/get/" + db);
		for (String key: obj.fieldNames()) {
			sb.append("/" + RequestParameter.getParameterKey(RequestType.GET, key).get().valueToString(key, obj.getJsonObject(key)));
		}
		getRequest(sb.toString(), cons);
	}
}