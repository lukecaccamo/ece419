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
		GET_METADATA
	}

	public static final String ADMIN_ID = "<ADMIN>";

	/**
	 * @return the ActionType that is associated with this message
	 */
	public ActionType getAction();
	
}


