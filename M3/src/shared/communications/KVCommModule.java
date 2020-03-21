package shared.communications;

import app_kvServer.IKVServer;
import app_kvServer.KVServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import shared.exceptions.DeleteException;
import shared.exceptions.GetException;
import shared.exceptions.PutException;
import shared.hashring.Hash;
import shared.hashring.HashRing;
import shared.messages.KVAdminMessage;
import shared.messages.KVMessage.StatusType;
import shared.messages.KVSimpleMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class KVCommModule implements Runnable {

	private static final char LINE_FEED = 0x0A;
	private static final char RETURN = 0x0D;

	private Logger logger = Logger.getRootLogger();
	private KVServer server;
	private boolean running;

	private Socket socket;
	private OutputStream output;
 	private InputStream input;

	private static final int BUFFER_SIZE = 1024;
	private static final int DROP_SIZE = 1024 * BUFFER_SIZE;
	private static final String DELETE_VALUE = "null";

	private String serverAddress;
	private int serverPort;

	private ObjectMapper om;
	/**
	 * Initialize KVCommModule with address and port of KVServer
	 * @param socket socket for the KVServer
	 */
	public KVCommModule(Socket socket, KVServer server) {
		this.running = true;
		this.socket = socket;
		this.server = server;
		this.serverAddress = this.socket.getInetAddress().getHostAddress();
		this.serverPort = this.socket.getLocalPort();

		this.om = new ObjectMapper();
		logger.info("Connection established");
	}

	// Called by server only
	public void run() {
		try {

			output = this.socket.getOutputStream();
			input = this.socket.getInputStream();

			while(this.running) {
				try {

					String msg = getMessage();

					KVSimpleMessage simpleMessage = om.readValue(msg, KVSimpleMessage.class);

					// Check if the server can respond to requests
					if (server.getServerState() == IKVServer.ServerStateType.STOPPED) {
						sendKVMessage(StatusType.SERVER_STOPPED, simpleMessage.getKey(), simpleMessage.getValue());
					} else if (server.isWriterLocked() && simpleMessage.getStatus() == StatusType.PUT) {
						sendKVMessage(StatusType.SERVER_WRITE_LOCK, simpleMessage.getKey(), null);
					} else {
						sendKVSimpleMsgResponse(simpleMessage);
					}

				} catch (IOException ioe) {
					logger.error("Error! Connection lost!");
					this.running = false;
				} catch (PutException pex) {
					sendKVMessage(StatusType.PUT_ERROR, pex.getKey(), pex.getValue());
					logger.error(pex.getMessage());
				} catch (DeleteException dex) {
					sendKVMessage(StatusType.DELETE_ERROR, dex.getKey(), null);
					logger.error(dex.getMessage());
				} catch (GetException gex) {
					sendKVMessage(StatusType.GET_ERROR, gex.getKey(), null);
					logger.error(gex.getMessage());
				} catch (Exception ex){
					ex.printStackTrace();
					sendKVMessage(StatusType.FAILED, ex.getMessage(), null);
					logger.error(ex.getMessage());
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
		this.running = false;
		logger.info("tearing down the connection ...");
		if (this.socket != null) {
			this.socket.close();
			this.socket = null;
			logger.info("connection closed!");
		}
	}

	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Method sends a KVMessage using this socket.
	 * @param status the status of the message to be sent.
	 * @param key the key.
	 * @param value the value.
	 * @throws IOException some I/O error regarding the output stream
	 */
	public void sendKVMessage(StatusType status, String key, String value) throws IOException {

		if (value != null && value.isEmpty())
			value = DELETE_VALUE;


		KVSimpleMessage msg = new KVSimpleMessage(status, key, value);
		String simpleMsg = om.writeValueAsString(msg);

		byte[] msgBytes = toByteArray(simpleMsg);
		this.output.write(msgBytes, 0, msgBytes.length);
		this.output.flush();
		logger.info("Send simple message:\t '" + msg.getMsg() + "'");
    }

	// For client
	public KVSimpleMessage receiveKVMessage() throws IOException {
		String msg = getMessage();
		return om.readValue(msg, KVSimpleMessage.class);
	}

	private String getMessage() throws IOException {
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
			if(read > 0 ) {
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
		String msg = new String(msgBytes);

		return msg;
	}

	private void sendKVSimpleMsgResponse(KVSimpleMessage msg) throws Exception {
		StatusType status = msg.getStatus();
		String key = msg.getKey();
		String value = msg.getValue();

		switch (status) {
			case GET:
				if (!server.inServer(key)){
					String mdString = om.writeValueAsString(server.getMetaData());
					sendKVMessage(StatusType.SERVER_NOT_RESPONSIBLE, key, mdString);
					return;
				}

				value = server.getKV(key);

				if (value == null) {
					sendKVMessage(StatusType.GET_ERROR, key, null);
				} else {
					sendKVMessage(StatusType.GET_SUCCESS, key, value);
				}

				break;

			case PUT:
				if (!server.inServer(key)){
					String mdString = om.writeValueAsString(server.getMetaData());
					sendKVMessage(StatusType.SERVER_NOT_RESPONSIBLE, key, mdString);
					return;
				}

				status = StatusType.PUT_SUCCESS;

				if(value.equals(DELETE_VALUE))
					status = StatusType.DELETE_SUCCESS;
				else if(server.inCache(key) || server.inStorage(key)) {
					status = StatusType.PUT_UPDATE;
				}

				server.putKV(key, value);

				sendKVMessage(status, key, value);

				//do replication here
				if (!server.replicate(key, value)) {
					logger.error("Replication failure!");
				} else {
					logger.info("Replication success!");
				}

				break;

			case MOVE_VALUES:
				server.receiveData(value);
				sendKVMessage(StatusType.MOVE_VALUES_DONE, key, "");
				break;

			case REPLICATE:
				server.putKV(key, value);
				sendKVMessage(StatusType.REPLICATION_DONE, key, "");
				break;
		}
	}

	public static final byte[] toByteArray(String s){
		byte[] bytes = s.getBytes();
		byte[] ctrBytes = new byte[]{LINE_FEED, RETURN};
		byte[] tmp = new byte[bytes.length + ctrBytes.length];

		System.arraycopy(bytes, 0, tmp, 0, bytes.length);
		System.arraycopy(ctrBytes, 0, tmp, bytes.length, ctrBytes.length);

		return tmp;
	}

}