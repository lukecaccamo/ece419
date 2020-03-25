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

import app_kvECS.ECSClient;

public class ECSNode implements IECSNode {
    private static final String M2_PATH = System.getProperty("user.dir");
    private static final String SSH_START_PATH = M2_PATH + "/startKVServer.sh";
    private static final String SSH_STOP_PATH = M2_PATH + "/stopKVServer.sh";
    private static final Logger logger = Logger.getRootLogger();

    private final ZooKeeper zk;
    private final String zkNodeName;
    private final boolean debug;
    private final ObjectMapper om = new ObjectMapper();

    private final String hashKey;
    private final String nodeName;
    private final String nodeHost;
    private final int nodePort;

    private String[] nodeHashRange;
    private IECSNodeFlag flag;
    private int cacheSize;
    private CacheStrategy cacheStrategy;

    public ECSNode() {
        this.zk = null;
        this.zkNodeName = null;
        this.debug = false;

        this.hashKey = null;
        this.nodeName = null;
        this.nodeHost = null;
        this.nodePort = 0;

        this.nodeHashRange = new String[] { null, null };
        this.flag = IECSNodeFlag.SHUT_DOWN;
        this.cacheSize = 0;
        this.cacheStrategy = CacheStrategy.None;
    }

    public ECSNode(String nodeName, String nodeHost, int nodePort) {
        this.zk = null;
        this.zkNodeName = null;
        this.debug = false;

        this.hashKey = Hash.MD5(nodeHost + ":" + nodePort);
        this.nodeName = nodeName;
        this.nodeHost = nodeHost;
        this.nodePort = nodePort;

        this.nodeHashRange = new String[] { null, null };
        this.flag = IECSNodeFlag.SHUT_DOWN;
        this.cacheSize = 0;
        this.cacheStrategy = CacheStrategy.None;
    }

    public ECSNode(String nodeName, String nodeHost, int nodePort, ZooKeeper zk, boolean debug) {
        this.zk = zk;
        this.zkNodeName = IECS.ZOOKEEPER_ADMIN_NODE_NAME + "/" + nodeName;
        this.debug = debug;

        this.hashKey = Hash.MD5(nodeHost + ":" + nodePort);
        this.nodeName = nodeName;
        this.nodeHost = nodeHost;
        this.nodePort = nodePort;

        this.nodeHashRange = new String[] { null, null };
        this.flag = IECSNodeFlag.SHUT_DOWN;
        this.cacheSize = 0;
        this.cacheStrategy = CacheStrategy.None;

        this.stopKVServer();
    }

    public ECSNode resetNode() {
        this.nodeHashRange = new String[] { null, null };
        this.flag = IECSNodeFlag.SHUT_DOWN;
        this.cacheSize = 0;
        this.cacheStrategy = CacheStrategy.None;
        return this;
    }

    public boolean startKVServer() {
        if (this.flag != IECSNodeFlag.SHUT_DOWN)
            return false;
        String[] command = { SSH_START_PATH, this.nodeName, this.nodeHost, Integer.toString(this.nodePort),
                IECS.ZOOKEEPER_HOST, Integer.toString(IECS.ZOOKEEPER_PORT) };

        try {
            ProcessBuilder kvServerProcessBuilder = new ProcessBuilder(command);
            if (this.debug)
                kvServerProcessBuilder.inheritIO();
            Process kvServerProcess = kvServerProcessBuilder.start();
            this.flag = IECSNodeFlag.STOP;
        } catch (Exception e) {
            this.logger.error(e);
            e.printStackTrace();
        }
        return true;
    }

    public boolean stopKVServer() {
        if (this.flag != IECSNodeFlag.SHUT_DOWN)
            return false;
        String[] command = { SSH_STOP_PATH, this.nodeHost, Integer.toString(this.nodePort) };

        try {
            ProcessBuilder kvServerProcessBuilder = new ProcessBuilder(command);
            if (this.debug)
                kvServerProcessBuilder.inheritIO();
            Process kvServerProcess = kvServerProcessBuilder.start();
            kvServerProcess.waitFor();
        } catch (Exception e) {
            this.logger.error(e);
            e.printStackTrace();
        }
        return true;
    }

