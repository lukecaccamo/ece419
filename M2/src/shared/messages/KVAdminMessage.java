package shared.messages;

import shared.hashring.HashRing;

import java.io.Serializable;

/**
 * Represents a KV Admin message, which is intended to be received and sent
 * by the ECS and the server.
 */
public class KVAdminMessage implements Serializable, IKVAdminMessage {

	private ActionType action;
	private String hashKey;
    private HashRing hashRing = null;

	public KVAdminMessage(ActionType action, String hashKey, HashRing hashRing) {
		this.action = action;
		this.hashKey = hashKey;
		this.hashRing = hashRing;
	}

	// Set
	public void setAction(ActionType action) {
		this.action = action;
	}

	public void setHashKey(String hashKey) {
		this.hashKey = hashKey;
	}

	public void setMetaData(HashRing hashRing) {
		this.hashRing = hashRing;
	}


	// Get
	public ActionType getAction() {
		return action;
	}

	public String getHashKey() {
		return hashKey;
	}

	public HashRing getMetaData() {
		return hashRing;
	}
}
