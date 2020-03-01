package app_kvServer;

import app_kvServer.KVCache.FIFOCache;
import app_kvServer.KVCache.IKVCache;
import app_kvServer.KVCache.LFUCache;
import app_kvServer.KVCache.LRUCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import shared.communications.KVCommModule;
import shared.exceptions.DeleteException;
import shared.exceptions.GetException;
import shared.exceptions.PutException;
import shared.hashring.Hash;
import shared.hashring.HashRing;
import ecs.IECSNode;

import org.apache.zookeeper.*;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.data.Stat;
import java.util.concurrent.CountDownLatch;

import java.io.IOException;
import java.math.BigInteger;
import java.net.*;

public class KVServer implements IKVServer, Runnable {

	private static Logger logger = Logger.getRootLogger();

	private String name;
	private String zkHost;
	private int zkPort;

	private String host;
	private int port;
	private String strategy;
	private int cacheSize;
	private ServerSocket serverSocket;
	private boolean running;
	private boolean writeLock;
	private ServerStateType serverStateType;
	private IKVCache cache;
	private KVDatabase db;
	private HashRing metaData;
	private BigInteger serverHash;
	private ObjectMapper om;
	private static final String DELETE_VAL = "null";

	private ZooKeeper zookeeper;
	private CountDownLatch connected;

