package shared.metadata;

import app_kvServer.IKVServer.ServerStateType;

public class ServerData {

    private String name;
    private String host;
    int port;

    // ServerStateType serverStateType;

    public ServerData(String name, String host, int port) {
        this.name = name;
        this.host = host;
        this.port = port;
    }

    public String getName() {
        return this.name;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    /*
    public ServerStateType getServerStateType() {
        return this.serverStateType;
    }
    

    public void setServerStateType(ServerStateType serverStateType) {
        this.serverStateType = serverStateType;
    }
    */


}
