package client;

import java.math.BigInteger;
import java.net.Socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.KVAdminMessage;
import shared.messages.KVMessage.StatusType;
import shared.communications.KVCommModule;
import shared.messages.KVSimpleMessage;
import shared.hashring.*;

import ecs.IECSNode;

public class KVStore implements KVCommInterface {

	private String serverAddress;
	private int serverPort;
	private KVCommModule communications;
	private HashRing metaData; // this client's cached metadata
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
		StatusType returnMsgStatus = StatusType.SERVER_NOT_RESPONSIBLE;
		KVSimpleMessage returnMsg = null;
		while(returnMsgStatus == StatusType.SERVER_NOT_RESPONSIBLE) {

			if (key.length() > MAX_KEY || key.isEmpty())
				return new KVSimpleMessage(StatusType.PUT_ERROR, "Key cannot be greater than 20 bytes or empty", null);

			if (key.contains(" "))
				return new KVSimpleMessage(StatusType.PUT_ERROR, "Key cannot contain spaces", null);

			if (value.length() > MAX_VALUE)
				return new KVSimpleMessage(StatusType.PUT_ERROR, "Value cannot be greater than 120 kilobytes", null);

			// get correct server, connect to it
			if (this.metaData != null) {
				System.out.println("Reconnect");
				String keyHash = Hash.MD5(key);
				IECSNode responsible = this.metaData.serverLookup(keyHash);
				disconnect();
				this.serverAddress = responsible.getNodeHost();
				this.serverPort = responsible.getNodePort();
				connect();
			}

			this.communications.sendKVMessage(StatusType.PUT, key, value);

			returnMsg = this.communications.receiveKVMessage();
			returnMsgStatus = returnMsg.getStatus();
			System.out.println(returnMsgStatus);
			if (returnMsgStatus == StatusType.SERVER_NOT_RESPONSIBLE) {
				try {
					this.metaData = this.om.readValue(returnMsg.getValue(), HashRing.class);
					System.out.println("new metadata");
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
				// set value to say metadata instead of the entire serialization
				returnMsg.setValue("new metadata received");
			}
		}
		return returnMsg;
	}

	@Override
	public KVSimpleMessage get(String key) throws Exception {
		StatusType returnMsgStatus = StatusType.SERVER_NOT_RESPONSIBLE;
		KVSimpleMessage returnMsg = null;
		while(returnMsgStatus == StatusType.SERVER_NOT_RESPONSIBLE) {
			if (key.length() > MAX_KEY || key.isEmpty())
				return new KVSimpleMessage(StatusType.GET_ERROR, "Key cannot be greater than 20 bytes or empty", null);

			if (key.contains(" "))
				return new KVSimpleMessage(StatusType.GET_ERROR, "Key cannot contain spaces", null);

			// get correct server, connect to it
			if (this.metaData != null) {
				String keyHash = Hash.MD5(key);
				IECSNode responsible = this.metaData.serverLookup(keyHash);
				disconnect();
				this.serverAddress = responsible.getNodeHost();
				this.serverPort = responsible.getNodePort();
				connect();
			}

			this.communications.sendKVMessage(StatusType.GET, key, null);

			returnMsg = this.communications.receiveKVMessage();
			returnMsgStatus = returnMsg.getStatus();
			if (returnMsgStatus == StatusType.SERVER_NOT_RESPONSIBLE) {
				try {
					this.metaData = this.om.readValue(returnMsg.getValue(), HashRing.class);
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
				// set value to say metadata instead of the entire serialization
				returnMsg.setValue("new metadata received");
			}
		}
		return returnMsg;
	}

	// Temporary for testing purposes
	public int getServerPort() {
		return this.serverPort;
	}

	public HashRing getMetaData() {
		return this.metaData;
	}
}
