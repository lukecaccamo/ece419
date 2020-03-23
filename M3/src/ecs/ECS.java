package ecs;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.zookeeper.*;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.data.Stat;
import java.util.concurrent.CountDownLatch;

import app_kvServer.IKVServer;
import ecs.IECS.*;
import ecs.IECSNode.IECSNodeFlag;
import shared.hashring.HashRing;
import shared.messages.IKVAdminMessage.ActionType;

public class ECS implements IECS {
    private static Logger logger = Logger.getRootLogger();
    private final boolean debug;
    private final String configFilePath;

    private Properties properties;
    public HashRing usedServers;
    public PriorityQueue<ECSNode> freeServers;

    private ZooKeeper zk;
    private CountDownLatch connected;

    public ECS(String configFilePath, boolean debug) {
        this.debug = debug;
        this.configFilePath = configFilePath;

        this.properties = new Properties();
        this.usedServers = new HashRing();
        Comparator<ECSNode> serverNameComparator = new Comparator<ECSNode>() {
            @Override
            public int compare(ECSNode a, ECSNode b) {
                return a.getNodeName().compareTo(b.getNodeName());
            }
        };
        this.freeServers = new PriorityQueue<ECSNode>(serverNameComparator);

        try {
            this.initializeZooKeeper();
            this.initializeAdminNode();
            this.initializeECSNodes();
        } catch (Exception e) {
            this.logger.error(e);
            e.printStackTrace();
        }
    }

    @Override
    public boolean start() {
        return this.broadcast(ActionType.START);
    }

    @Override
    public boolean stop() {
        return this.broadcast(ActionType.STOP);
    }

    @Override
    public boolean shutdown() {
        return this.broadcast(ActionType.SHUTDOWN);
    }

    @Override
    public IECSNode addNode(String cacheStrategy, int cacheSize) {
        ECSNode node = null;
        if (this.freeServers.isEmpty())
            return null;
        try {
            node = this.freeServers.remove();
            node.setCacheStrategy(cacheStrategy);
            node.setCacheSize(cacheSize);
            this.usedServers.hashRing.put(node.getHashKey(), node);

            this.updateRingRanges(null);
            node.startKVServer();
            awaitNodes(1, 10000);

            node = node.setData(ActionType.INIT, this.usedServers);
            this.usedServers.hashRing.put(node.getHashKey(), node);

            this.broadcast(ActionType.UPDATE);

            ECSNode succ = this.usedServers.getSucc(node.getHashKey());
            succ.setData(ActionType.LOCK_WRITE, this.usedServers);
            node.setData(ActionType.LOCK_WRITE, this.usedServers);

            // step 0
            succ.moveData(node.getHashKey(), this.usedServers);

            succ.setData(ActionType.UNLOCK_WRITE, this.usedServers);
            node.setData(ActionType.UNLOCK_WRITE, this.usedServers);

            // step 1, 2
            ECSNode pred = this.usedServers.getPred(node.getHashKey());
            ECSNode pred2 = this.usedServers.getPred(pred.getHashKey());
            
            // ONLY SEND RANGE OF PRED
            pred.moveReplicas(node.getHashKey(), this.usedServers);
            // ONLY SEND THE RANGE OF PRED 2
            pred2.moveReplicas(node.getHashKey(), this.usedServers);

        } catch (Exception e) {
            this.logger.error(e);
            e.printStackTrace();
        }
        return node;
    }

