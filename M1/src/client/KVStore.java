package client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;

import shared.communications.KVStoreCommunications;
import shared.messages.KVMessage;
import shared.messages.KVMessage.StatusType;

public class KVStore implements KVCommInterface {
	private boolean running;

	private KVStoreCommunications communications;
	private OutputStream output;
 	private InputStream input;

	private String serverAddress;
	private int serverPort;

	/**
	 * Initialize KVStore with address and port of KVServer
	 * @param address the address of the KVServer
	 * @param port the port of the KVServer
	 */
	public KVStore(String address, int port) {
		serverAddress = address;
		serverPort = port;
	}

	@Override
	public void connect() throws UnknownHostException, IOException {
		communications = new KVStoreCommunications(serverAddress, serverPort);
	}

	@Override
	public void disconnect() {
		communications.closeConnection();
	}

	public boolean isRunning() {
		return communications.isRunning();
	}

	@Override
	public KVMessage put(String key, String value) throws Exception {
		communications.sendKVMessage(StatusType.PUT, key, value);
        return communications.receiveKVMessage();
	}

	@Override
	public KVMessage get(String key) throws Exception {
		communications.sendKVMessage(StatusType.GET, key, null);
        return communications.receiveKVMessage();
	}
}