	/**
	 * Start KV Server at given port
	 * 
	 * @param port      given port for storage server to operate
	 * @param cacheSize specifies how many key-value pairs the server is allowed to keep in-memory
	 * @param strategy  specifies the cache replacement strategy in case the cache is full and there
	 *                  is a GET- or PUT-request on a key that is currently not contained in the
	 *                  cache. Options are "FIFO", "LRU", and "LFU".
	 */
	public KVServer(int port, int cacheSize, String strategy) {

		this.port = port;
		this.cacheSize = cacheSize;
		this.strategy = strategy;
		this.serverStateType = ServerStateType.STOPPED;
		this.writeLock = false;
		this.host = "";

		try {
			InetAddress localhost = InetAddress.getLocalHost();
			this.host = (localhost.getHostAddress()).trim();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		this.serverHash = Hash.MD5_BI(this.host + ":" + this.port);
		this.om = new ObjectMapper();

		assignCache(strategy);

		db = new KVDatabase(this.port);
		new Thread(this).start();
	}

	/**
	 * Start KV Server at given port w/ ZooKeeper
	 * 
	 * @param name      given name for this server
	 * @param zkHost    given IP for ZooKeeper to operate
	 * @param zkPort    given port for ZooKeeper to operate
	 * @param port      given port for storage server to operate
	 * @param cacheSize specifies how many key-value pairs the server is allowed to keep in-memory
	 * @param strategy  specifies the cache replacement strategy in case the cache is full and there
	 *                  is a GET- or PUT-request on a key that is currently not contained in the
	 *                  cache. Options are "FIFO", "LRU", and "LFU".
	 */
	public KVServer(String name, String zkHost, int zkPort, int port, int cacheSize,
			String strategy) {

		this.logger.setLevel(Level.ERROR);
		this.name = name;
		this.zkHost = zkHost;
		this.zkPort = zkPort;

		this.port = port;
		this.cacheSize = cacheSize;
		this.strategy = strategy;
		this.serverStateType = ServerStateType.STOPPED;
		this.writeLock = false;
		this.host = "";

		try {
			InetAddress localhost = InetAddress.getLocalHost();
			this.host = (localhost.getHostAddress()).trim();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		this.serverHash = Hash.MD5_BI(this.host + ":" + this.port);
		this.om = new ObjectMapper();

		assignCache(strategy);

		initializeZooKeeper();

		db = new KVDatabase(this.port);
		new Thread(this).start();
	}

	// M2
	private void initializeZooKeeper() {
		String zkParentName = "/ECSAdmin";
		String zkNodeName = zkParentName + "/" + this.name;

		try {
			this.connected = new CountDownLatch(1);
			Watcher watcher = new Watcher() {
				@Override
				public void process(WatchedEvent we) {
					if (we.getState() == KeeperState.SyncConnected) {
						connected.countDown();
					}
				}
			};
			this.zookeeper = new ZooKeeper(this.zkHost + ":" + this.zkPort, 300000000, watcher);
			connected.await();
			this.zookeeper.create(zkNodeName, null, ZooDefs.Ids.OPEN_ACL_UNSAFE,
					CreateMode.PERSISTENT);
		} catch (IOException | KeeperException | InterruptedException e) {
			logger.error(e);
		}

		try {
			byte[] jsonBytes = null;
			while (jsonBytes == null) {
				Stat zkStat = this.zookeeper.exists(zkNodeName, false);
				jsonBytes = this.zookeeper.getData(zkNodeName, false, zkStat);
			}
			String json = new String(jsonBytes).trim();
			System.out.println(this.name + " recieved HashRing: " + json);
		} catch (KeeperException | InterruptedException e) {
			logger.error(e);
		}
	}

	public void initKVServer(HashRing metaData, int cacheSize, String replacementStrategy) {
		this.metaData = metaData;
		this.cacheSize = cacheSize;
		this.serverStateType = ServerStateType.STOPPED;
		assignCache(replacementStrategy);
	}

	public ServerStateType getServerState() {
		return this.serverStateType;
	}

	public void start() {
		this.serverStateType = ServerStateType.STARTED;
	}

	public void stop() {
		this.serverStateType = ServerStateType.STOPPED;
	}

	public void shutDown() {
		// TODO: send shutdown message
		this.serverStateType = ServerStateType.SHUT_DOWN;

		close();
	}

	public HashRing getMetaData() {
		return this.metaData;
	}

	public void updateMetaData(HashRing metaData) {
		this.metaData = metaData;
	}

	public void moveData(String[] range, IECSNode server) {
		BigInteger start = Hash.MD5_BI(range[0]);
		BigInteger end = Hash.MD5_BI(range[1]);

		// foreach kv in db
		// db.get()
		// if in range
		// send to server
		// remove from db
		// if not in range
		// continue

		// when done send completed msg to ecs
	}

	public void lockWrite() {
		writeLock = true;
	}

	public void unlockWrite() {
		writeLock = false;
	}

	public boolean isWriterLocked() {
		return writeLock;
	}

	public boolean inServer(String key) {
		BigInteger keyHash = Hash.MD5_BI(key);
		return this.metaData.inServer(keyHash, this.serverHash);
	}

	// M1

	@Override
	public int getPort() {
		int port = 0;

		if (serverSocket != null) {
			port = serverSocket.getLocalPort();
		}

		return port;
	}

	@Override
	public String getHostname() {

		String host = null;

		if (serverSocket != null)
			host = serverSocket.getInetAddress().getHostName();

		return host;
	}

	@Override
	public CacheStrategy getCacheStrategy() {
		return CacheStrategy.valueOf(this.strategy);
	}

	@Override
	public int getCacheSize() {
		return this.cacheSize;
	}

	@Override
	public boolean inStorage(String key) {
		return db.inStorage(key);
	}

	@Override
	public boolean inCache(String key) {
		if (getCacheStrategy() == CacheStrategy.None) {
			return false;
		}
		return cache.inCache(key);
	}

	@Override
	public String getKV(String key) throws Exception {

		try {
			if (getCacheStrategy() != CacheStrategy.None) {
				String cache_return = cache.get(key);
				if (cache_return != null) {

					return cache_return;
				}
			}

			String db_return = db.get(key);
			// System.out.println("here");
			if (db_return != null && getCacheStrategy() != CacheStrategy.None) {
				cache.put(key, db_return);
			}

			return db_return;

		} catch (Exception ex) {
			throw new GetException(key, "GET failed unexpectedly!");
		}
	}

	@Override
	public void putKV(String key, String value) throws Exception {

		try {

			if (getCacheStrategy() != CacheStrategy.None) {
				cache.put(key, value);
			}


			db.put(key, value);
		} catch (Exception ex) {
			if (value.equals(DELETE_VAL)) {
				throw new DeleteException(key, "DELETE failed unexpectedly!");
			} else {
				throw new PutException(key, value, "PUT failed unexpectedly!");
			}
		}
	}

	@Override
	public void clearCache() {
		if (getCacheStrategy() != CacheStrategy.None) {
			cache.clear();
		}
	}

	@Override
	public void clearStorage() {

		if (getCacheStrategy() != CacheStrategy.None) {
			cache.clear();
		}

		db.clear();
	}

	@Override
	public void run() {

		running = initializeServer();

		if (this.serverSocket != null) {
			while (isRunning()) {
				try {
					Socket client = this.serverSocket.accept();
					KVCommModule connection = new KVCommModule(client, this);
					new Thread(connection).start();

					logger.info("Connected to " + client.getInetAddress().getHostName()
							+ " on port " + client.getPort());
				} catch (IOException e) {
					logger.error("Error! " + "Unable to establish connection. \n", e);
				}
			}
		}
		logger.info("Server stopped.");
	}

	@Override
	public void kill() {
		this.running = false;
		try {
			if (this.serverSocket != null && !this.serverSocket.isClosed())
				this.serverSocket.close();
		} catch (IOException e) {
			logger.error("Error! " + "Unable to close socket on port: " + this.getPort(), e);
		}
	}

	@Override
	public void close() {
		this.running = false;
		try {
			// TODO: Destroy all generated threads and close connections.
			if (this.serverSocket != null && !this.serverSocket.isClosed())
				this.serverSocket.close();
		} catch (IOException e) {
			logger.error("Error! " + "Unable to close socket on port: " + this.getPort(), e);
		}
	}

	private boolean isRunning() {
		return this.running;
	}

	private boolean initializeServer() {
		logger.info("Initialize server ...");
		try {
			this.serverSocket = new ServerSocket(port);
			logger.info("Server listening on port: " + this.serverSocket.getLocalPort());
			return true;

		} catch (IOException e) {
			logger.error("Error! Cannot open server socket:");
			if (e instanceof BindException) {
				logger.error("Port " + port + " is already bound!");
			}
			return false;
		}
	}

	/**
	 * Main entry point for the echo server application.
	 * 
	 * @param args contains the port number at args[0].
	 */
	public static void main(String[] args) {
		try {
			new LogSetup("logs/server.log", Level.ALL);
			if (args.length == 3) {
				int port = Integer.parseInt(args[0]);
				int cacheSize = Integer.parseInt(args[1]);
				String strategy = args[2];

				KVServer server = new KVServer(port, cacheSize, strategy);
			} else if (args.length == 6) {
				String zkName = args[0];
				String zkHost = args[1];
				int zkPort = Integer.parseInt(args[2]);
				int port = Integer.parseInt(args[3]);
				int cacheSize = Integer.parseInt(args[4]);
				String strategy = args[5];

				KVServer server = new KVServer(zkName, zkHost, zkPort, port, cacheSize, strategy);
			} else {
				System.out.println("Error! Invalid number of arguments!");
				System.out.println("Usage: Server <port> <cacheSize> <strategy>");
			}
		} catch (IOException e) {
			System.out.println("Error! Unable to initialize logger!");
			e.printStackTrace();
			System.exit(1);
		} catch (NumberFormatException nfe) {
			System.out.println("Error! Invalid argument <port>! Not a number!");
			System.out.println("Usage: Server <port>!");
			System.exit(1);
		}
	}

	private void assignCache(String strategy) {
		switch (CacheStrategy.valueOf(strategy)) {
			case LRU:
				cache = new LRUCache(this.cacheSize);
			case FIFO:
				cache = new FIFOCache(this.cacheSize);
			case LFU:
				cache = new LFUCache(this.cacheSize);
			case None:
				break;
			default:
				// logger error
				break;
		}
	}

}

