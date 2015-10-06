package bittorrent;

public interface Errors {
	static final String INVALID_ARGS = "Error: Invalid number of arguments\n"
			+ "Usage: java cp . RUBTClient <load_file> <storage_file>";
	static final String NULL_FILENAME = "Error: Please provide a valid file path";
	static final String FILE_DOESNT_EXIST  = "Error: The file %s doesn't exist. Please "
			+ "provide a valid file path name";
	static final String BAD_FILE = "Error: The file provided seems corrupted";
	static final String CORRUPT_FILE = "Error: the file %s is corrupted.";
	
}
