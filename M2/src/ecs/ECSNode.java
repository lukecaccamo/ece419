package ecs;

public class ECSNode implements IECSNode {

    private String name;
    private String host;
    private int port;
    private String[] hashRange;

    public ECSNode(String name, String host, int port, String from, String to) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.hashRange = new String[] {from, to};
    }

    /**
     * @return the name of the node (ie "Server 8.8.8.8")
     */
    public String getNodeName() {
        return this.name;
    }

    /**
     * @return the hostname of the node (ie "8.8.8.8")
     */
    public String getNodeHost() {
        return this.host;
    }

    /**
     * @return the port number of the node (ie 8080)
     */
    public int getNodePort() {
        return this.port;
    }

    /**
     * @return array of two strings representing the low and high range of the hashes that the given
     *         node is responsible for
     */
    public String[] getNodeHashRange() {
        return this.hashRange;
    }
}
