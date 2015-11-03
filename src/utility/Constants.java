package utility;

import java.nio.ByteBuffer;

public interface Constants {

	static final byte[] BT_PROTOCOL = { 'B', 'i', 't', 'T', 'o',
		'r', 'r', 'e', 'n', 't', ' ', 'p', 'r', 'o', 't', 'o', 'c', 'o', 'l' 
	};
	static final ByteBuffer KEY_FAILURE_REASON = ByteBuffer.wrap( new byte[] { 
			'f', 'a', 'i', 'l', 'u', 'r', 'e', ' ', 'r', 'e', 'a', 's', 'o', 'n'
	});
	static final ByteBuffer KEY_WARNING_MESSAGE = ByteBuffer.wrap(new byte[] { 
		'w', 'a', 'r', 'n', 'i', 'n', 'g', ' ', 'm','e', 's', 's', 'a', 'g', 'e'
	});
	static final ByteBuffer KEY_COMPLETE = ByteBuffer.wrap(new byte[] {
			'c', 'o', 'm', 'p', 'l', 'e', 't', 'e' 
	});
	static final ByteBuffer KEY_INCOMPLETE = ByteBuffer.wrap(new byte[] {
			'i', 'n', 'c', 'o', 'm', 'p', 'l', 'e', 't', 'e'
	});
	static final ByteBuffer KEY_MIN_INTERVAL = ByteBuffer.wrap(new byte[] { 
			'm', 'i', 'n', ' ', 'i', 'n', 't', 'e', 'r', 'v', 'a', 'l' 
	});
	static final ByteBuffer KEY_DOWNLOADED = ByteBuffer.wrap(new byte[] {
			'd', 'o', 'w', 'n', 'l', 'o', 'a', 'd', 'e', 'd'
	});
	static final ByteBuffer KEY_INTERVAL = ByteBuffer.wrap(new byte[] {
			'i', 'n', 't', 'e', 'r', 'v', 'a', 'l' 
	});
	static final ByteBuffer KEY_PEERS = ByteBuffer.wrap(new byte[] {
			'p', 'e', 'e', 'r', 's' 
	});
	static final ByteBuffer KEY_PORT = ByteBuffer.wrap(new byte[] {
			'p', 'o', 'r', 't' 
	});
	static final ByteBuffer KEY_IP = ByteBuffer.wrap(new byte[] { 'i', 'p' });
	static final ByteBuffer KEY_PEER_ID = ByteBuffer.wrap(new byte[] {
			'p', 'e', 'e', 'r', ' ', 'i', 'd' 
	});
	
	static final String INVALID_ARGS = "Error: Invalid number of arguments\n"
			+ "\nUsage: java cp . RUBTClient <load_file> <storage_file>";
	static final String NULL_FILENAME = "Error: Please provide a valid file path";
	static final String FILE_NOT_FOUND  = "Error: The file %s doesn't exist. "
			+ "\nPlease provide a valid file path name";
	static final String CORRUPT_FILE = "Error: the file %s is corrupted.";
	static final String INVALID_URL = "Error: An invalid url was formed. "
			+ "\nCheck the contents of the file %s";
	static final String GET_FAILED = "Error: The program failed to properly "
			+ "execute HTTP GET request";
	static final String NO_PEERS_FOUND = "Warning: No peers found in the response. Terminating ..";
	
	/*
	 * Made an enum to make life easier. 
	 */
	public enum MessageType{
		
		Keep_Alive(-1),
		Choke(0),
		Un_Choke(1), 
		Interested(2), 
		Not_Interested(3), 
		Have(4), 
		BitField(5), 
		Request(6), 
		Piece(7), 
		Cancel(8);
		
		public final byte id;
		public byte lenPref;
		public int pieceIndex;
		public byte[] data;
		public int begin;
		public int length;
		public byte[] block;
		
		/**
		 * @param data the data to set
		 */
		public void setData(byte[] data) {
			this.data = data;
		}
		/**
		 * @param begin the begin to set
		 */
		public void setBegin(int begin) {
			this.begin = begin;
		}
		/**
		 * @param length the length to set
		 */
		public void setLength(int length) {
			this.length = length;
		}
		/**
		 * @param block the block to set
		 */
		public void setBlock(byte[] block) {
			this.block = block;
		}
		public void setPieceIndex(int i){
			this.pieceIndex = i;
		}
		
		MessageType(int id){
			this.id = (byte) id;
			if(id > -1 && id < 4)
				this.lenPref = 1;
			else if(id == 4)
				this.lenPref = 5;
			else if(id == 5)
				this.lenPref = 1;
			else if(id == 6)
				this.lenPref = 13;
			else
				this.lenPref = 0;
		}
		
		@Override
		public String toString(){
			String ret = this.name() + ": " + this.lenPref + " " + this.id;
			if(this.data != null)
				ret += " " + Converter.byteArrToStr(this.data);
			if(this.pieceIndex != 0)
				ret += " " + String.valueOf(this.pieceIndex);
			if(this.begin != 0)
				ret += " " + String.valueOf(this.begin);
			return ret;
		}
		
	}
	
	public enum Event{
		Empty, Started, Stopped, Completed;
		
		@Override
		public String toString(){
			if(this.name().equals(Empty))
				return "";
			else
				return "&event=" + this.name().toLowerCase();
		}
	}
}
