package fr.umlv.papayaDB.DataBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vertx.core.json.JsonObject;

public class DataBase {
	
	private final String dBName;
	private final DBManager manager;
	
	/* Optimisation (voire plus tard)*/
	private final Map<String, TreeMap<?, Integer>> indexedCollection = new HashMap<>();
	
	
	public DataBase(String dBName){
		this.dBName = dBName;
		this.manager = new DBManager(dBName);
	}
	
	public void insertObject(JsonObject jsonObject){
		manager.insertObject(jsonObject);
	}
	
	public void deleteObject(String id){
		Optional<Integer> optional = manager.getObjects().keySet().stream()
			.filter(key -> {
				return manager.getObjectByAdress(key).getString("UID").equals(id);
			}).findFirst();
		
		if (optional.isPresent()){
			manager.deleteObject(optional.get());
		}
	}
	
	public void updateObject(String id, JsonObject jsonObject){
		Optional<Integer> optional = manager.getObjects().keySet().stream()
				.filter(key -> {
					return manager.getObjectByAdress(key).getString("UID").equals(id);
				}).findFirst();
			
			if (optional.isPresent()){
				manager.updateObject(optional.get(), jsonObject); 
			}
	}
	
	public List<JsonObject> searchObjects(JsonObject jsonObject) {
		Stream<JsonObject> res = workOnDB(jsonObject);
		return res.collect(Collectors.toList());
	}
	
	
	private Stream<JsonObject> workOnDB(JsonObject query) {
		if(!query.containsKey("type")) throw new Exception("No query type providen"); //ExceptionType ?
		
		String typeString = query.getString("type");
		QueryType type;
		Stream<JsonObject> terminalResult = null;
		Stream<Entry<Integer, Integer>> result = manager.getRecordsMap().entrySet().stream();
		
		try {
			type = QueryType.valueOf(typeString);
		}
		catch(IllegalArgumentException e) {
			throw new Exception("Query type "+typeString+" doesn't exists"); //ExceptionType ?
		}
		
		JsonObject parametersContainer = query.getJsonObject("parameters");
		if(parametersContainer != null) {
			ArrayList<String> parametersNames = new ArrayList<>(parametersContainer.fieldNames());
			parametersNames.sort((parameterName1, parameterName2) -> {
				Optional<QueryParameter> qp1 = QueryParameter.getQueryParameterKey(type, parameterName1);
				Optional<QueryParameter> qp2 = QueryParameter.getQueryParameterKey(type, parameterName2);
				
				//ExceptionType ?
				if(!qp1.isPresent()) throw new Exception("Query parameter "+parameterName1+" doesn't exists or isn't correct with query type "+type.name());
				if(!qp2.isPresent()) throw new Exception("Query parameter "+parameterName2+" doesn't exists or isn't correct with query type "+type.name());
				
				QueryParameter q1 = qp1.get();
				QueryParameter q2 = qp2.get();
				
				if(!q1.isTerminalModifier()) return -1;
				if(!q2.isTerminalModifier()) return 1;
				
				return 0;
			});
			
			boolean reachedTerminalOperations = false;
			for(String parameter : parametersContainer.fieldNames()) {
				JsonObject parameters = parametersContainer.getJsonObject(parameter);
				Optional<QueryParameter> queryParameter = QueryParameter.getQueryParameterKey(QueryType.GET, parameter);
				if(queryParameter.isPresent()) {
					QueryParameter qp = queryParameter.get();
					
					// Si on a atteint les modificateurs terminaux
					if(qp.isTerminalModifier()) {
						if(!reachedTerminalOperations) {
							reachedTerminalOperations = true;
							terminalResult = convertAddressStreamToTerminal(result);
						}
						terminalResult = qp.processTerminalOperation(parameters, terminalResult, manager);
					}
					else {
						result = qp.processQueryParameters(parameters, result, manager);
					}
				}
			}
		}
		
		if(terminalResult == null) {
			terminalResult = convertAddressStreamToTerminal(result);
		}
		
		return terminalResult;
}

}
