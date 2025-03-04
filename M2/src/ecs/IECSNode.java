package ecs;

import app_kvServer.IKVServer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import shared.hashring.HashRing;

@JsonSerialize(as = ECSNode.class)
@JsonDeserialize(as = ECSNode.class)
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

    public int getCacheSize();

    public IKVServer.CacheStrategy getCacheStrategy();
}
