package testing;

import app_kvServer.IKVServer;
import app_kvServer.KVServer;
import app_kvServer.IKVServer.CacheStrategy;
import ecs.ECSNode;
import ecs.IECSNode.IECSNodeFlag;

import org.junit.Test;

import client.KVStore;
import junit.framework.TestCase;
import shared.hashring.Hash;
import shared.messages.IKVAdminMessage;
import shared.messages.KVAdminMessage;
import shared.messages.KVMessage;
import shared.messages.KVMessage.StatusType;
import shared.hashring.HashRing;
import ecs.ECSNode;
import shared.messages.KVSimpleMessage;
import shared.hashring.HashRing;

import java.io.File;
import java.math.BigInteger;
import java.util.TreeMap;

public class ECSTest extends TestCase {
    private ECSNode kvServer1, kvServer2;
    private String serverHash1, serverHash2;
    private KVStore kvClient1, kvClient2;
    private int port1, port2;
    private ECSNode node1, node2;
    private HashRing metadata;
    private int NUM_OPS = 5000;
    private int CACHE_SIZE = 1000;
    private String POLICY = "FIFO";
    private ECSNode node = null;

    public void setUp() {
        Exception ex = null;

        String nodeKey = AllTests.ecs.getNodeKeys()[0];
        kvServer1 = (ECSNode) AllTests.ecs.getNodeByKey(nodeKey);
        kvClient1 = new KVStore(kvServer1.getNodeHost(), kvServer1.getNodePort());

        nodeKey = AllTests.ecs.getNodeKeys()[1];
        kvServer2 = (ECSNode) AllTests.ecs.getNodeByKey(nodeKey);
        kvClient2 = new KVStore(kvServer2.getNodeHost(), kvServer2.getNodePort());

        try {
            kvClient1.connect();
            kvClient2.connect();
        } catch (Exception e) {
            ex = e;
        }

        assertNull(ex);
    }

    public void tearDown() {
        AllTests.resetDB();
    }

    @Test
	public void testStop() {
        AllTests.ecs.stop();

        assertEquals(IECSNodeFlag.STOP ,kvServer1.getFlag());
        assertEquals(IECSNodeFlag.STOP, kvServer2.getFlag());
	}

    @Test
	public void testStart() {
        AllTests.ecs.start();

        assertEquals(IECSNodeFlag.START ,kvServer1.getFlag());
        assertEquals(IECSNodeFlag.START, kvServer2.getFlag());
    }
}
