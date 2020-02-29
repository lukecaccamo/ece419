package client;

import java.math.BigInteger;
import java.net.Socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.KVAdminMessage;
import shared.messages.KVMessage.StatusType;
import shared.communications.KVCommModule;
import shared.messages.KVSimpleMessage;
import shared.metadata.*;

public class KVStore implements KVCommInterface {

	private String serverAddress;
	private int serverPort;
	private KVCommModule communications;
	private MetaData metaData; // this client's cached metadata
	private ObjectMapper om;

	private static final int MAX_KEY = 20;
	private static final int MAX_VALUE = 122880;
	/**
	 * Initialize KVStore with address and port of KVServer
	 * @param address the address of the KVServer
	 * @param port the port of the KVServer
	 */
	public KVStore(String address, int port) {
		this.serverAddress = address;
		this.serverPort = port;
		this.metaData = null;
		this.om = new ObjectMapper();
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

		if (key.length() > MAX_KEY || key.isEmpty())
			return new KVSimpleMessage(StatusType.PUT_ERROR, "Key cannot be greater than 20 bytes or empty", null);

		if (key.contains(" "))
			return new KVSimpleMessage(StatusType.PUT_ERROR, "Key cannot contain spaces", null);

		if (value.length() > MAX_VALUE)
			return new KVSimpleMessage(StatusType.PUT_ERROR, "Value cannot be greater than 120 kilobytes", null);

		// get correct server, connect to it
		if (this.metaData != null){
			BigInteger keyHash = Hash.MD5_BI(key);
			ServerData responsible = this.metaData.serverLookup(keyHash);
			disconnect();
			this.serverAddress = responsible.getHost();
			this.serverPort = responsible.getPort();
			connect();
		}

		this.communications.sendKVMessage(StatusType.PUT, key, value);

		KVSimpleMessage returnMsg = this.communications.receiveKVMessage();
		if (returnMsg.getStatus() == StatusType.SERVER_NOT_RESPONSIBLE){
			try {
				this.metaData = this.om.readValue(returnMsg.getValue(), MetaData.class);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			// set value to say metadata instead of the entire serialization
			// returnMsg.setValue();
		}
		return returnMsg;
	}

	@Override
	public KVSimpleMessage get(String key) throws Exception {

		if (key.length() > MAX_KEY || key.isEmpty())
			return new KVSimpleMessage(StatusType.GET_ERROR, "Key cannot be greater than 20 bytes or empty", null);

		if (key.contains(" "))
			return new KVSimpleMessage(StatusType.GET_ERROR, "Key cannot contain spaces", null);

		// get correct server, connect to it
		if (this.metaData != null){
			BigInteger keyHash = Hash.MD5_BI(key);
			ServerData responsible = this.metaData.serverLookup(keyHash);
			disconnect();
			this.serverAddress = responsible.getHost();
			this.serverPort = responsible.getPort();
			connect();
		}

		this.communications.sendKVMessage(StatusType.GET, key, null);

		KVSimpleMessage returnMsg = this.communications.receiveKVMessage();
		if (returnMsg.getStatus() == StatusType.SERVER_NOT_RESPONSIBLE){
			try {
				this.metaData = this.om.readValue(returnMsg.getValue(), MetaData.class);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			// set value to say metadata instead of the entire serialization
			// returnMsg.setValue();
		}
		return returnMsg;
	}

	// Temporary for testing purposes
	public void sendAdmin(KVAdminMessage msg) throws Exception {

		this.communications.sendKVAdminMessage(msg);
	}

}
