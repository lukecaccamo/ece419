package app_kvServer;

import app_kvServer.KVCache.FIFOCache;
import app_kvServer.KVCache.IKVCache;
import app_kvServer.KVCache.LFUCache;
import app_kvServer.KVCache.LRUCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import ecs.ECSNode;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import shared.Prompt;
import shared.communications.KVAdminCommModule;
import shared.communications.KVCommModule;
import shared.exceptions.DeleteException;
import shared.exceptions.GetException;
import shared.exceptions.PutException;
import shared.hashring.Hash;
import shared.hashring.HashRing;
import shared.messages.KVMessage;
import shared.messages.KVSimpleMessage;
import shared.replication.Replicator;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;

public class KVServer implements IKVServer, Runnable {
	private static Logger logger = Logger.getRootLogger();
	public final Prompt prompt;

	private boolean running;
	private String host;
	private int port;
	private int cacheSize;
	private String cacheStrategy;
	private ServerSocket serverSocket;
	private IKVCache cache;
	private KVDatabase database;
	private ObjectMapper om;
	private static final String DELETE_VAL = "null";

	private String zkHost;
	private int zkPort;
	private String serverName;
	private String serverHash;
	private ServerStateType serverStateType;
	private HashRing metadata;
	private boolean writeLock;

	private KVAdminCommModule adminConnection;
	private KVCommModule serverConnection;
	private Replicator replicator;

	/**
	 * Start KV Server at given port
	 * 
	 * @param port          given port for storage server to operate
	 * @param cacheSize     specifies how many key-value pairs the server is allowed
	 *                      to keep in-memory
	 * @param cacheStrategy specifies the cache replacement strategy in case the
	 *                      cache is full and there is a GET- or PUT-request on a
	 *                      key that is currently not contained in the cache.
	 *                      Options are "FIFO", "LRU", and "LFU".
	 */
	public KVServer(int port, int cacheSize, String cacheStrategy) {
		this.prompt = new Prompt("KVServer");

		this.host = "";
		this.port = port;
		this.cacheSize = cacheSize;
		this.cacheStrategy = cacheStrategy;

		try {
			InetAddress localhost = InetAddress.getLocalHost();
			this.host = (localhost.getHostAddress()).trim();
		} catch (UnknownHostException e) {
			this.logger.error(e);
			e.printStackTrace();
		}

		this.om = new ObjectMapper();

		this.database = new KVDatabase(this.port);

		this.replicator = new Replicator(this);

		initializeServer(null, null, cacheSize, CacheStrategy.valueOf(cacheStrategy));
	}

	/**
	 * Start KV Server at given port w/ ZooKeeper
	 * 
	 * @param serverName given serverName for this server
	 * @param port       given port for storage server to operate
	 * @param zkHost     given IP for ZooKeeper to operate
	 * @param zkPort     given port for ZooKeeper to operate
	 */
	public KVServer(String serverName, int port, String zkHost, int zkPort) {
		this.prompt = new Prompt(serverName);

		this.serverName = serverName;
		this.host = "";
		this.port = port;
		this.zkHost = zkHost;
		this.zkPort = zkPort;

		try {
			InetAddress localhost = InetAddress.getLocalHost();
			this.host = (localhost.getHostAddress()).trim();
		} catch (UnknownHostException e) {
			this.logger.error(e);
			e.printStackTrace();
		}

		this.om = new ObjectMapper();

		this.database = new KVDatabase(this.port);

		this.serverStateType = ServerStateType.STOPPED;
		this.writeLock = false;

		this.adminConnection = new KVAdminCommModule(serverName, zkHost, zkPort, this);
		this.replicator = new Replicator(this);

		new Thread(this.adminConnection).start();
	}

	public void initializeServer(String key, HashRing metadata, int cacheSize, CacheStrategy cacheStrategy) {
		this.serverHash = key;
		this.metadata = metadata;
		this.serverStateType = ServerStateType.STOPPED;
		this.writeLock = false;

		switch (cacheStrategy) {
			case LRU:
				this.cache = new LRUCache(this.cacheSize);
			case FIFO:
				this.cache = new FIFOCache(this.cacheSize);
			case LFU:
				this.cache = new LFUCache(this.cacheSize);
			case None:
				break;
			default:
				this.prompt.print("Error! Invalid <cacheStrategy>!");
				System.exit(1);
				break;
		}
		this.cacheSize = cacheSize;
		this.cacheStrategy = cacheStrategy.toString();

		if (!this.running) {
			this.prompt.print("Running!");
			new Thread(this).start();
		}
	}

