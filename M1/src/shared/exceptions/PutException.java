package shared.exceptions;

public class PutException extends Exception {

    private String key;

    private String value;

    public PutException(String key, String value, String message) {
        super(message);
        this.key = key;
        this.value = value;
    }

    public String getKey() { return key; }

    public String getValue() { return value; }
}
