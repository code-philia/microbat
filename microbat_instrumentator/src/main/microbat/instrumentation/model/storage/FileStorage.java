package microbat.instrumentation.model.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;

public class FileStorage implements Storage {

	private String fileName;
	
	private static final String CLASS_DELIM_STRING = ";";
	public FileStorage(String fileName) {
		this.fileName = fileName;
	}
	
	@Override
	public void store(HashSet<Storable> objects) {
		File file = new File(fileName);
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			for (Storable storable: objects) {
				fileOutputStream.write(storable.getClass().getName().getBytes());
				fileOutputStream.write(CLASS_DELIM_STRING.getBytes());
				
				fileOutputStream.write(storable.store().getBytes());
				fileOutputStream.write(Storage.itemDelimString.getBytes());
			}
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

}