	public boolean replicate(String key, String value) {
		return replicator.replicate(key, value);
	}

	public ServerStateType getServerState() {
		return this.serverStateType;
	}

	public void start() {
		this.serverStateType = ServerStateType.STARTED;
		replicator.connect();
	}

	public void stop() {
		this.serverStateType = ServerStateType.STOPPED;
	}

	public void shutDown() {
		this.serverStateType = ServerStateType.SHUT_DOWN;
		close();
	}

	public String getServerHash() {
		return this.serverHash;
	}

	public void setServerHash(String serverHash) {
		this.serverHash = serverHash;
	}

	public HashRing getMetaData() {
		return this.metadata;
	}

	public void setMetaData(HashRing metadata) {
		this.metadata = metadata;
		replicator.connect();
	}

	public void moveData(String[] range, String serverKey) throws Exception {
		HashMap<String, String> movingData = this.database.moveData(range);
		String dataJSON = "";
		try {
			dataJSON = om.writeValueAsString(movingData);
		} catch (JsonProcessingException e) {
			this.logger.error(e);
			e.printStackTrace();
		}

		ECSNode dest = this.metadata.getServer(serverKey);
		// connect to other server, send dataJSON as value
		Socket socket = new Socket(dest.getNodeHost(), dest.getNodePort());
		this.serverConnection = new KVCommModule(socket, null);
		this.serverConnection.connect();

		this.serverConnection.sendKVMessage(KVMessage.StatusType.MOVE_VALUES, serverKey, dataJSON);

		KVSimpleMessage returnMsg = this.serverConnection.receiveKVMessage();
		if (returnMsg.getStatus() == KVMessage.StatusType.MOVE_VALUES_DONE) {
			//this.database.deleteMovedData(movingData);
			//this.cache.clear();
			this.logger.info("Move data done");
		}
		// receive msg from server that its done, delete data in movingData
		this.serverConnection.disconnect();
	}

	public void receiveData(String newDataJSON) {
		HashMap<String, String> newData = null;
		try {
			newData = om.readValue(newDataJSON, HashMap.class);
		} catch (JsonProcessingException e) {
			this.logger.error(e);
			e.printStackTrace();
		}
		this.database.receiveData(newData);
	}

	public void lockWrite() {
		this.writeLock = true;
	}

	public void unlockWrite() {
		this.writeLock = false;
	}

	public boolean isWriterLocked() {
		return this.writeLock;
	}

	public boolean isCoordinator(String key) {
		String keyHash = Hash.MD5(key);
		return this.metadata.inServer(keyHash, this.serverHash);
	}

	public boolean isCoordinatorOrReplica(String key) {
		return this.metadata.isCoordinatorOrReplica(key, this.serverHash);
	}

	@Override
	public int getPort() {
		int port = 0;

		if (this.serverSocket != null)
			port = this.serverSocket.getLocalPort();

		return port;
	}

	@Override
	public String getHostname() {
		String host = null;

		if (this.serverSocket != null)
			host = serverSocket.getInetAddress().getHostName();

		return host;
	}

	@Override
	public CacheStrategy getCacheStrategy() {
		return CacheStrategy.valueOf(this.cacheStrategy);
	}

	@Override
	public int getCacheSize() {
		return this.cacheSize;
	}

	@Override
	public boolean inStorage(String key) {
		return this.database.inStorage(key);
	}

	@Override
	public boolean inCache(String key) {
		if (getCacheStrategy() == CacheStrategy.None)
			return false;

		return this.cache.inCache(key);
	}

