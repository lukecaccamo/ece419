package shared.communications;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import app_kvServer.KVServer;
import org.apache.log4j.Logger;

import shared.messages.KVMessage;
import shared.messages.KVMessage.StatusType;
import shared.messages.KVSimpleMessage;

public class KVCommModule implements Runnable {

	private Logger logger = Logger.getRootLogger();
	private KVServer server;
	private boolean running;

	private Socket socket;
	private OutputStream output;
 	private InputStream input;
	
	private static final int BUFFER_SIZE = 1024;
	private static final int DROP_SIZE = 1024 * BUFFER_SIZE;

	private String serverAddress;
	private int serverPort;

	/**
	 * Initialize KVCommModule with address and port of KVServer
	 * @param socket socket for the KVServer
	 */
	public KVCommModule(Socket socket, KVServer server) {
		this.socket = socket;
		this.server = server;
		this.serverAddress = this.socket.getInetAddress().getHostAddress();
		this.serverPort = this.socket.getLocalPort();
		this.setRunning(true);
		logger.info("Connection established");
	}

	// Called by server only
	public void run() {
		try {
			output = this.socket.getOutputStream();
			input = this.socket.getInputStream();

			while(running) {
				try {

					KVMessage msg = receiveKVMessage();
					StatusType status = msg.getStatus();
					String key = msg.getKey();
					String value = msg.getValue();

					switch (status) {

						case GET:
							value = server.getKV(key);
							sendKVMessage(StatusType.GET_SUCCESS, key, value);
							break;

						case PUT:
							server.putKV(key, value);
							sendKVMessage(value.equals("null") ? StatusType.DELETE_SUCCESS : StatusType.PUT_SUCCESS, key, value);
							break;
					}



					/* connection either terminated by the client or lost due to
					 * network problems*/
				} catch (IOException ioe) {
					logger.error("Error! Connection lost!");
					running = false;
				} catch (Exception ex) {
					sendKVMessage(StatusType.GET_ERROR, null, null);
					sendKVMessage(StatusType.PUT_ERROR, null, null);
				}
			}

		} catch (IOException ioe) {
			logger.error("Error! Connection could not be established!", ioe);

		} finally {

			try {
				if (this.socket != null) {
					input.close();
					output.close();
					this.socket.close();
				}
			} catch (IOException ioe) {
				logger.error("Error! Unable to tear down connection!", ioe);
			}
		}
	}

	public void connect() throws Exception {
		this.output = this.socket.getOutputStream();
		this.input = this.socket.getInputStream();
	}

	public void disconnect() {
		logger.info("try to close connection ...");
		
		try {
			tearDownConnection();
		} catch (IOException ioe) {
			logger.error("Unable to close connection!");
		}
	}

	private void tearDownConnection() throws IOException {
		setRunning(false);
		logger.info("tearing down the connection ...");
		if (this.socket != null) {
			//input.close();
			//output.close();
			this.socket.close();
			this.socket = null;
			logger.info("connection closed!");
		}
	}

	public boolean isRunning() {
		return this.running;
	}

	public void setRunning(boolean run) {
		this.running = run;
	}

	/**
	 * Method sends a KVMessage using this socket.
	 * @param status the status of the message to be sent.
	 * @param key the key.
	 * @param value the value.
	 * @throws IOException some I/O error regarding the output stream
	 */
	public void sendKVMessage(StatusType status, String key, String value) throws IOException {
		KVSimpleMessage msg = new KVSimpleMessage(status, key, value);
		byte[] msgBytes = msg.getMsgBytes();
		this.output.write(msgBytes, 0, msgBytes.length);
		this.output.flush();
		logger.info("Send message:\t '" + msg.getMsg() + "'");
    }

	public KVSimpleMessage receiveKVMessage() throws IOException {
		
		int index = 0;
		byte[] msgBytes = null, tmp = null;
		byte[] bufferBytes = new byte[BUFFER_SIZE];
		
		/* read first char from stream */
		byte read = (byte) this.input.read();	
		boolean reading = true;
		
		while(read != 13 && reading) {/* carriage return */
			/* if buffer filled, copy to msg array */
			if(index == BUFFER_SIZE) {
				if(msgBytes == null){
					tmp = new byte[BUFFER_SIZE];
					System.arraycopy(bufferBytes, 0, tmp, 0, BUFFER_SIZE);
				} else {
					tmp = new byte[msgBytes.length + BUFFER_SIZE];
					System.arraycopy(msgBytes, 0, tmp, 0, msgBytes.length);
					System.arraycopy(bufferBytes, 0, tmp, msgBytes.length,
							BUFFER_SIZE);
				}

				msgBytes = tmp;
				bufferBytes = new byte[BUFFER_SIZE];
				index = 0;
			} 
			
			/* only read valid characters, i.e. letters and numbers */
			if((read > 31 && read < 127)) {
				bufferBytes[index] = read;
				index++;
			}
			
			/* stop reading is DROP_SIZE is reached */
			if(msgBytes != null && msgBytes.length + index >= DROP_SIZE) {
				reading = false;
			}
			
			/* read next char from stream */
			read = (byte) this.input.read();
		}
		
		if(msgBytes == null){
			tmp = new byte[index];
			System.arraycopy(bufferBytes, 0, tmp, 0, index);
		} else {
			tmp = new byte[msgBytes.length + index];
			System.arraycopy(msgBytes, 0, tmp, 0, msgBytes.length);
			System.arraycopy(bufferBytes, 0, tmp, msgBytes.length, index);
		}
		
		msgBytes = tmp;
		
		/* build final String */
		String msg = new String(msgBytes).trim();
		String[] tokens = msg.split("\\s+");

		KVSimpleMessage ret = new KVSimpleMessage(StatusType.NONE, null, null);

		//handle message types
		StatusType status = StatusType.valueOf(tokens[0]);

		switch (status) {

			case GET:
				ret = new KVSimpleMessage(status, tokens[1], null);
				break;

			case PUT:
				ret = new KVSimpleMessage(status, tokens[1], tokens[2]);
				break;

			case DELETE_SUCCESS:
				ret = new KVSimpleMessage(status, tokens[1], null);
				break;

			default:
				ret = new KVSimpleMessage(status, tokens[1], tokens[2]);
		}

		logger.info("Receive message:\t '" + ret.getMsg() + "'");
		return ret;
    }

}