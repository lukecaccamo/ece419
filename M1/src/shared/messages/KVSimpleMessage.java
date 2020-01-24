package shared.messages;

import java.io.Serializable;

/**
 * Represents a simple KV message, which is intended to be received and sent 
 * by the client and the server.
 */
public class KVSimpleMessage implements Serializable, KVMessage {

	private static final char LINE_FEED = 0x0A;
	private static final char RETURN = 0x0D;

	private StatusType status;
    private String key;
    private String value;

	public KVSimpleMessage(StatusType status, String key, String value) {
		this.status = status;
		this.key = key;
		this.value = value;
	}

	/**
	 * Returns the content of this KVMessage as a String.
	 * 
	 * @return the content of this message in String format.
	 */
	public String getMsg() {
		String msg = this.status + " " + this.key + " " + this.value;
		return msg.trim();
	}

	/**
	 * Returns an array of bytes that represent the ASCII coded message content.
	 * 
	 * @return the content of this message as an array of bytes 
	 * 		in ASCII coding.
	 */
	public byte[] getMsgBytes() {
		byte[] msgBytes = toByteArray(this.getMsg());
		return msgBytes;
	}

	/**
	 * @return the key that is associated with this message, 
	 * 		null if not key is associated.
	 */
	@Override
	public String getKey() {
		return key;
	}

	/**
	 * @return the value that is associated with this message, 
	 * 		null if not value is associated.
	 */
	@Override
	public String getValue() {
		return value;
	}

	/**
	 * @return a status string that is used to identify request types, 
	 * response types and error types associated to the message.
	 */
	@Override
	public StatusType getStatus() {
		return status;
	}

	private byte[] toByteArray(String s){
		byte[] bytes = s.getBytes();
		byte[] ctrBytes = new byte[]{LINE_FEED, RETURN};
		byte[] tmp = new byte[bytes.length + ctrBytes.length];
		
		System.arraycopy(bytes, 0, tmp, 0, bytes.length);
		System.arraycopy(ctrBytes, 0, tmp, bytes.length, ctrBytes.length);
		
		return tmp;		
	}
}
