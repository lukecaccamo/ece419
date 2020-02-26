package shared.exceptions;

public class DeleteException extends Exception {

    private String key;

    public DeleteException(String key, String message) {
        super(message);
        this.key = key;
    }

    public String getKey() { return key; }
}
