package shared.communications;

import java.io.Serializable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import shared.messages.KVMessage;
import shared.messages.KVSimpleMessage;
import shared.messages.KVMessage.StatusType;

/**
 * Represents a simple KV message, which is intended to be received and sent 
 * by the client and the server.
 */
public class KVStoreCommunications {

	private Logger logger = Logger.getRootLogger();
	private boolean running;
	
	private Socket storeSocket;
	private OutputStream output;
 	private InputStream input;
	
	private static final int BUFFER_SIZE = 1024;
	private static final int DROP_SIZE = 1024 * BUFFER_SIZE;
	
	
	public KVStoreCommunications(String address, int port) 
			throws UnknownHostException, IOException {
		try {
			this.storeSocket = new Socket(address, port);
			output = this.storeSocket.getOutputStream();
			input = this.storeSocket.getInputStream();
			setRunning(true);
			logger.info("Connection established");
		} catch (IOException ioe) {
			if(isRunning()) {
				logger.error("Connection lost!");
				try {
					tearDownConnection();
				} catch (IOException e) {
					logger.error("Unable to close connection!");
				}
			}
		} catch (Exception e) {
			logger.error(e);
		}
	}
	
	public void closeConnection() {
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
		if (this.storeSocket != null) {
			input.close();
			output.close();
			this.storeSocket.close();
			this.storeSocket = null;
			logger.info("connection closed!");
		}
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void setRunning(boolean run) {
		running = run;
	}
	
	/**
	 * Method sends a KVMessage using this socket.
	 * @param msg the message that is to be sent.
	 * @throws IOException some I/O error regarding the output stream 
	 */
	public void sendKVMessage(StatusType status, String key, String value) throws IOException {
		KVSimpleMessage msg = new KVSimpleMessage(status, key, value);
		byte[] msgBytes = msg.getMsgBytes();
		output.write(msgBytes, 0, msgBytes.length - 1);
		output.flush();
		logger.info("Send message:\t '" + msg.getMsg() + "'");
    }
	
	
	public KVMessage receiveKVMessage() throws IOException {
		
		int index = 0;
		byte[] msgBytes = null, tmp = null;
		byte[] bufferBytes = new byte[BUFFER_SIZE];
		
		/* read first char from stream */
		byte read = (byte) input.read();	
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
			read = (byte) input.read();
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
		logger.info(tokens);
		KVSimpleMessage ret = new KVSimpleMessage(StatusType.valueOf(tokens[0]), null, null);
		logger.info("Receive message:\t '" + ret.getMsg() + "'");
		return ret;
    }
}
