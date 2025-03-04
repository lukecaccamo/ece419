package ecs;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.zookeeper.*;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.data.Stat;
import java.util.concurrent.CountDownLatch;

import app_kvServer.IKVServer;
import app_kvServer.IKVServer.ServerStateType;
import ecs.IECS;
import ecs.IECSNode.IECSNodeFlag;
import shared.hashring.Hash;
import shared.hashring.HashRing;
import shared.messages.IKVAdminMessage.ActionType;

public class ECS implements IECS {
    public static final String ZOOKEEPER_HOST = "127.0.0.1";
    public static final int ZOOKEEPER_PORT = 2181;

    public static final String ZOOKEEPER_ADMIN_NODE_NAME = "/ECSAdmin";
    public static final String M2_PATH = System.getProperty("user.dir");
    public static final String ZOOKEEPER_PATH = M2_PATH + "/zookeeper-3.4.11";
    public static final String ZOOKEEPER_SCRIPT_PATH = ZOOKEEPER_PATH + "/bin/zkServer.sh";
    public static final String ZOOKEEPER_CONF_PATH = ZOOKEEPER_PATH + "/conf/zoo_sample.cfg";

    private static Logger logger = Logger.getRootLogger();

    private static String DEFAULT_CACHE_STRATEGY = "FIFO";
    private static int DEFAULT_CACHE_SIZE = 1;

    private Properties properties;
    public ArrayList<IECSNode> freeServers;
    public HashRing usedServers;

    private ZooKeeper zookeeper;
    private CountDownLatch connected;

