package shared.messages;

import java.io.Serializable;

/**
 * Represents a simple KV message, which is intended to be received and sent 
 * by the client and the server.
 */
public class KVSimpleMessage implements Serializable, KVMessage {

	private StatusType status;
    private String key;
	private String value;
	
	String msg;

	public KVSimpleMessage() {
		this.status = null;
		this.key = null;
		this.value = null;
		this.msg = null;
	}

	public KVSimpleMessage(StatusType status, String key, String value) {
		this.status = status;
		this.key = key;
		this.value = value;

		this.msg = this.status.toString();
		if(this.key != null) this.msg += " " + this.key;
		if(this.value != null) this.msg += " " + this.value;
		this.msg.trim();
	}

	public void setStatus(StatusType status) {
		this.status = status;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setMsg(String msg) {
		this.msg = msg;
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
	 * @return the key that is associated with this message, 
	 * 		null if not key is associated.
	 */
	//@Override
	public String getKey() {
		return key;
	}

	/**
	 * @return the value that is associated with this message, 
	 * 		null if not value is associated.
	 */
	//@Override
	public String getValue() {
		return value;
	}

	/**
	 * @return a status string that is used to identify request types, 
	 * response types and error types associated to the message.
	 */
	//@Override
	public StatusType getStatus() {
		return status;
	}
}