    @Override
    public Collection<IECSNode> addNodes(int count, String cacheStrategy, int cacheSize) {
        Collection<IECSNode> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            nodes.add(this.addNode(cacheStrategy, cacheSize));
        }
        return nodes;
    }

    @Override
    public Collection<IECSNode> setupNodes(int count, String cacheStrategy, int cacheSize) {
        ArrayList<IECSNode> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (this.freeServers.isEmpty())
                break;
            ECSNode node = this.freeServers.remove();
            node.setCacheStrategy(cacheStrategy);
            node.setCacheSize(cacheSize);
            this.usedServers.hashRing.put(node.getHashKey(), node);
        }

        this.updateRingRanges(null);

        for (Map.Entry<String, ECSNode> entry : this.usedServers.hashRing.entrySet()) {
            ECSNode node = entry.getValue();
            node.startKVServer();
            nodes.add(node);
        }

        try {
            awaitNodes(this.usedServers.hashRing.size(), 10000);
        } catch (Exception e) {
            this.logger.error(e);
            e.printStackTrace();
        }

        this.broadcast(ActionType.INIT);
        return nodes;
    }

    @Override
    public boolean awaitNodes(int count, int timeout) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            List<String> zkNodes = this.zk.getChildren(IECS.ZOOKEEPER_ADMIN_NODE_NAME, true);
            if (zkNodes.size() == this.usedServers.hashRing.size())
                return true;
        }
        throw new Exception("`awaitNodes(" + String.valueOf(count) + ", " + String.valueOf(timeout) + ")` Timeout.");
    }

    @Override
    public boolean removeNodes(Collection<String> nodeNames) {
        for (String name : nodeNames) {
            Iterator<Map.Entry<String, ECSNode>> it = this.usedServers.hashRing.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, ECSNode> e = it.next();
                String removed = e.getKey();
                ECSNode node = e.getValue();
                if (node.getNodeName().equals(name)) {
                    // NODE = PRED
                    // SUCC = SERVER 2
                    ECSNode succ = this.usedServers.getSucc(node.getHashKey());
                    // PRED = SERVER 3
                    ECSNode pred = this.usedServers.getPred(succ.getHashKey());
                    // PRED2 = SERVER4
                    ECSNode pred2 = this.usedServers.getPred(pred.getHashKey());
                    // PRED3 = SERVER 5
                    ECSNode pred3 = this.usedServers.getPred(pred2.getHashKey());
                    // SUCC2 = SERVER 1
                    ECSNode succ2 = this.usedServers.getSucc(succ.getHashKey());
                    // succ3 = SERVER 5
                    ECSNode succ3 = this.usedServers.getSucc(succ2.getHashKey());   

                    succ.setData(ActionType.LOCK_WRITE, this.usedServers);
                    node.setData(ActionType.LOCK_WRITE, this.usedServers);

                    // 0.) move node's ranged data to succ
                    // NOT NECESSARY SINCE SUCCESSOR WOULD ALREADY HAVE DATA
                    // CORRECT
                    pred.moveReplicas(succ.getHashKey(), this.usedServers);

                    /*
                    System.out.println("succ: " + succ.getHashKey());
                    System.out.println("succ2: " + succ2.getHashKey());
                    System.out.println("succ3: " + succ3.getHashKey());
                    System.out.println("node: " + node.getHashKey());
                    System.out.println("pred: " + pred.getHashKey());
                    System.out.println("pred2: " + pred2.getHashKey());
                    System.out.println("pred3: " + pred3.getHashKey());*/

                    succ.setData(ActionType.UNLOCK_WRITE, this.usedServers);
                    node.setData(ActionType.UNLOCK_WRITE, this.usedServers);

                    updateRingRanges(node);
                    this.broadcast(ActionType.UPDATE);

                    // 1.) pred pred to succ
                    // right now goes to server4
                    // succ needs to be server 2
                    // CORRECT
                    pred3.moveReplicas(succ.getHashKey(), this.usedServers);

                    // 2.) move data from succ to succ2 and succ3 -- ALL CORRECT
                    // just the hash range doesn't cover server 3's
                    // right now goes to old server2
                    // succ = SERVER1
                    pred.moveReplicas(succ2.getHashKey(), this.usedServers);
                    // right now goes to old server2
                    pred.moveReplicas(succ3.getHashKey(), this.usedServers);
                    
                    node = node.setData(ActionType.SHUTDOWN, this.usedServers);
                    this.freeServers.add(node);

                    it.remove();
                    updateRingRanges(null);
                    this.broadcast(ActionType.UPDATE);
                }
            }
        }
        return true;
    }

    @Override
    public Map<String, IECSNode> getNodes() {
        Map<String, IECSNode> nodes = new TreeMap<String, IECSNode>();
        for (String key : this.usedServers.getHashRing().keySet()) {
            nodes.put(key, this.usedServers.getHashRing().get(key));
        }
        return nodes;
    }

    public String[] getNodeKeys() {
        return this.usedServers.hashRing.keySet().toArray(new String[this.usedServers.hashRing.size()]);
    }

    @Override
    public IECSNode getNodeByKey(String Key) {
        return this.usedServers.hashRing.get(Key);
    }

    private void initializeZooKeeper() throws Exception {
        ProcessBuilder zkProcessBuilder = new ProcessBuilder(IECS.ZOOKEEPER_SCRIPT_PATH, "restart",
                IECS.ZOOKEEPER_CONF_PATH);
        if (this.debug)
            zkProcessBuilder.inheritIO();
        Process zkProcess = zkProcessBuilder.start();
        zkProcess.waitFor();
        this.connected = new CountDownLatch(1);
        Watcher watcher = new Watcher() {
            @Override
            public void process(WatchedEvent e) {
                if (e.getState() == KeeperState.SyncConnected)
                    connected.countDown();
            }
        };
        this.zk = new ZooKeeper(IECS.ZOOKEEPER_HOST, 10000, watcher);
        connected.await();
    }

    private void initializeAdminNode() throws Exception {
        if (this.zk == null)
            throw new Exception("ZooKeeper uninitialized!");

        try {
            if (this.zk.exists(IECS.ZOOKEEPER_ADMIN_NODE_NAME, false) != null) {
                List<String> list = this.zk.getChildren(IECS.ZOOKEEPER_ADMIN_NODE_NAME, false);
                for (String nodeName : list) {
                    this.zk.delete(IECS.ZOOKEEPER_ADMIN_NODE_NAME + "/" + nodeName, -1);
                }
                this.zk.delete(IECS.ZOOKEEPER_ADMIN_NODE_NAME, -1);
            }
        } catch (KeeperException | InterruptedException e) {
            this.logger.info(e);
        }

        this.zk.create(IECS.ZOOKEEPER_ADMIN_NODE_NAME, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    private void initializeECSNodes() throws Exception {
        InputStream configFile = new FileInputStream(this.configFilePath);
        this.properties.load(configFile);
        configFile.close();
        for (String key : this.properties.stringPropertyNames()) {
            String[] value = this.properties.getProperty(key).split("\\s+");
            String host = value[0];
            String port = value[1];

            ECSNode node = new ECSNode(key, host, Integer.parseInt(port), this.zk, this.debug);
            this.freeServers.add(node);
        }
    }

    private boolean broadcast(ActionType action) {
        Iterator<Map.Entry<String, ECSNode>> it = this.usedServers.hashRing.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ECSNode> e = it.next();
            ECSNode node = e.getValue();
            node = node.setData(action, this.usedServers);
            e.setValue(node);

            if (node.getFlag() == IECSNodeFlag.SHUT_DOWN) {
                this.freeServers.add(node);
                it.remove();
            }
        }
        return true;
    }

    private void updateRingRanges(ECSNode neglected) {
        if (this.usedServers.hashRing.size() < 1)
            return;

        Set<Map.Entry<String, ECSNode>> entries = this.usedServers.getHashRing().entrySet();
        if (neglected != null)
            entries.remove(neglected.getHashKey());

        Iterator<Map.Entry<String, ECSNode>> it = entries.iterator();
        String moveFrom = null;
        String prevHash = null;
        while (it.hasNext()) {
            Map.Entry<String, ECSNode> entry = it.next();
            if (prevHash != null) {
                ECSNode n = entry.getValue();
                n.setNodeHashRange(prevHash, entry.getKey());
            }
            prevHash = entry.getKey();
        }
        String firstHash = this.usedServers.hashRing.firstKey();
        ECSNode n = this.usedServers.hashRing.get(firstHash);
        n.setNodeHashRange(prevHash, firstHash);
    }
}
