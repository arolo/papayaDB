package fr.umlv.papayaDB.Request;

import java.util.List;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Contien l'Objet Json de réponse et le code de status.
 * Le code de status es contenu aussi dans l'objet JSON, il est présent dans la classe pour des raisons de confort.
 */

public class RequestResult {
	private JsonObject data;		
	private RequestAnswerStatus status;		
	
	public RequestResult(JsonObject answer) {
		if(!answer.containsKey("status") || (answer.getString("status") == QueryAnswerStatus.OK.name() && !answer.containsKey("data"))) {
			throw new IllegalArgumentException("JSON Object provided to build Query answer is malformed :"+answer.toString());
		}
		this.status = QueryAnswerStatus.valueOf(answer.getString("status"));
		this.data = answer;
	}
	
	@Override
	public String toString() {
		return status.name()+": "+data.encodePrettily();
	}
	
	public JsonObject getData() {
		return this.data;
	}
	
	public static RequestResult buildNewErrorAnswer(QueryAnswerStatus status, String message) {
		if(status == QueryAnswerStatus.OK) throw new IllegalArgumentException("OK status can't be used as an error status");
		return new RequestResult(new JsonObject().put("type", status.name()).put("message", message));
	}
	
	public static RequestResult buildNewDataAnswer(List<JsonObject> objects) {
		return new RequestResult(new JsonObject().put("type", QueryAnswerStatus.OK.name()).put("data", new JsonArray(objects)));
	}
	
	public static RequestResult buildNewEmptyOkAnswer() {
		return new RequestResult(new JsonObject().put("type", QueryAnswerStatus.OK.name()));
	}
}