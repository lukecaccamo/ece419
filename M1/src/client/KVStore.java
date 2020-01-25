package client;

import shared.messages.KVMessage.StatusType;
import shared.communications.KVCommModule;
import shared.messages.KVSimpleMessage;

import app_kvClient.KVClient;

public class KVStore implements KVCommInterface {

	private KVCommModule communications;

	/**
	 * Initialize KVStore with address and port of KVServer
	 * @param address the address of the KVServer
	 * @param port the port of the KVServer
	 */
	public KVStore(String address, int port) {
		this.communications = new KVCommModule(address, port);
	}

	@Override
	public void connect() throws Exception {
		this.communications.connect();
	}

	@Override
	public void disconnect() {
		this.communications.disconnect();
	}

	public boolean isRunning() {
		return this.communications.isRunning();
	}

	public void setRunning(boolean run) {
		this.communications.setRunning(run);
	}

	public void addListener(KVClient listener){
		this.communications.addListener(listener);
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
