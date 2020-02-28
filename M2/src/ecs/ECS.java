package ecs;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;
import org.apache.zookeeper.*;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.data.Stat;

import ecs.IECS;

public class ECS implements IECS {
    private static String ZOOKEEPER_PATH = System.getProperty("user.dir") + "/zookeeper-3.4.11";
    private static String ZOOKEEPER_SCRIPT_PATH = ZOOKEEPER_PATH + "/bin/zkServer.sh";
    private static String ZOOKEEPER_CONF_PATH = ZOOKEEPER_PATH + "/conf/zoo_sample.cfg";
    private static Logger logger = Logger.getRootLogger();

    private Properties properties;
    private ZooKeeper zookeeper;
    private CountDownLatch connected;

    public ECS(String configFilePath) {
        this.properties = new Properties();
        try {
            InputStream configFile = new FileInputStream(configFilePath);
            this.properties.load(configFile);
            configFile.close();
            for (String key : this.properties.stringPropertyNames()) {
                String[] value = this.properties.getProperty(key).split("\\s+");
                String hostName = value[0];
                int port = Integer.parseInt(value[1]);
            }

            ProcessBuilder zookeeperProcessBuilder = new ProcessBuilder(ZOOKEEPER_SCRIPT_PATH, "start", ZOOKEEPER_CONF_PATH);
            Process zooKeeperProcess = zookeeperProcessBuilder.start();
            zooKeeperProcess.waitFor();

            connected = new CountDownLatch(1);
            this.zookeeper = new ZooKeeper("localhost", 3000000, new Watcher() {
                @Override
                public void process(WatchedEvent e) {
                    if (e.getState() == KeeperState.SyncConnected) {
                        connected.countDown();
                    }
                }
            });
            connected.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts the storage service by calling start() on all KVServer instances that participate in
     * the service.\
     * 
     * @throws Exception some meaningfull exception on failure
     * @return true on success, false on failure
     */
    @Override
    public boolean start() {
        // TODO
        return false;
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
        // TODO
        return false;
    }

    /**
     * Stops all server instances and exits the remote processes.
     * 
     * @throws Exception some meaningfull exception on failure
     * @return true on success, false on failure
     */
    @Override
    public boolean shutdown() {
        // TODO
        return false;
    }

    /**
     * Create a new KVServer with the specified cache size and replacement strategy and add it to
     * the storage service at an arbitrary position.
     * 
     * @return name of new server
     */
    @Override
    public IECSNode addNode(String cacheStrategy, int cacheSize) {
        // TODO
        return null;
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
        // TODO
        return null;
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
        // TODO
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
        // TODO
        return false;
    }

    /**
     * Get a map of all nodes
     */
    @Override
    public Map<String, IECSNode> getNodes() {
        // TODO
        return null;
    }

    /**
     * Get the specific node responsible for the given key
     */
    @Override
    public IECSNode getNodeByKey(String Key) {
        // TODO
        return null;
    }
}
