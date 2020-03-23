package shared.messages;

public interface IKVAdminMessage {
	
	public enum ActionType {
		INIT,
		START,
		STOP,
		SHUTDOWN,
		LOCK_WRITE,
		UNLOCK_WRITE,
		MOVE_DATA,
		UPDATE,
		GET_SERVER_STATE,
		IS_WRITER_LOCKED,
		GET_METADATA,
		MOVE_REPLICAS,

		INIT_ACK,
		START_ACK,
		STOP_ACK,
		SHUTDOWN_ACK,
		LOCK_WRITE_ACK,
		UNLOCK_WRITE_ACK,
		MOVE_DATA_ACK,
		UPDATE_ACK,
		GET_SERVER_STATE_ACK,
		IS_WRITER_LOCKED_ACK,
		GET_METADATA_ACK,
		MOVE_REPLICAS_ACK
	}

	/**
	 * @return the ActionType that is associated with this message
	 */
	public ActionType getAction();
	
}


