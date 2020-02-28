package ecs;

import shared.metadata.MetaData;

public interface IECSNode {
    public enum IECSNodeFlag {
        STOP, /* Node has stopped */
        START, /* Node has started */
        STATE_CHANGE, /* Node state has changed */
        KV_TRANSFER, /* Data transfer occurred */
        SHUT_DOWN, /* Node has shutdown */
        UPDATE, /* Node has updated */
        TRANSFER_FINISH /* Data transfer operation finished */
    }

    /**
     * @return the MD5 hash of the node
     */
    public String getHashKey();

    public enum ECSNodeFlag {
        STOP,
        START,
        STATE_CHANGE,
        KV_TRANSFER,
        SHUT_DOWN,
        UPDATE,
        TRANSFER_FINISH
    }

    /**
     * @return the name of the node (ie "Server 8.8.8.8")
     */
    public String getNodeName();

    /**
     * @return the hostname of the node (ie "8.8.8.8")
     */
    public String getNodeHost();

    /**
     * @return the port number of the node (ie 8080)
     */
    public int getNodePort();

    /**
     * @return array of two strings representing the low and high range of the hashes that the given
     *         node is responsible for
     */
    public String[] getNodeHashRange();

    public void setNodeHashRange(String from, String to);

    /**
     * @return the flag for the ECSNode
     */
    public IECSNodeFlag getFlag();

    public void setFlag(IECSNodeFlag flag);

    /**
     * @return the meta data
     */
    public MetaData getMetaData();

    public void setMetaData(MetaData metaData);
}