	@Override
	public String getKV(String key) throws Exception {
		try {
			if (getCacheStrategy() != CacheStrategy.None) {
				String cacheReturn = this.cache.get(key);
				if (cacheReturn != null)
					return cacheReturn;
			}

			String dbReturn = this.database.get(key);
			if (dbReturn != null && getCacheStrategy() != CacheStrategy.None)
				this.cache.put(key, dbReturn);

			return dbReturn;

		} catch (Exception e) {
			e.printStackTrace();
			throw new GetException(key, "GET failed unexpectedly!");
		}
	}

	@Override
	public void putKV(String key, String value) throws Exception {
		try {

			if (getCacheStrategy() != CacheStrategy.None)
				this.cache.put(key, value);

			if (value.equals(DELETE_VAL)) {
				this.database.delete(key);
			} else {
				this.database.put(key, value);
			}

		} catch (Exception e) {
			e.printStackTrace();
			if (value.equals(DELETE_VAL)) {
				throw new DeleteException(key, "DELETE failed unexpectedly!");
			} else {
				throw new PutException(key, value, "PUT failed unexpectedly!");
			}
		}
	}

	@Override
	public void clearCache() {
		if (getCacheStrategy() != CacheStrategy.None)
			this.cache.clear();
	}

	@Override
	public void clearStorage() {
		if (getCacheStrategy() != CacheStrategy.None)
			this.cache.clear();
		this.database.clear();
	}

	@Override
	public void run() {
		initializeConnection();
		if (this.serverSocket != null) {
			try {
				while (this.running) {
					Socket client = this.serverSocket.accept();
					KVCommModule connection = new KVCommModule(client, this);
					new Thread(connection).start();
					this.logger.info(
							"Connected to " + client.getInetAddress().getHostName() + " on port " + client.getPort());
				}
				this.serverSocket.close();
				this.serverSocket = null;
			} catch (IOException e) {
				this.logger.error("Error! " + "Unable to establish connection. \n", e);
			}
		}
		this.logger.info("Server stopped.");

		if (this.adminConnection != null)
			this.adminConnection.close();
			System.exit(0);
	}

	@Override
	public void kill() {
		try {
			if (this.serverSocket != null && !this.serverSocket.isClosed())
				this.serverSocket.close();
		} catch (IOException e) {
			this.logger.error("Error! " + "Unable to close socket on port: " + this.getPort(), e);
		}
	}

	@Override
	public void close() {
		this.running = false;

		if (this.adminConnection != null)
			this.adminConnection.close();
			System.exit(0);
	}

	/**
	 * Main entry point for the echo server application.
	 * 
	 * @param args contains the port number at args[0].
	 */
	public static void main(String[] args) {
		Prompt prompt = new Prompt("KVServer");
		try {
			new LogSetup("logs/server.log", Level.INFO);
			if (args.length == 3) {
				int port = Integer.parseInt(args[0]);
				int cacheSize = Integer.parseInt(args[1]);
				String cacheStrategy = args[2];

				KVServer server = new KVServer(port, cacheSize, cacheStrategy);
			} else if (args.length == 4) {
				String serverName = args[0];
				int port = Integer.parseInt(args[1]);
				String zkHost = args[2];
				int zkPort = Integer.parseInt(args[3]);

				KVServer server = new KVServer(serverName, port, zkHost, zkPort);
			} else {
				prompt.printError("Error! Invalid number of arguments!");
				prompt.printError("Usage: Server <port> <cacheSize> <cacheStrategy>");
			}
		} catch (IOException e) {
			prompt.printError("Error! Unable to initialize logger!");
			e.printStackTrace();
			System.exit(1);
		} catch (NumberFormatException nfe) {
			prompt.printError("Error! Invalid argument <port>! Not a number!");
			prompt.printError("Usage: Server <port>!");
			System.exit(1);
		}
	}

	private void initializeConnection() {
		this.logger.info("Opening server socket...");
		try {
			this.serverSocket = new ServerSocket(port);
			this.running = true;
			this.logger.info("Server listening on port: " + this.serverSocket.getLocalPort());
		} catch (IOException e) {
			this.serverSocket = null;
			this.running = false;
			this.logger.error("Error! Cannot open server socket:");
			if (e instanceof BindException)
				this.logger.error("Port " + port + " is already bound!");
		}
	}
}
