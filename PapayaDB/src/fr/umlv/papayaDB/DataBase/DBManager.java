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
	private HashMap<Integer, ArrayList<Integer>> holes = new HashMap<>(); // Map
																			// inverse
	private HashMap<Integer, Integer> adresses = new HashMap<>(); // Still
																	// relevant
																	// ?
	private final RandomAccessFile randomAccessFile;
	private int numberOfObjects; // Not used ?

	public DBManager(String fileName) throws IOException {
		Path path = Paths.get(fileName + ".coll");
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
					// REGISTER
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
		if(realSize == -1) return null; //Y a pas
		
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
				// Register
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
		if(sizefile == -1) return;
		
		int size = (int) Math.ceil((double) (sizefile)/ (double) 64)*64;
		
		fileBuffer.position(address);
		for(int i = 0; i < size; i++) {
			fileBuffer.put((byte)0);
		}
		
		 int[] infos = new int[2];
		// REGISTER 
		
		MappedByteBuffer buffer;
		try {
			buffer = fileChannel.map(MapMode.READ_WRITE, infos[0], Integer.BYTES*2);
			buffer.putInt(-1);
			buffer.putInt(infos[1]);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		numberOfObjects = numberOfObjects-1;
		int temppos = fileBuffer.position();
		fileBuffer.rewind();
		fileBuffer.putInt(numberOfObjects-1);
		fileBuffer.position(temppos);
		}
	
	public int updateObject(int oldAddress, JsonObject newObject)
	{
		int oldSize = adresses.getOrDefault(oldAddress, -1);
		if(oldSize == -1) {
			return -1;
		}
		byte[] bytes = newObject.toString().getBytes();
		
		int oldSizeInChunks = ((int) Math.ceil((double) (oldSize)/ (double) 64)*64)/64;
		int newSizeInChunks = ((int) Math.ceil((double) (bytes.length+Integer.BYTES)/ (double) 64)*64)/64;
		
		if(oldSizeInChunks == newSizeInChunks) {
			fileBuffer.position(oldAddress);
			fileBuffer.putInt(bytes.length);
			fileBuffer.put(bytes);
			adresses.put(oldAddress, bytes.length);
			return oldAddress;
		}
		if(oldSizeInChunks > newSizeInChunks) {
			int chunkSizeDiff = oldSizeInChunks - newSizeInChunks;
			fileBuffer.position(oldAddress);
			fileBuffer.putInt(bytes.length);
			fileBuffer.put(bytes);
			adresses.put(oldAddress, bytes.length);
			
			int holePos = oldAddress + chunkSizeDiff*64;
			fileBuffer.position(holePos);
			fileBuffer.putInt(-1);
			fileBuffer.putInt(chunkSizeDiff);
			//RESGISTER
			return oldAddress;
		}
		if(oldSizeInChunks > newSizeInChunks) {
			fileBuffer.position(oldAddress);
			byte[] oldRecordInBytes = new byte[bytes.length];
			fileBuffer.get(oldRecordInBytes, 0, bytes.length);
			int newPos;
			try {
				for(Entry<Integer, ArrayList<Integer>> entry : holes.entrySet()) {
					System.out.println(entry.getKey()+" => "+entry.getValue());
		}
				newPos = findPosition(newSizeInChunks*64);
				for(Entry<Integer, ArrayList<Integer>> entry : holes.entrySet()) {
					System.out.println(entry.getKey()+" => "+entry.getValue());
		}
			} catch (IOException e) {
				// Pas réussi à modifier (IOException sur findSuitableSize) : on réinscrit l'ancien au même endroit
				fileBuffer.position(oldAddress);
				fileBuffer.putInt(oldRecordInBytes.length);
				fileBuffer.put(oldRecordInBytes);
				return oldAddress;
			}
			
			// Ecriture au nouvel endroit
			fileBuffer.position(newPos);
			fileBuffer.putInt(bytes.length);
			fileBuffer.put(bytes);
			
			// Suppression de l'ancien exemplaire
			fileBuffer.position(oldAddress);
			fileBuffer.putInt(-1);
			fileBuffer.putInt(oldSizeInChunks);
			//REGISTER
			adresses.put(newPos, bytes.length);
			return newPos;
		}
		return -1;
}
	
	//REGISTER
	private int[] registerNewSpace(Integer positionInFile, Integer chunkSize) {		
		int holePosition = positionInFile;
		
		// Essaie de trouver si la section suivante est le début d'un trou ou pas
		int nextSectionPosition = positionInFile+chunkSize*64;
		Entry<Integer, ArrayList<Integer>> nextHoleEntry = holes.entrySet().stream()
																		.filter(currentEntry -> currentEntry.getValue().contains(nextSectionPosition))
																		.findFirst()
																		.orElse(null);
		
		// Si la fin de la section à effacer est bien le début d'un trou, on agrandit le trou
		if(nextHoleEntry != null) {
			int holeSizeInChunks = nextHoleEntry.getKey();
			ArrayList<Integer> holes = nextHoleEntry.getValue();
//			System.out.println("Found a hole section just right after the one to create : "+nextSectionPosition+" in "+holes+" (chunks size "+holeSizeInChunks+")");
			holes.remove((Integer) nextSectionPosition);
			chunkSize += holeSizeInChunks;
		}
		
		// Essaie de trouver la section précédente vide si elle existe
		Entry<Integer, ArrayList<Integer>> previousHoleEntry = holes.entrySet().stream()
																				.filter(currentEntry -> currentEntry.getValue().contains(positionInFile - currentEntry.getKey()*64))
																				.findFirst()
																				.orElse(null);
		// S'il y a bien une entrée avant
		if(previousHoleEntry != null) {
			int holeSizeInChunks = previousHoleEntry.getKey();
			ArrayList<Integer> holes = previousHoleEntry.getValue();
			int previousSectionPosition = positionInFile - holeSizeInChunks*64;
			
			holes.remove((Integer) previousSectionPosition);
			holePosition = previousSectionPosition;
			chunkSize += holeSizeInChunks;
		}
		
		
		
		ArrayList<Integer> emptyChunksOfThisSize = holes.getOrDefault(chunkSize, new ArrayList<>());
		emptyChunksOfThisSize.add(holePosition);
		holes.put(chunkSize, emptyChunksOfThisSize);
		adresses.remove(positionInFile);
		return new int[]{holePosition, chunkSize};
}
	
	

}
