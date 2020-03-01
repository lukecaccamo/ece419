package shared.messages;

public interface IKVAdminMessage {
	
	public enum ActionType {
		INIT, /* STOP */
		START, /* START */
		STOP, /* STOP */
		SHUTDOWN, /* SHUT_DOWN */
		LOCK_WRITE, /* KV_TRANSFER */
		UNLOCK_WRITE, /* TRANSFER_FINISH */
		MOVE_DATA, /* KV_TRANSFER */
		UPDATE, /* UPDATE */
		GET_SERVER_STATE, /* I don't think this is used */
		IS_WRITER_LOCKED, /* I don't think this is used */
		GET_METADATA /* I don't think this is used */
	}

	public static final String ADMIN_ID = "<ADMIN>";

	/**
	 * @return the ActionType that is associated with this message
	 */
	public ActionType getAction();
	
}


