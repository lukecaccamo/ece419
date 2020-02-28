package shared.messages;

import shared.metadata.MetaData;

import java.io.Serializable;

/**
 * Represents a KV  Admin message, which is intended to be received and sent
 * by the ECS and the server.
 */
public class KVAdminMessage implements Serializable, IKVAdminMessage {

	private static final char LINE_FEED = 0x0A;
	private static final char RETURN = 0x0D;

	private ActionType action;
    private String params = null;
    private int argCount = 0;
    private MetaData metaData = null;
    private int cacheSize = 0;
    private String replacementStrategy = null;
    private String server = null;
    private String startHash = null;
    private String endHash = null;


	String msg;
	byte[] msgBytes;

	public KVAdminMessage(ActionType action, String ... params) {
		this.action = action;
		this.argCount = 0;

		for (String arg : params){
			this.argCount++;
			this.params += ADMIN_DELIMIT + arg;
		}

		this.msg = this.action.toString() + this.params;
		this.msg.trim();

		//Add MSG ID for sending
		this.msgBytes = toByteArray(ADMIN_ID + this.msg);

		if (this.params != null)
			updateParams(action, this.params);
	}

	/**
	 * Returns the content of this KVMessage as a String.
	 * 
	 * @return the content of this message in String format.
	 */
	public String getMsg() {
		return this.msg;
	}

	/**
	 * Returns an array of bytes that represent the ASCII coded message content.
	 * 
	 * @return the content of this message as an array of bytes 
	 * 		in ASCII coding.
	 */
	public byte[] getMsgBytes() {
		return this.msgBytes;
	}

	/**
	 * @return the key that is associated with this message, 
	 * 		null if not key is associated.
	 */
	@Override
	public ActionType getAction() {
		return action;
	}

	/**
	 * @return the value that is associated with this message, 
	 * 		null if not value is associated.
	 */
	@Override
	public String getParams() {
		return params;
	}

	@Override
	public int getArgCount() {
		return argCount;
	}

	public MetaData getMetaData() {
		return metaData;
	}

	public int getCacheSize() {
		return cacheSize;
	}

	public String getReplacementStrategy() {
		return replacementStrategy;
	}

	public String getServer() {
		return server;
	}

	public String getStartHash() {
		return startHash;
	}

	public String getEndHash() {
		return endHash;
	}

	public String [] getRange() {
		return new String[] {startHash, endHash};
	}


	private byte[] toByteArray(String s){
		byte[] bytes = s.getBytes();
		byte[] ctrBytes = new byte[]{LINE_FEED, RETURN};
		byte[] tmp = new byte[bytes.length + ctrBytes.length];
		
		System.arraycopy(bytes, 0, tmp, 0, bytes.length);
		System.arraycopy(ctrBytes, 0, tmp, bytes.length, ctrBytes.length);
		
		return tmp;		
	}

	private void updateParams(ActionType action, String params) {
		String[] tokens = msg.split(ADMIN_DELIMIT);

		switch (action) {
			case INIT:
				metaData = new MetaData(tokens[0]);
				cacheSize = Integer.getInteger(tokens[1]);
				replacementStrategy = tokens[2];
				break;
			case MOVE_DATA:
				startHash = tokens[0];
				endHash = tokens[1];
				server = tokens[2];
				break;
			case UPDATE:
				metaData = new MetaData(tokens[0]);
				break;
			default:
				break;
		}
	}
}
