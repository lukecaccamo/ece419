package shared.metadata;

import app_kvServer.IKVServer.ServerStateType;

public class ServerData {

    private String name;
    private String host;
    int port;

    public void setName(String name) {
        this.name = name;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    // ServerStateType serverStateType;

    public ServerData() {
        this.name = null;
        this.host = null;
        this.port = 0;
    }

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
