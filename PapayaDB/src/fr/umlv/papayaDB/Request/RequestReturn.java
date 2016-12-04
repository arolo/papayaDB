package fr.umlv.papayaDB.Request;

import java.util.List;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public class RequestReturn {
	private JsonObject content;
	private RequestStatus status;
	
	public RequestReturn(JsonObject obj) {
		if(!obj.containsKey("status") || (obj.getString("status") == RequestStatus.OK.name() && !obj.containsKey("data"))) {
			throw new IllegalArgumentException("JsonObject incorrect :" + obj.toString());
		}
		this.status = RequestStatus.valueOf(obj.getString("status"));
		this.content = obj;
	}
	
	@Override
	public String toString() {
		return status.name() + " : " + content.encodePrettily();
	}
	
	public JsonObject getData() {
		return this.content;
	}
	
	public static RequestReturn buildNewErrorAnswer(RequestStatus status, String msg) {
		if(status == RequestStatus.OK) throw new IllegalArgumentException("OK is not an error !!");
		return new RequestReturn(new JsonObject().put("Type", status.name()).put("Message", msg));
	}
	
	public static RequestReturn buildNewDataAnswer(List<JsonObject> obj) {
		return new RequestReturn(new JsonObject().put("Type", RequestStatus.OK.name()).put("data", new JsonArray(obj)));
	}
	
	public static RequestReturn buildNewEmptyOkAnswer() {
		return new RequestReturn(new JsonObject().put("Type", RequestStatus.OK.name()));
	}
}