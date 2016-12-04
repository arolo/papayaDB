package fr.umlv.papayaDB.Request;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.umlv.papayaDB.DataBase.DBManager;
import io.vertx.core.json.JsonObject;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;


public abstract class RequestParameter {
	boolean isTerminalModifier = false;
	
	static final Map<RequestType, Map<String, ? super RequestParameter>> parameter = new HashMap<>();
	private static boolean isLoaded;
	
	static {
		loadQueryParameter();
	}
	
	
	private static void loadRParameter() {
		//Création de la map pour chaque type contenu dans les QueryType
		for (RequestType type: RequestType.values()) {
			parameter.put(type, new HashMap<String, RequestParameter>());
		}
		
		Class<?>[] clazz = null;
		try {
			//Récupération des différentes classes du package
			clazz = getClasses("papayaDB.api.queryParameters");
		} catch (ClassNotFoundException | IOException e) {
			
		}
		//pour chaque classe, si elle hérite de celle ci, on invoke sa méthode register pour qu'elle s'ajoute dans la bonne map.
		for (Class<?> c : clazz) {
			if(!c.getName().toString().endsWith("QueryParameter")) {
				Method method = null;
				try {
					method = c.getMethod("registerParameter");
				} catch (NoSuchMethodException | SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				try {
					method.invoke(null);
				} catch (IllegalAccessException e) {
					throw new AssertionError(e);
				} catch (InvocationTargetException e) {
					Throwable cause = e.getCause(); 
					if(cause instanceof RuntimeException) {
						throw (RuntimeException)cause;
					}
					if(cause instanceof Error) { 
						throw (Error)cause;
					}
					throw new UndeclaredThrowableException(cause);
				}
			}
		}
		// Confirmation du chargement des différentes classes.
		isLoaded = true;
	}
	
	private static Class<?>[] getClasses(String packageName) throws ClassNotFoundException, IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		String path = packageName.replace('.', '/');
		Enumeration<URL> resources = classLoader.getResources(path);
		List<Path> dirs = new ArrayList<>();
		while (resources.hasMoreElements()) {
			URL resource = (URL) resources.nextElement();
			// Fix pour les path Windows
			dirs.add(Paths.get(resource.getPath().replaceFirst("^/(.:/)", "$1")));
		}
		ArrayList<Class<?>> classes = new ArrayList<>();
		for (Path directory : dirs) {
			classes.addAll(findClasses(directory, packageName));
		}
		return (Class[]) classes.toArray(new Class[classes.size()]);
	}

    private static List<Class<?>> findClasses(Path directory, String packageName) throws ClassNotFoundException, IOException {
    	/*if (Files.exists(directory)) {
    		return classes;
    	}*/
    	
    	//TODO: voir dans le tp 3 pour faire ça propre
    	Stream<Path> files = Files.list(directory);
    	List<Class<?>> classes = files	.filter(f -> f.getFileName().toString().endsWith(".class"))
    									.map(f -> {
											try {
												return Class.forName(packageName + '.' + f.getFileName().toString().substring(0, f.getFileName().toString().length() - 6));
											} catch (ClassNotFoundException e) {
												return null;
											}
										})
    									.filter(f -> f != null)
    									.collect(Collectors.toList());
    	return classes;
    }

	public static void registerParameter() {
		throw new NotImplementedException();
	}

	public JsonObject valueToJson(JsonObject json, String value){
		throw new NotImplementedException();
	}
	
	public String valueToString(String key, JsonObject value) {
		throw new NotImplementedException();
	}
	
	
	public Stream<Map.Entry<Integer, Integer>> processQueryParameters(JsonObject parameters, Stream<Map.Entry<Integer, Integer>> elements, DBManager manager) {
		throw new UnsupportedOperationException();
	}
	

	public Stream<JsonObject> processTerminalOperation(JsonObject parameters, Stream<JsonObject> elements, DBManager manager) {
		throw new UnsupportedOperationException();
	}
	

	static JsonObject getJsonParameters(JsonObject json) {
		if(json.containsKey("parameters")) {
			return json.getJsonObject("parameters");
		}
		return new JsonObject();
	}

	public static Map<String, ? super RequestParameter> getQueryParametersForType(RequestType type) {
		if(!isLoaded) {
			loadQueryParameter();
		}
		return parameter.get(type);
	}

	public static Optional<RequestParameter> getQueryParameterKey(RequestType type, String key) {
		if(!isLoaded) {
			loadQueryParameter();
		}
		return Optional	.ofNullable((RequestParameter) getQueryParametersForType(type).get(key));
	}
	
	public boolean isTerminalModifier() {
		return isTerminalModifier;
	}
}