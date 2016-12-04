package fr.umlv.papayaDB.DataBase;

import java.io.IOException;
import java.util.Optional;

import io.vertx.core.json.JsonObject;

public class DataBase {

	@SuppressWarnings("unused") // Not used for now
	private final String dBName; 
	private final DBManager manager;

	public DataBase(String dBName) throws IOException {
		this.dBName = dBName;
		this.manager = new DBManager(dBName);
	}

	public void insertObject(JsonObject jsonObject) {
		manager.insertObject(jsonObject);
	}

	public void deleteObject(String id) {
		Optional<Integer> optional = manager.getObjects().keySet().stream().filter(x -> {
			return manager.getObject(x).getString("UID").equals(id);
		}).findFirst();

		if (optional.isPresent()) {
			manager.deleteObject(optional.get());
		}
	}

	public void updateObject(String id, JsonObject jsonObject) {
		Optional<Integer> optional = manager.getObjects().keySet().stream().filter(x -> {
			return manager.getObject(x).getString("UID").equals(id);
		}).findFirst();

		if (optional.isPresent()) {
			manager.updateObject(optional.get(), jsonObject);
		}
	}

	/*public List<JsonObject> searchObjects(JsonObject jsonObject) {
		return workOnDB(jsonObject).collect(Collectors.toList());
	}*/

	/*private Stream<JsonObject> workOnDB(JsonObject query) { 
		//TO DO	
	}*/
	

}