    public Stat zkStat() {
        Stat zkStat = null;
        try {
            zkStat = this.zk.exists(zkNodeName, false);
        } catch (KeeperException | InterruptedException e) {
            this.logger.warn(e);
            e.printStackTrace();
        }
        return zkStat;
    }

    public ECSNode setData(ActionType action, HashRing hashRing) {
        try {
            KVAdminMessage adminMessage = new KVAdminMessage(action, this.getHashKey(), hashRing);
            String jsonMetaData = this.om.writeValueAsString(adminMessage).trim();
            byte[] jsonBytes = KVCommModule.toByteArray(jsonMetaData);

            Stat zkStat = this.zkStat();

            if (zkStat != null) {
                this.zk.setData(this.zkNodeName, jsonBytes, zkStat.getVersion());
                return this.await(jsonMetaData);
            }
        } catch (JsonProcessingException | KeeperException | InterruptedException e) {
            this.logger.error(e);
            e.printStackTrace();
        }
        return this;
    }

    public ECSNode moveData(String moveTo, HashRing hashRing) {
        try {
            KVAdminMessage adminMessage = new KVAdminMessage(ActionType.MOVE_DATA, moveTo, hashRing);
            String jsonMetaData = this.om.writeValueAsString(adminMessage).trim();
            byte[] jsonBytes = KVCommModule.toByteArray(jsonMetaData);

            Stat zkStat = this.zkStat();

            if (zkStat != null) {
                this.zk.setData(this.zkNodeName, jsonBytes, zkStat.getVersion());
                return this.await(jsonMetaData);
            }
        } catch (JsonProcessingException | KeeperException | InterruptedException e) {
            this.logger.error(e);
            e.printStackTrace();
        }
        return this;
    }

    public ECSNode moveReplicas(String moveTo, HashRing hashRing) {
        try {
            KVAdminMessage adminMessage = new KVAdminMessage(ActionType.MOVE_REPLICAS, moveTo, hashRing);
            String jsonMetaData = this.om.writeValueAsString(adminMessage).trim();
            byte[] jsonBytes = KVCommModule.toByteArray(jsonMetaData);

            Stat zkStat = this.zkStat();

            if (zkStat != null) {
                this.zk.setData(this.zkNodeName, jsonBytes, zkStat.getVersion());
                return this.await(jsonMetaData);
            }
        } catch (JsonProcessingException | KeeperException | InterruptedException e) {
            this.logger.error(e);
            e.printStackTrace();
        }
        return this;
    }

    private ECSNode await(String oldState) {
        try {
            String newState = oldState;
            while (oldState.equals(newState)) {
                Stat zkStat = this.zkStat();
                newState = new String(this.zk.getData(this.zkNodeName, false, zkStat)).trim();
            }

            KVAdminMessage adminMessage = this.om.readValue(newState, KVAdminMessage.class);
            if (this.debug)
                ECSClient.prompt.print(adminMessage.toString());

            ECSNode updatedNode = adminMessage.getMetaData().hashRing.get(this.getHashKey());
            this.nodeHashRange = updatedNode.getNodeHashRange();
            this.flag = updatedNode.getFlag();
            this.cacheSize = updatedNode.getCacheSize();
            this.cacheStrategy = updatedNode.getCacheStrategy();

            if (this.getFlag().equals(IECSNodeFlag.SHUT_DOWN))
                this.zk.delete(this.zkNodeName, -1);

            return this;
        } catch (JsonProcessingException | KeeperException | InterruptedException e) {
            this.logger.error(e);
            e.printStackTrace();
        }
        return this;
    }

    public String toString() {
        return String.format(" %s(%s:%d) flag: %s range: (%s,%s) cache: %s(%d)", this.getNodeName(), this.getNodeHost(),
                this.getNodePort(), this.getFlag().toString(), this.getNodeHashRange()[0], this.getNodeHashRange()[1],
                this.getCacheStrategy().toString(), this.getCacheSize());
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

    @Override
    public CacheStrategy getCacheStrategy() {
        return cacheStrategy;
    }

    public void setCacheStrategy(String cacheStrategy) {
        CacheStrategy cacheStrategyEnum = CacheStrategy.valueOf(cacheStrategy);
        this.cacheStrategy = cacheStrategyEnum;
    }

    @Override
    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    @JsonIgnore
    public ECSNode getServer() {
        return this;
    }
}
