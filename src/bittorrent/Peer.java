package bittorrent;

public interface Peer {
	 void choke();
	 void unchoke();
	 boolean isChoked();
	 void setLastUpdated(long l);
	 long getLastUpdated();
}
