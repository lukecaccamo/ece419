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

	public KVSimpleMessage(StatusType status, String key, String value) {
		this.status = status;
		this.key = key;
		this.value = value;
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
}
