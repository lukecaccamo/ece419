package ecs;

import app_kvServer.IKVServer.CacheStrategy;
import shared.hashring.Hash;

import org.apache.log4j.Logger;

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

    public ECSNode() {
        this.hashKey = null;
        this.nodeName = null;
        this.nodeHost = null;
        this.nodePort = 0;
        this.nodeHashRange = null;
        this.flag = null;
        this.cacheSize = 0;
        this.cacheStrategy = null;
    }


    public ECSNode(String nodeName, String nodeHost, int nodePort) {
        this.hashKey = Hash.MD5(nodeHost + ":" + nodePort);
        this.nodeName = nodeName;
        this.nodeHost = nodeHost;
        this.nodePort = nodePort;
        this.nodeHashRange = new String[] {null, null};
        this.flag = IECSNodeFlag.SHUT_DOWN;
        this.cacheSize = 0;
        this.cacheStrategy = CacheStrategy.None;
    }

    public IECSNodeFlag startKVServer(String zkHost, int zkPort) {
        if (this.flag == IECSNodeFlag.SHUT_DOWN) {
            String[] command = {SSH_SCRIPT_PATH, this.nodeName, zkHost, Integer.toString(zkPort),
                    this.nodeHost, Integer.toString(this.nodePort), Integer.toString(this.cacheSize),
                    this.cacheStrategy.toString()};

            try {
                ProcessBuilder kvServerProcessBuilder = new ProcessBuilder(command).inheritIO();
                Process kvServerProcess = kvServerProcessBuilder.start();
                this.flag = IECSNodeFlag.START;
            } catch (Exception e) {
                logger.error(e);
            }
        }
        return this.flag;
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
     * @return array of two strings representing the low and high range of the hashes that the given
     *         node is responsible for
     */
    @Override
    public String[] getNodeHashRange() {
        return this.nodeHashRange;
    }

    @Override
    public void setNodeHashRange(String from, String to) {
        this.nodeHashRange = new String[] {from, to};
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
}
