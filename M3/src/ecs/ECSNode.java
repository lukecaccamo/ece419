package ecs;

import app_kvServer.IKVServer.CacheStrategy;

import shared.hashring.Hash;
import shared.hashring.HashRing;
import shared.messages.KVAdminMessage;
import shared.messages.IKVAdminMessage.ActionType;
import shared.communications.KVCommModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.apache.log4j.Logger;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

public class ECSNode implements IECSNode {
    private static String M2_PATH = System.getProperty("user.dir");
    private static String SSH_SCRIPT_PATH = M2_PATH + "/startKVServer.sh";
    private static Logger logger = Logger.getRootLogger();

    private String hashKey;
    private String nodeName;
    private String nodeHost;
    private int nodePort;
    private String[] nodeHashRange;
    private IECSNodeFlag flag;
    private int cacheSize;
    private CacheStrategy cacheStrategy;

    private ObjectMapper om;

    public ECSNode() {
        this.hashKey = null;
        this.nodeName = null;
        this.nodeHost = null;
        this.nodePort = 0;
        this.nodeHashRange = null;
        this.flag = null;
        this.cacheSize = 0;
        this.cacheStrategy = null;

        this.om = new ObjectMapper();
    }

    public ECSNode(String nodeName, String nodeHost, int nodePort) {
        this.hashKey = Hash.MD5(nodeHost + ":" + nodePort);
        this.nodeName = nodeName;
        this.nodeHost = nodeHost;
        this.nodePort = nodePort;
        this.nodeHashRange = new String[] { null, null };
        this.flag = IECSNodeFlag.SHUT_DOWN;
        this.cacheSize = 0;
        this.cacheStrategy = CacheStrategy.None;

        this.om = new ObjectMapper();
    }

    public boolean startKVServer(String zkHost, int zkPort) {
        if (this.flag != IECSNodeFlag.SHUT_DOWN)
            return false;
        String[] command = { SSH_SCRIPT_PATH, this.nodeName, this.nodeHost, Integer.toString(this.nodePort), zkHost,
                Integer.toString(zkPort) };

        try {
            ProcessBuilder kvServerProcessBuilder = new ProcessBuilder(command).inheritIO();
            Process kvServerProcess = kvServerProcessBuilder.start();
            this.flag = IECSNodeFlag.STOP;
        } catch (Exception e) {
            this.logger.error(e);
            e.printStackTrace();
        }
        return true;
    }

    public ECSNode setData(ActionType action, ZooKeeper zk, HashRing hashRing) {
        try {
            String zkNodeName = IECS.ZOOKEEPER_ADMIN_NODE_NAME + "/" + this.getNodeName();
            Stat zkStat = zk.exists(zkNodeName, false);
            KVAdminMessage adminMessage = new KVAdminMessage(action, this.getHashKey(), hashRing);
            String jsonMetaData = this.om.writeValueAsString(adminMessage).trim();
            byte[] jsonBytes = KVCommModule.toByteArray(jsonMetaData);
            zk.setData(zkNodeName, jsonBytes, zkStat.getVersion());
            return this.await(jsonMetaData, zk);
        } catch (JsonProcessingException | KeeperException | InterruptedException e) {
            this.logger.error(e);
            e.printStackTrace();
        }
        return null;
    }

    public ECSNode moveData(String moveTo, ZooKeeper zk, HashRing hashRing) {
        try {
            String zkNodeName = IECS.ZOOKEEPER_ADMIN_NODE_NAME + "/" + this.getNodeName();
            Stat zkStat = zk.exists(zkNodeName, false);
            KVAdminMessage adminMessage = new KVAdminMessage(ActionType.MOVE_DATA, moveTo, hashRing);
            String jsonMetaData = this.om.writeValueAsString(adminMessage).trim();
            byte[] jsonBytes = KVCommModule.toByteArray(jsonMetaData);
            zk.setData(zkNodeName, jsonBytes, zkStat.getVersion());
            return this.await(jsonMetaData, zk);
        } catch (JsonProcessingException | KeeperException | InterruptedException e) {
            this.logger.error(e);
            e.printStackTrace();
        }
        return null;
    }

    private ECSNode await(String oldState, ZooKeeper zk) {
        try {
            String zkNodeName = IECS.ZOOKEEPER_ADMIN_NODE_NAME + "/" + this.getNodeName();
            String newState = oldState;
            while (oldState.equals(newState)) {
                Stat zkStat = zk.exists(zkNodeName, false);
                newState = new String(zk.getData(zkNodeName, false, zkStat)).trim();
            }
            KVAdminMessage adminMessage = this.om.readValue(newState, KVAdminMessage.class);
            return (ECSNode) adminMessage.getMetaData().hashRing.get(this.getHashKey());
        } catch (JsonProcessingException | KeeperException | InterruptedException e) {
            this.logger.error(e);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @return the MD5 hash of the node
     */
    @Override
    public String getHashKey() {
        return this.hashKey;
    }

    /**
     * @return the name of the node (ie "Server 8.8.8.8")
     */
    @Override
    public String getNodeName() {
        return this.nodeName;
    }

    /**
     * @return the hostname of the node (ie "8.8.8.8")
     */
    @Override
    public String getNodeHost() {
        return this.nodeHost;
    }

    /**
     * @return the port number of the node (ie 8080)
     */
    @Override
    public int getNodePort() {
        return this.nodePort;
    }

    /**
     * @return array of two strings representing the low and high range of the
     *         hashes that the given node is responsible for
     */
    @Override
    public String[] getNodeHashRange() {
        return this.nodeHashRange;
    }

    @Override
    public void setNodeHashRange(String from, String to) {
        this.nodeHashRange = new String[] { from, to };
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

    public void setCacheStrategy(String cacheStrategy) {
        CacheStrategy cacheStrategyEnum = CacheStrategy.valueOf(cacheStrategy);
        this.cacheStrategy = cacheStrategyEnum;
    }

    @Override
    public CacheStrategy getCacheStrategy() {
        return cacheStrategy;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    @Override
    public int getCacheSize() {
        return cacheSize;
    }

    @JsonIgnore
    public ECSNode getServer() {
        return this;
    }
}
