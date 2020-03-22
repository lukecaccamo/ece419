package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecs.ECSNode;
import shared.communications.KVCommModule;
import shared.hashring.Hash;
import shared.hashring.HashRing;
import shared.messages.KVMessage.StatusType;
import shared.messages.KVSimpleMessage;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;

public class KVStore implements KVCommInterface {

	private String serverAddress;
	private int serverPort;
	private KVCommModule communications;
	private HashRing metadata; // this client's cached metadata
	private ObjectMapper om;
	private String connectedServerHash;
	private String connectedServerName;

	private static final int MAX_KEY = 20;
	private static final int MAX_VALUE = 122880;

	private static final int TIMEOUT = 2;

	/**
	 * Initialize KVStore with address and port of KVServer
	 * @param address the address of the KVServer
	 * @param port the port of the KVServer
	 */
	public KVStore(String address, int port) {
		this.serverAddress = address;
		this.serverPort = port;
		this.metadata = null;
		this.connectedServerHash = null;
		this.om = new ObjectMapper();
	}

	@Override
	public void connect() throws Exception {
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress(this.serverAddress, this.serverPort));
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
			if (this.metadata != null) {
				String keyHash = Hash.MD5(key);

				boolean isCoordinator = this.metadata.inServer(keyHash, connectedServerHash);
				//Check if reconnect is necessary
				if (!isCoordinator) {
					ECSNode responsible = this.metadata.serverLookup(keyHash);
					System.out.println("Reconnecting...");
					disconnect();
					this.serverAddress = responsible.getNodeHost();
					this.serverPort = responsible.getNodePort();
					connect();
					connectedServerHash = responsible.getHashKey();
					connectedServerName = responsible.getNodeName();
				}

				System.out.println("Connected to server: " + connectedServerName + " which serves key: " + key);
			}

			this.communications.sendKVMessage(StatusType.PUT, key, value);

			returnMsg = receiveWithTimeout();
			if(returnMsg == null) {
				throw new TimeoutException();
			}
			returnMsgStatus = returnMsg.getStatus();

			if (returnMsgStatus == StatusType.SERVER_NOT_RESPONSIBLE) {
				try {
					this.metadata = this.om.readValue(returnMsg.getValue(), HashRing.class);
					System.out.println("Received new metadata from server: " + this.metadata.serverLookup(key).getNodeName());
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
				// set value to say metadata instead of the entire serialization
				returnMsg.setValue("New metadata received");
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
			if (this.metadata != null) {
				String keyHash = Hash.MD5(key);

				//Check if reconnect is necessary
				boolean isCoordinatorOrReplica = !connectedServerHash.equals(null)
												&& metadata.isCoordinatorOrReplica(key, connectedServerHash);

				if (!isCoordinatorOrReplica) {
					ECSNode responsible = this.metadata.serverLookup(keyHash);
					System.out.println("Reconnecting...");
					disconnect();
					this.serverAddress = responsible.getNodeHost();
					this.serverPort = responsible.getNodePort();
					connect();
					connectedServerHash = responsible.getHashKey();
					connectedServerName = responsible.getNodeName();
				}

				System.out.println("Connected to server: " + connectedServerName + " which serves key: " + key);
			}

			this.communications.sendKVMessage(StatusType.GET, key, null);

			returnMsg = receiveWithTimeout();
			if(returnMsg == null) {
				throw new TimeoutException();
			}
			returnMsgStatus = returnMsg.getStatus();

			if (returnMsgStatus == StatusType.SERVER_NOT_RESPONSIBLE) {
				try {
					this.metadata = this.om.readValue(returnMsg.getValue(), HashRing.class);
					System.out.println("Received new metadata from server: " + metadata.serverLookup(key).getNodeName());
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
				// set value to say metadata instead of the entire serialization
				returnMsg.setValue("New metadata received");
			}
		}

		return returnMsg;
	}

	// Temporary for testing purposes
	public int getServerPort() {
		return this.serverPort;
	}

	public HashRing getMetaData() {
		return this.metadata;
	}

	private KVSimpleMessage receiveWithTimeout() throws Exception {
		ExecutorService executor = Executors.newCachedThreadPool();
		Callable<Object> task = new Callable<Object>() {
			public Object call() throws IOException {
				return communications.receiveKVMessage();
			}
		};
		Future<Object> future = executor.submit(task);

		try {
			Object result = future.get(TIMEOUT, TimeUnit.SECONDS);
			KVSimpleMessage returnMsg = (KVSimpleMessage) result;
			return returnMsg;
		} catch (TimeoutException e) {
			System.out.println("Socket timed out! Server: " + this.connectedServerName + " is down");
			disconnect();

			//try to connect to another server
			if(this.metadata != null && this.connectedServerHash != null) {
				ECSNode nextServer = this.metadata.getSucc(connectedServerHash);
				this.serverAddress = nextServer.getNodeHost();
				this.serverPort = nextServer.getNodePort();
				connect();
				this.connectedServerHash = nextServer.getHashKey();
				this.connectedServerName = nextServer.getNodeName();
			}
		} finally {
			future.cancel(true);
			executor.shutdownNow();
		}

		return null;
	}
}
