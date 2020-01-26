package client;

import java.net.Socket;

import shared.messages.KVMessage.StatusType;
import shared.communications.KVCommModule;
import shared.messages.KVSimpleMessage;

public class KVStore implements KVCommInterface {

	private String serverAddress;
	private int serverPort;

	private KVCommModule communications;

	/**
	 * Initialize KVStore with address and port of KVServer
	 * @param address the address of the KVServer
	 * @param port the port of the KVServer
	 */
	public KVStore(String address, int port) {
		this.serverAddress = address;
		this.serverPort = port;
	}

	@Override
	public void connect() throws Exception {
		Socket socket = new Socket(this.serverAddress, this.serverPort);
		this.communications = new KVCommModule(socket, null);
		this.communications.connect();
	}

	@Override
	public void disconnect() {
		this.communications.disconnect();
	}

	public boolean isRunning() {
		return this.communications.isRunning();
	}

	@Override
	public KVSimpleMessage put(String key, String value) throws Exception {
		this.communications.sendKVMessage(StatusType.PUT, key, value);
		return this.communications.receiveKVMessage();
	}

	@Override
	public KVSimpleMessage get(String key) throws Exception {
		this.communications.sendKVMessage(StatusType.GET, key, null);
		return this.communications.receiveKVMessage();
	}
}
