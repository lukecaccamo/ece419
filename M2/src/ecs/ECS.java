package ecs;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;
import org.apache.zookeeper.*;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.data.Stat;
import app_kvServer.IKVServer;
import app_kvServer.IKVServer.ServerStateType;
import ecs.IECS;
import ecs.IECSNode.ECSNodeFlag;
import ecs.IECSNode.IECSNodeFlag;
import shared.metadata.Hash;
import shared.metadata.MetaData;

import com.fasterxml.jackson.databind.ObjectMapper;
import shared.communications.KVCommModule;

public class ECS implements IECS {
    private static String M2_PATH = System.getProperty("user.dir");
    private static String ZOOKEEPER_PATH = M2_PATH + "/zookeeper-3.4.11";
    private static String ZOOKEEPER_SCRIPT_PATH = ZOOKEEPER_PATH + "/bin/zkServer.sh";
    private static String ZOOKEEPER_CONF_PATH = ZOOKEEPER_PATH + "/conf/zoo_sample.cfg";
    private static Logger logger = Logger.getRootLogger();

    private Properties properties;
    private HashMap<String, IECSNode> servers;
    private TreeMap<String, IECSNode> hashRing;

    private ZooKeeper zookeeper;
    private CountDownLatch connected;

    private ObjectMapper objectMapper;

    private void initializeZooKeeper() {
        try {
            ProcessBuilder zookeeperProcessBuilder =
                    new ProcessBuilder(ZOOKEEPER_SCRIPT_PATH, "start", ZOOKEEPER_CONF_PATH)
                            .inheritIO();
            Process zookeeperProcess = zookeeperProcessBuilder.inheritIO().start();
            zookeeperProcess.waitFor();

            connected = new CountDownLatch(1);
            Watcher watcher = new Watcher() {
                @Override
                public void process(WatchedEvent e) {
                    if (e.getState() == KeeperState.SyncConnected) {
                        connected.countDown();
                    }
                }
            };
            this.zookeeper = new ZooKeeper("localhost", 3000, watcher);
            connected.await();
        } catch (Exception e) {
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
                this.servers.put(node.getHashKey(), node);
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private void clearZooKeeperNodes() {
        try {
            Stat s = zookeeper.exists("/AwaitNode", false);
            if (s != null) {
                List<String> list = zookeeper.getChildren("/AwaitNode", false);

                for (String nodeName : list) {
                    zookeeper.delete("/AwaitNode/" + nodeName, -1);
                }
                zookeeper.delete("/AwaitNode", -1);
            }

            if (this.hashRing != null) {
                Iterator it = this.hashRing.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, ECSNode> entry = (Map.Entry<String, ECSNode>) it.next();
                    IECSNode node = entry.getValue();
                    String zkNodeName = "/" + node.getNodeName();
                    try {
                        if (zookeeper.exists(zkNodeName, true) != null)
                            zookeeper.delete(zkNodeName, -1);
                    } catch (KeeperException e) {
                        logger.error(e);
                    }
                }
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error(e);
        }
    }

    private void initializeZooKeeperNodes() {
        try {
            if (this.zookeeper != null) {
                String path = "/AwaitNode";
                Stat s = this.zookeeper.exists(path, false);
                if (s == null) {
                    this.zookeeper.create(path, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,
                            CreateMode.PERSISTENT);
                }
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error(e);
        }
    }

    private boolean broadcast(IECSNodeFlag flag) {
        Iterator<Map.Entry<String, IECSNode>> it = this.hashRing.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, IECSNode> e = it.next();
            ECSNode node = (ECSNode) e.getValue();
            node.setFlag(flag);
            if (flag == IECSNodeFlag.SHUT_DOWN)
                it.remove();
        }
        return true;
    }

    private void updateRingRanges() {
        String prevHash = null;
        for (Map.Entry<String, IECSNode> entry : this.hashRing.entrySet()) {
            if (prevHash != null) {
                IECSNode n = entry.getValue();
                n.setNodeHashRange(prevHash, entry.getKey());
            }
            prevHash = entry.getKey();
        }
        String firstHash = hashRing.firstKey();
        IECSNode n = hashRing.get(firstHash);
        n.setNodeHashRange(prevHash, firstHash);
    }

    private void updateRingMetaData() {
        for (Map.Entry<String, IECSNode> entry : this.hashRing.entrySet()) {
            IECSNode n = entry.getValue();
            n.setMetaData();
        }
    }

    private void awaitMigration(String zkNodeName) {
        return null;
    }

    public ECS(String configFilePath) {
        this.properties = new Properties();
        this.servers = new HashMap<>();
        this.hashRing = new TreeMap<>();

        this.objectMapper = new ObjectMapper();

        this.initializeZooKeeper();
        this.initializeServers(configFilePath);
        this.clearZooKeeperNodes();
        this.initializeZooKeeperNodes();
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
        return this.broadcast(IECSNodeFlag.START);
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
        return this.broadcast(IECSNodeFlag.STOP);
    }

    /**
     * Stops all server instances and exits the remote processes.
     * 
     * @throws Exception some meaningfull exception on failure
     * @return true on success, false on failure
     */
    @Override
    public boolean shutdown() {
        return this.broadcast(IECSNodeFlag.SHUT_DOWN);
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
            // Adding new ECSNode into hashRing
            for (Map.Entry<String, IECSNode> entry : servers.entrySet()) {
                node = (ECSNode) entry.getValue();
                if (node.getFlag() == IECSNodeFlag.SHUT_DOWN)
                    break;
            }
            this.hashRing.put(node.getHashKey(), node);

            this.updateRingRanges();
            this.updateRingMetaData();
            node.startKVServer();
            awaitNodes(1, 3000);

            String zkNodeName = "/" + node.getNodeName();
            Stat zkStat = this.zookeeper.exists(zkNodeName, true);
            String serializedMetaData = objectMapper.writeValueAsString(node.getMetaData());
            byte[] jsonBytes = KVCommModule.toByteArray(serializedMetaData);
            System.out.println(serializedMetaData);
            this.zookeeper.setData(zkNodeName, jsonBytes, zkStat.getVersion());
            awaitMigration(zkNodeName);
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
        // TODO
        return null;
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
            List<String> awaitNodes = this.zookeeper.getChildren("/AwaitNode", true);
            if (awaitNodes.size() - this.hashRing.size() == count)
                return true;
        }
        return false;
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
            Iterator<Map.Entry<String, IECSNode>> it = this.hashRing.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, IECSNode> e = it.next();
                ECSNode node = (ECSNode) e.getValue();
                if (node.getNodeName().equals(name)) {
                    node.setFlag(IECSNodeFlag.SHUT_DOWN);
                    it.remove();
                }
            }
        }
        return false;
    }

    /**
     * Get a map of all nodes
     */
    @Override
    public Map<String, IECSNode> getNodes() {
        return this.hashRing;
    }

    /**
     * Get the specific node responsible for the given key
     */
    @Override
    public IECSNode getNodeByKey(String Key) {
        return this.hashRing.get(Key);
    }
}
