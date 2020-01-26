package app_kvServer;

import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import shared.communications.KVCommModule;
import shared.exceptions.GetException;
import shared.exceptions.PutException;

import java.io.IOException;
import java.net.*;

public class KVServer implements IKVServer, Runnable {

	private static Logger logger = Logger.getRootLogger();

	private int port;
	private String strategy;
	private int cacheSize;
	private ServerSocket serverSocket;
	private boolean running;

	/**
	 * Start KV Server at given port
	 * @param port given port for storage server to operate
	 * @param cacheSize specifies how many key-value pairs the server is allowed
	 *           to keep in-memory
	 * @param strategy specifies the cache replacement strategy in case the cache
	 *           is full and there is a GET- or PUT-request on a key that is
	 *           currently not contained in the cache. Options are "FIFO", "LRU",
	 *           and "LFU".
	 */
	public KVServer(int port, int cacheSize, String strategy) {
		// TODO Auto-generated method stub
		this.port = port;
		this.cacheSize = cacheSize;
		this.strategy = strategy;
		new Thread(this).start();
	}
	
	@Override
	public int getPort(){
		return this.serverSocket.getLocalPort();
	}

	@Override
    public String getHostname() {

		String host = null;

		try {
			host = InetAddress.getLocalHost().getHostName();
		} catch (Exception ex) {
			return null;
		}

		return host;
	}

	@Override
    public CacheStrategy getCacheStrategy(){
		return IKVServer.CacheStrategy.valueOf(this.strategy);
	}

	@Override
    public int getCacheSize(){
		return this.cacheSize;
	}

	@Override
    public boolean inStorage(String key){
		// TODO Auto-generated method stub
		return false;
	}

	@Override
    public boolean inCache(String key){
		// TODO Auto-generated method stub
		return false;
	}

	@Override
    public String getKV(String key) throws Exception{
		// TODO Auto-generated method stub
		if (key.equals("error"))
			throw new GetException(key, "GET failed unexpectedly!");

		return "value";
	}

	@Override
    public void putKV(String key, String value) throws Exception{
		// TODO Auto-generated method stub
		if (key.equals("error"))
			throw new PutException(key, value, "PUT failed unexpectedly!");
	}

	@Override
    public void clearCache(){
		// TODO Auto-generated method stub
	}

	@Override
    public void clearStorage(){
		// TODO Auto-generated method stub
	}

	@Override
    public void run(){

		running = initializeServer();

		if(this.serverSocket != null) {
			while(isRunning()){
				try {

					Socket client = this.serverSocket.accept();
					KVCommModule connection = new KVCommModule(client, this);
					new Thread(connection).start();

					logger.info("Connected to "
							+ client.getInetAddress().getHostName()
							+  " on port " + client.getPort());
				} catch (IOException e) {
					logger.error("Error! " +
							"Unable to establish connection. \n", e);
				}
			}
		}
		logger.info("Server stopped.");
	}

	@Override
    public void kill(){
		this.running = false;
        try {
			this.serverSocket.close();
		} catch (IOException e) {
			logger.error("Error! " +
					"Unable to close socket on port: " + this.getPort(), e);
		}
	}

	@Override
    public void close(){
		this.running = false;
        try {
			this.serverSocket.close();
		} catch (IOException e) {
			logger.error("Error! " +
					"Unable to close socket on port: " + this.getPort(), e);
		}
	}

	private boolean isRunning() {
		return this.running;
	}

	private boolean initializeServer() {
		logger.info("Initialize server ...");
		try {
			this.serverSocket = new ServerSocket(port);
			logger.info("Server listening on port: "
					+ this.serverSocket.getLocalPort());
			return true;

		} catch (IOException e) {
			logger.error("Error! Cannot open server socket:");
			if(e instanceof BindException){
				logger.error("Port " + port + " is already bound!");
			}
			return false;
		}
	}

	/**
	 * Main entry point for the echo server application.
	 * @param args contains the port number at args[0].
	 */
	public static void main(String[] args) {
		try {
			new LogSetup("logs/server.log", Level.ALL);
			if(args.length != 3) {
				System.out.println("Error! Invalid number of arguments!");
				System.out.println("Usage: Server <port> <cacheSize> <strategy>");
			} else {
				int port = Integer.parseInt(args[0]);
				int cacheSize = Integer.parseInt(args[1]);
				String strategy = args[2];

				KVServer server = new KVServer(port, cacheSize, strategy);
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

}
