package fr.umlv.papayaDB.DataBase;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.UUID;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class DBManager {
	private FileChannel fileChannel;
	private MappedByteBuffer fileBuffer;
	private HashMap<Integer, Integer> adresses = new HashMap<>(); //Still relevant ?

	private int insertObject(JsonObject jsonObject) {
		Buffer buffer = Buffer.buffer();
		// jsonObject.put("UID",UUID.randomUUID().toString());
		buffer.appendBytes("UID".getBytes());
		buffer.appendBytes(UUID.randomUUID().toString().getBytes());
		buffer.appendBytes(jsonObject.toString().getBytes());

		int size = buffer.length();
		int filesize = (int) Math.ceil((double) (size + Integer.BYTES) / (double) 64) * 64;

		try {
			int adress = findPosition();
			fileBuffer.position(adress);
			fileBuffer.putInt(size);
			fileBuffer.put(buffer.getBytes());

		} catch (IOException e) {
			return -1;
		}
	}

	public HashMap<Integer, Integer> getObjects() {
		return new HashMap<>(adresses);
	}
	
	private int findPostion(){ //TO DO
		return 0;
	}

}
