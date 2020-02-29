package shared.messages;

import shared.metadata.MetaData;

import java.io.Serializable;

/**
 * Represents a KV  Admin message, which is intended to be received and sent
 * by the ECS and the server.
 */
public class KVAdminMessage implements Serializable, IKVAdminMessage {

	private ActionType action;
    private MetaData metaData = null;
    private int cacheSize = 0;
	private String replacementStrategy = null;
    private String server = null;
    private String startHash = null;
    private String endHash = null;
    private String[] range = null;


	public KVAdminMessage(ActionType action, MetaData metaData, String ... params) {
		this.action = action;

		if (metaData != null || params.length > 0)
			updateParams(action, metaData, params);
	}

	// Set
	public void setAction(ActionType action) {
		this.action = action;
	}

	public void setMetaData(MetaData metaData) {
		this.metaData = metaData;
	}

	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}

	public void setReplacementStrategy(String replacementStrategy) {
		this.replacementStrategy = replacementStrategy;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public void setRange(String[] range) {
		this.range = range;
	}

	public void setStartHash(String startHash) {
		this.startHash = startHash;
		range = new String[] {startHash, this.endHash};
	}

	public void setEndHash(String endHash) {
		this.endHash = endHash;
		range = new String[] {this.startHash, endHash};
	}


	// Get
	@Override
	public ActionType getAction() {
		return action;
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
		return range;
	}

	private void updateParams(ActionType action, MetaData metaData, String[] params) {

		switch (action) {
			case INIT:
				this.metaData = metaData;
				cacheSize = Integer.getInteger(params[0]);
				replacementStrategy = params[1];
				break;
			case MOVE_DATA:
				startHash = params[0];
				endHash = params[1];
				server = params[2];
				range = new String[] {params[0], params[1]};
				break;
			case UPDATE:
				this.metaData = metaData;
				break;
			default:
				break;
		}
	}
}
