package fr.umlv.papayaDB.DataBase;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class DBManager {
	private FileChannel fileChannel;
	private MappedByteBuffer fileBuffer;
	private HashMap<Integer, ArrayList<Integer>> holes = new HashMap<>(); // Map-inverse
	private HashMap<Integer, Integer> adresses = new HashMap<>();
	private final RandomAccessFile randomAccessFile;
	private int numberOfObjects; //Only used in initialization loop

	public DBManager(String fileName) throws IOException {
		Path path = Paths.get(fileName + ".papayadb");
		if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
			Files.createFile(path);
			numberOfObjects = -1;
		}

		randomAccessFile = new RandomAccessFile(path.toFile(), "rw");
		this.fileChannel = randomAccessFile.getChannel();

		if (randomAccessFile.length() == 0) {
			randomAccessFile.setLength(1);
			fileChannel.map(MapMode.READ_WRITE, 0, Integer.BYTES).putInt(0);
		}

		// Read number of elements
		fileBuffer = fileChannel.map(MapMode.READ_WRITE, 0, fileChannel.size());

		if (numberOfObjects == -1) {
			numberOfObjects = 0;
			int oldPos = fileBuffer.position();
			fileBuffer.rewind();
			fileBuffer.putInt(0);
			fileBuffer.position(oldPos);
		} else {
			fileBuffer.rewind();
			numberOfObjects = fileBuffer.getInt();
		}
		fileBuffer.rewind();

		loadObjects();
	}

	private void loadObjects() throws IOException {
		long fileSize = randomAccessFile.length();

		if (fileSize == 0) {
			return;
		}

		fileBuffer.rewind();
		fileBuffer.getInt();
		try {
			for (int i = 0; i < numberOfObjects;) {

				int objectSize = fileBuffer.getInt();
				if (objectSize == -1) {
					int nextObjectPositionInChunks = fileBuffer.getInt();
					// NotifySpace
					fileBuffer.position(fileBuffer.position() + nextObjectPositionInChunks * 64 - (Integer.BYTES * 2));
					continue;
				}
				int objectPosition = fileBuffer.position() - Integer.BYTES;
				System.out.println("Loading object, chunk size "
						+ (int) Math.ceil((double) (objectSize + Integer.BYTES) / (double) 64) * 64 + " bytes at pos "
						+ objectPosition);
				adresses.put(objectPosition, objectSize);

				fileBuffer.position(
						objectPosition + (int) Math.ceil((double) (objectSize + Integer.BYTES) / (double) 64) * 64);
				i++;
			}
		} catch (BufferUnderflowException e) {
			e.printStackTrace();
		}
	}

	public int insertObject(JsonObject jsonObject) {
		Buffer buffer = Buffer.buffer();
		// jsonObject.put("UID",UUID.randomUUID().toString());
		buffer.appendBytes("UID".getBytes());
		buffer.appendBytes(UUID.randomUUID().toString().getBytes());
		buffer.appendBytes(jsonObject.toString().getBytes());

		int size = buffer.length();
		int fileSize = (int) Math.ceil((double) (size + Integer.BYTES) / (double) 64) * 64;

		try {
			int adress = findPosition(fileSize);
			fileBuffer.position(adress);
			fileBuffer.putInt(size);
			fileBuffer.put(buffer.getBytes());
			return 0;
		} catch (IOException e) {
			return -1;
		}
	}

	public HashMap<Integer, Integer> getObjects() {
		return new HashMap<>(adresses);
	}

	public JsonObject getObject(int address) {
		int realSize = adresses.getOrDefault(address, -1);
		if (realSize == -1)
			return null; // Y a pas

		byte[] byteArr = ByteBuffer.allocate(realSize).array();
		fileBuffer.position(address);

		fileBuffer.getInt();
		fileBuffer.get(byteArr, 0, realSize);
		Buffer b = Buffer.buffer(byteArr);
		return b.toJsonObject();
	}

	private int findPosition(int size) throws IOException { // to reshape : too
															// long
		int sizeReal = size / 64;

		int insertionPosition = -1;

		List<Entry<Integer, ArrayList<Integer>>> freeSpaces = holes.entrySet().stream()
				.filter(entry -> entry.getKey() >= sizeReal).map(entry -> {
					entry.getValue().sort((x, y) -> {
						return x - y;
					});
					return entry;
				}).collect(Collectors.toList());

		for (Entry<Integer, ArrayList<Integer>> space : freeSpaces) {
			ArrayList<Integer> holes = space.getValue();
			int spaceSize = space.getKey();

			if (holes.size() == 0) {
				continue;
			}

			if (spaceSize == sizeReal) {
				insertionPosition = holes.remove(0);
			} else {
				insertionPosition = holes.remove(0);
				int newHolePosition = insertionPosition + size;
				int newHoleSize = spaceSize - sizeReal;
				// NotifySpace
				MappedByteBuffer buffer = fileChannel.map(MapMode.READ_WRITE, newHolePosition, Integer.BYTES * 2);
				buffer.putInt(-1);
				buffer.putInt(newHoleSize);
			}
			break;
		}

		if (insertionPosition == -1) {
			insertionPosition = (int) fileChannel.size();
			randomAccessFile.setLength(insertionPosition + size);
			fileBuffer = fileChannel.map(MapMode.READ_WRITE, 0, fileChannel.size());
		}

		return insertionPosition;
	}

	public void deleteObject(int address) {
		int sizefile = adresses.getOrDefault(address, -1);
		if (sizefile == -1)
			return;

		int size = (int) Math.ceil((double) (sizefile) / (double) 64) * 64;

		fileBuffer.position(address);
		for (int i = 0; i < size; i++) {
			fileBuffer.put((byte) 0);
		}

		// NotifySpace

		MappedByteBuffer buffer;
		try {
			buffer = fileChannel.map(MapMode.READ_WRITE, address, Integer.BYTES * 2);
			buffer.putInt(-1);
		} catch (IOException e) {
			e.printStackTrace();
		}

		numberOfObjects = numberOfObjects - 1;
		int temppos = fileBuffer.position();
		fileBuffer.rewind();
		fileBuffer.putInt(numberOfObjects - 1);
		fileBuffer.position(temppos);
	}

	public int updateObject(int oldAddress, JsonObject newObject) {
		int size = adresses.getOrDefault(oldAddress, -1);
		if (size == -1) {
			return -1;
		}
		byte[] bytes = newObject.toString().getBytes();

		int oldSize = ((int) Math.ceil((double) (size) / (double) 64) * 64) / 64;
		int newSize = ((int) Math.ceil((double) (bytes.length + Integer.BYTES) / (double) 64) * 64) / 64;

		//MEME TAILLE : On ecrase
		if (oldSize == newSize) {
			fileBuffer.position(oldAddress);
			fileBuffer.putInt(bytes.length);
			fileBuffer.put(bytes);
			adresses.put(oldAddress, bytes.length);
			return oldAddress;
		}
		//Trop de place : On ecrase puis on indique que c'est libre
		if (oldSize > newSize) {
			int delta = oldSize - newSize;
			fileBuffer.position(oldAddress);
			fileBuffer.putInt(bytes.length);
			fileBuffer.put(bytes);
			adresses.put(oldAddress, bytes.length);

			fileBuffer.position(oldAddress + delta * 64);
			fileBuffer.putInt(-1);
			fileBuffer.putInt(delta);
			// REGISTER
			return oldAddress;
		}
		
		//Pas assez de place : On indique que c'est vide puis on cherche un espace pour stocker
		if (oldSize < newSize) {
			fileBuffer.position(oldAddress);
			int newPos;
			try {
				newPos = findPosition(newSize * 64);
			} catch (IOException e) {
				// FAIL : No place  for you
				return oldAddress;
			}

			fileBuffer.position(newPos);
			fileBuffer.putInt(bytes.length);
			fileBuffer.put(bytes);
			fileBuffer.position(oldAddress);
			fileBuffer.putInt(-1);
			fileBuffer.putInt(oldSize);
			// NotifySpace
			adresses.put(newPos, bytes.length);
			return newPos;
		}
		return -1;
	}

	/*
	 * private void notifySpaces(Integer position, Integer size) { 
	 * //Notify when a space is liberted 
	 * }
	 */

}