    private void initializeZooKeeper() {
        try {
            ProcessBuilder zookeeperProcessBuilder =
                    new ProcessBuilder(ZOOKEEPER_SCRIPT_PATH, "start", ZOOKEEPER_CONF_PATH)
                            .inheritIO();
            Process zookeeperProcess = zookeeperProcessBuilder.inheritIO().start();
            zookeeperProcess.waitFor();

            this.connected = new CountDownLatch(1);
            Watcher watcher = new Watcher() {
                @Override
                public void process(WatchedEvent e) {
                    if (e.getState() == KeeperState.SyncConnected) {
                        connected.countDown();
                    }
                }
            };
            this.zookeeper = new ZooKeeper("localhost", 300000000, watcher);
            connected.await();
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private void initializeECSNode() {
        try {
            if (this.zookeeper != null && this.zookeeper.exists(ZOOKEEPER_ADMIN_NODE_NAME, false) != null) {
                List<String> list = this.zookeeper.getChildren(ZOOKEEPER_ADMIN_NODE_NAME, false);
                for (String nodeName : list) {
                    this.zookeeper.delete(ZOOKEEPER_ADMIN_NODE_NAME + "/" + nodeName, -1);
                }
                this.zookeeper.delete(ZOOKEEPER_ADMIN_NODE_NAME, -1);
            }
        } catch (KeeperException | InterruptedException e) {
            logger.info(e);
        }

        try {
            if (this.zookeeper != null) {
                this.zookeeper.create(ZOOKEEPER_ADMIN_NODE_NAME, new byte[0],
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error(e);
        }
    }

    private void initializeServers(String configFilePath) {
        try {
            InputStream configFile = new FileInputStream(configFilePath);
            this.properties.load(configFile);
            configFile.close();
            for (String key : this.properties.stringPropertyNames()) {
                String[] value = this.properties.getProperty(key).split("\\s+");
                String host = value[0];
                String port = value[1];

                IECSNode node = new ECSNode(key, host, Integer.parseInt(port));
                this.freeServers.add(node);
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private void resetZooKeeperNodes(Iterator nodes) {
        try {
            while (nodes.hasNext()) {
                IECSNode node = (ECSNode) nodes.next();
                String zkNodeName = "/" + node.getNodeName();
                try {
                    if (this.zookeeper.exists(zkNodeName, true) != null)
                        this.zookeeper.delete(zkNodeName, -1);
                } catch (KeeperException e) {
                    logger.info(e);
                }
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private boolean broadcast(ActionType action) {
        Iterator<Map.Entry<String, IECSNode>> it = this.usedServers.hashRing.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, IECSNode> e = it.next();
            ECSNode node = (ECSNode) e.getValue();
            node = node.setData(action, this.zookeeper, this.usedServers);
            e.setValue(node);

            if (node.getFlag() == IECSNodeFlag.SHUT_DOWN) {
                this.freeServers.add(node);
                it.remove();
            }
        }
        return true;
    }

    private void updateRingRanges() {
        if (this.usedServers.hashRing.size() < 1) return;

        Iterator<Map.Entry<String, IECSNode>> it = this.usedServers.hashRing.entrySet().iterator();
        String moveFrom = null;
        String prevHash = null;
        while (it.hasNext()) {
            Map.Entry<String, IECSNode> entry = it.next();
            if (prevHash != null) {
                IECSNode n = entry.getValue();
                n.setNodeHashRange(prevHash, entry.getKey());
            }
            prevHash = entry.getKey();
        }
        String firstHash = this.usedServers.hashRing.firstKey();
        IECSNode n = this.usedServers.hashRing.get(firstHash);
        n.setNodeHashRange(prevHash, firstHash);
    }

    public ECS(String configFilePath) {
        this.properties = new Properties();
        this.freeServers = new ArrayList<>();
        this.usedServers = new HashRing();

        this.initializeZooKeeper();
        this.initializeECSNode();
        this.initializeServers(configFilePath);
        this.resetZooKeeperNodes(this.freeServers.iterator());

        this.setupNodes(1, DEFAULT_CACHE_STRATEGY, DEFAULT_CACHE_SIZE);
    }

    /**
     * Starts the storage service by calling start() on all KVServer instances that participate in
     * the service.
     * 
     * @throws Exception some meaningfull exception on failure
     * @return true on success, false on failure
     */
    @Override
    public boolean start() {
        return this.broadcast(ActionType.START);
    }

    /**
     * Stops the service; all participating KVServers are stopped for processing client requests but
     * the processes remain running.
     * 
     * @throws Exception some meaningfull exception on failure
     * @return true on success, false on failure
     */
    @Override
    public boolean stop() {
        return this.broadcast(ActionType.STOP);
    }

    /**
     * Stops all server instances and exits the remote processes.
     * 
     * @throws Exception some meaningfull exception on failure
     * @return true on success, false on failure
     */
    @Override
    public boolean shutdown() {
        return this.broadcast(ActionType.SHUTDOWN);
    }

    /**
     * Create a new KVServer with the specified cache size and replacement strategy and add it to
     * the storage service at an arbitrary position.
     * 
     * @return name of new server
     */
    @Override
    public IECSNode addNode(String cacheStrategy, int cacheSize) {
        ECSNode node = null;
        try {
            if (this.freeServers.size() > 0) {
                node = (ECSNode) this.freeServers.remove(0);
                node.setCacheStrategy(cacheStrategy);
                node.setCacheSize(cacheSize);
                this.usedServers.hashRing.put(node.getHashKey(), node);

                this.updateRingRanges();
                node.startKVServer(ZOOKEEPER_HOST, ZOOKEEPER_PORT);
                awaitNodes(1, 3000);

                node = node.setData(ActionType.INIT, this.zookeeper, this.usedServers);
                this.usedServers.hashRing.put(node.getHashKey(), node);

                this.broadcast(ActionType.UPDATE);

                ECSNode from = (ECSNode) this.usedServers.getPred(node.getHashKey());
                from.setData(ActionType.LOCK_WRITE, this.zookeeper, this.usedServers);
                node.setData(ActionType.LOCK_WRITE, this.zookeeper, this.usedServers);

                from.moveData(node.getHashKey(), this.zookeeper, this.usedServers);

                from.setData(ActionType.UNLOCK_WRITE, this.zookeeper, this.usedServers);
                node.setData(ActionType.UNLOCK_WRITE, this.zookeeper, this.usedServers);
            }
        } catch (Exception e) {
            logger.error(e);
        }
        return node;
    }

    /**
     * Randomly choose <numberOfNodes> servers from the available machines and start the KVServer by
     * issuing an SSH call to the respective machine. This call launches the storage server with the
     * specified cache size and replacement strategy. For simplicity, locate the KVServer.jar in the
     * same directory as the ECS. All storage servers are initialized with the metadata and any
     * persisted data, and remain in state stopped. NOTE: Must call setupNodes before the SSH calls
     * to start the servers and must call awaitNodes before returning
     * 
     * @return set of strings containing the names of the nodes
     */
    @Override
    public Collection<IECSNode> addNodes(int count, String cacheStrategy, int cacheSize) {
        Collection<IECSNode> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            nodes.add(this.addNode(cacheStrategy, cacheSize));
        }
        return nodes;
    }

    /**
     * Sets up `count` servers with the ECS (in this case Zookeeper)
     * 
     * @return array of strings, containing unique names of servers
     */
    @Override
    public Collection<IECSNode> setupNodes(int count, String cacheStrategy, int cacheSize) {
        ArrayList<IECSNode> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (this.freeServers.size() == 0)
                break;
            ECSNode node = (ECSNode) this.freeServers.remove(0);
            node.setCacheStrategy(cacheStrategy);
            node.setCacheSize(cacheSize);
            this.usedServers.hashRing.put(node.getHashKey(), node);
        }

        this.updateRingRanges();

        for (Map.Entry<String, IECSNode> entry : this.usedServers.hashRing.entrySet()) {
            ECSNode node = (ECSNode) entry.getValue();
            node.startKVServer(ZOOKEEPER_HOST, ZOOKEEPER_PORT);
            nodes.add(node);
        }

        try {
            awaitNodes(this.usedServers.hashRing.size(), 3000);
        } catch (Exception e) {
            logger.error(e);
        }

        this.broadcast(ActionType.INIT);
        return nodes;
    }

    /**
     * Wait for all nodes to report status or until timeout expires
     * 
     * @param count   number of nodes to wait for
     * @param timeout the timeout in milliseconds
     * @return true if all nodes reported successfully, false otherwise
     */
    @Override
    public boolean awaitNodes(int count, int timeout) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            List<String> zkNodes = this.zookeeper.getChildren(ZOOKEEPER_ADMIN_NODE_NAME, true);
            if (zkNodes.size() == this.usedServers.hashRing.size())
                return true;
        }
        throw new Exception("`awaitNodes(" + String.valueOf(count) + ", " + String.valueOf(timeout)
                + ")` Timeout.");
    }

    /**
     * Removes nodes with names matching the nodeNames array
     * 
     * @param nodeNames names of nodes to remove
     * @return true on success, false otherwise
     */
    @Override
    public boolean removeNodes(Collection<String> nodeNames) {
        for (String name : nodeNames) {
            Iterator<Map.Entry<String, IECSNode>> it =
                    this.usedServers.hashRing.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, IECSNode> e = it.next();
                String removed = e.getKey();
                ECSNode node = (ECSNode) e.getValue();
                if (node.getNodeName().equals(name)) {
                    ECSNode to = (ECSNode) this.usedServers.getSucc(node.getHashKey());

                    it.remove();
                    updateRingRanges();

                    to.setData(ActionType.LOCK_WRITE, this.zookeeper, this.usedServers);
                    node.setData(ActionType.LOCK_WRITE, this.zookeeper, this.usedServers);

                    node.moveData(to.getHashKey(), this.zookeeper, this.usedServers);

                    to.setData(ActionType.UNLOCK_WRITE, this.zookeeper, this.usedServers);
                    node.setData(ActionType.UNLOCK_WRITE, this.zookeeper, this.usedServers);

                    node = node.setData(ActionType.SHUTDOWN, this.zookeeper, this.usedServers);
                    this.freeServers.add(node);

                    this.broadcast(ActionType.UPDATE);
                }
            }
        }
        return true;
    }

    /**
     * Get a map of all nodes
     */
    @Override
    public Map<String, IECSNode> getNodes() {
        return this.usedServers.hashRing;
    }

    /**
     * Get the specific node responsible for the given key
     */
    @Override
    public IECSNode getNodeByKey(String Key) {
        return this.usedServers.hashRing.get(Key);
    }
}
