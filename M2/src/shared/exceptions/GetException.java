package shared.exceptions;

public class GetException extends Exception {

    private String key;

    public GetException(String key, String message) {
        super(message);
        this.key = key;
    }

    public String getKey() { return key; }
}
