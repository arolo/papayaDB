package fr.umlv.papayaDB.DataBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.umlv.papayaDB.Request.RequestParameter;
import fr.umlv.papayaDB.Request.RequestType;
import io.vertx.core.json.JsonObject;

public class DataBase {
	
	private final String dBName;
	private final DBManager manager;
		
	public DataBase(String dBName) throws IOException{
		this.dBName = dBName;
		this.manager = new DBManager(dBName);
	}
	
	public void insertObject(JsonObject jsonObject){
		manager.insertObject(jsonObject);
	}
	
	public void deleteObject(String id){
		Optional<Integer> optional = manager.getObjects().keySet().stream()
			.filter(key -> {
				return manager.getObject(key).getString("UID").equals(id);
			})
			.findFirst();
		
		if (optional.isPresent()){
			manager.deleteObject(optional.get());
		}
	}
	
	public void updateObject(String id, JsonObject jsonObject){
		Optional<Integer> optional = manager.getObjects().keySet().stream()
				.filter(key -> {
					return manager.getObject(key).getString("UID").equals(id);
				})
				.findFirst();
			
			if (optional.isPresent()){
				manager.updateObject(optional.get(), jsonObject); 
			}
	}
	
	public List<JsonObject> searchObjects(JsonObject jsonObject) {
		Stream<JsonObject> res = workOnDB(jsonObject);
		return res.collect(Collectors.toList());
	}
	
	
	private Stream<JsonObject> workOnDB(JsonObject query) { //TO REWORK
		String typeString = query.getString("type");
		RequestType type;
		Stream<JsonObject> terminalResult = null;
		Stream<Entry<Integer, Integer>> result = manager.getObjects().entrySet().stream();
		
		try {
			type = RequestType.valueOf(typeString);
		}
		catch(IllegalArgumentException e) {
			throw new IllegalArgumentException(); //ExceptionType ?
		}
		
		JsonObject parameters = query.getJsonObject("parameters");
		if(parameters != null) {
			ArrayList<String> parametersNames = new ArrayList<>(parameters.fieldNames());
			parametersNames.sort((x, y) -> {
				
				if(!RequestParameter.getQueryParameterKey(type, x).get().isTerminalModifier()) return -1;
				if(!RequestParameter.getQueryParameterKey(type, y).get().isTerminalModifier()) return 1;
				
				return 0;
			});
			
			boolean reachedTerminalOperations = false;
			for(String parameter : parameters.fieldNames()) {
				JsonObject subparameters = parameters.getJsonObject(parameter);
				Optional<RequestParameter> requestParameter = RequestParameter.getQueryParameterKey(RequestType.GET, parameter);
				if(requestParameter.isPresent()) {
					RequestParameter qp = requestParameter.get();
					
					if(qp.isTerminalModifier()) {
						if(!reachedTerminalOperations) {
							reachedTerminalOperations = true;
							terminalResult = entryConvertToJsonObject(result);
						}
						terminalResult = qp.processTerminalOperation(subparameters, terminalResult, manager);
					}
					else {
						result = qp.processQueryParameters(subparameters, result, manager);
					}
				}
			}
		}
		
		if(terminalResult == null) {
			terminalResult = entryConvertToJsonObject(result);
		}
		
		return terminalResult;
}
	
	private Stream<JsonObject> entryConvertToJsonObject(Stream<Entry<Integer, Integer>> elements) {
		if(elements == null) return Stream.empty();
		
		return elements.map(entry -> {
			return manager.getObject(entry.getKey());
		});
	}	

}
