package bittorrent;

public interface Errors {
	static final String INVALID_ARGS = "Error: Invalid number of arguments\n"
			+ "\nUsage: java cp . RUBTClient <load_file> <storage_file>";
	static final String NULL_FILENAME = "Error: Please provide a valid file path";
	static final String FILE_DOESNT_EXIST  = "Error: The file %s doesn't exist. "
			+ "\nPlease provide a valid file path name";
	static final String CORRUPT_FILE = "Error: the file %s is corrupted.";
	static final String INVALID_URL = "Error: An invalid url was formed. "
			+ "\nCheck the contents of the file %s";
	static final String GET_FAILED = "Error: The program failed to properly "
			+ "execute HTTP GET request";
}
