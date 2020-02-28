package shared.metadata;

public class MetaData {

    private static String delimit = ";";

    private String name;

    private String host;

    int port;

    private String[] hashRange;

    private ServerStateType serverStateType;

    public MetaData(String name, String host, int port, String startHash, String endHash) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.hashRange = new String[] {startHash, endHash};
    }

    public String getName() {
        return this.name;
    }

    public String getHost() {
        return this.host;
    }

    public String[] getHashRange() {
        return this.hashRange;
    }

    public ServerStateType getServerStateType() {
        return this.serverStateType;
    }

    public void setServerStateType(ServerStateType serverStateType) {
        this.serverStateType = serverStateType;
    }

    public String serialize() {
        String msg = name;
        msg += delimit + host;
        msg += delimit + port;
        msg += delimit + hashRange[0];
        msg += delimit + hashRange[1];
        return msg;
    }

    public void deserialize(String msg) {
        String[] tokens = msg.split(delimit);
        name = tokens[0];
        host = tokens[1];
        port = Integer.parseInt(tokens[1]);
        hashRange[0] = tokens[3];
        hashRange[1] = tokens[4];
    }
}
