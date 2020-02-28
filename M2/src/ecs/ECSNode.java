package ecs;

import app_kvServer.IKVServer.CacheStrategy;
import shared.metadata.Hash;
import shared.metadata.MetaData;

public class ECSNode implements IECSNode {

    private String hash;
    private String name;
    private String host;
    private int port;
    private String[] hashRange;
    private IECSNodeFlag flag;
    private MetaData metaData;
    private int cacheSize;
    private CacheStrategy cacheStrategy;

    public ECSNode(String name, String host, int port) {
        this.hash = Hash.MD5(host + ":" + port);
        this.name = name;
        this.host = host;
        this.port = port;
        this.hashRange = new String[] {null, null};
        this.flag = IECSNodeFlag.SHUT_DOWN;
        this.metaData = null;
        this.cacheSize = 0;
        this.cacheStrategy = CacheStrategy.None;
    }

    /**
     * @return the MD5 hash of the node
     */
    @Override
    public String getHashKey() {
        return this.hash;
    }

    /**
     * @return the name of the node (ie "Server 8.8.8.8")
     */
    @Override
    public String getNodeName() {
        return this.name;
    }

    /**
     * @return the hostname of the node (ie "8.8.8.8")
     */
    @Override
    public String getNodeHost() {
        return this.host;
    }

    /**
     * @return the port number of the node (ie 8080)
     */
    @Override
    public int getNodePort() {
        return this.port;
    }

    /**
     * @return array of two strings representing the low and high range of the hashes that the given
     *         node is responsible for
     */
    @Override
    public String[] getNodeHashRange() {
        return this.hashRange;
    }

    @Override
    public void setNodeHashRange(String from, String to) {
        this.hashRange = new String[] {from, to};
    }

    /**
     * @return the flag for the ECSNode
     */
    @Override
    public IECSNodeFlag getFlag() {
        return this.flag;
    }

    @Override
    public void setFlag(IECSNodeFlag flag) {
        this.flag = flag;
    }

    /**
     * @return the meta data
     */
    @Override
    public MetaData getMetaData() {
        return this.metaData;
    }

    @Override
    public void setMetaData(MetaData metaData) {
        this.metaData = metaData;
    }
}
