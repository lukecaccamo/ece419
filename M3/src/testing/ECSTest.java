package testing;

import app_kvServer.IKVServer.CacheStrategy;
import client.KVStore;
import ecs.ECSNode;
import ecs.IECSNode;
import ecs.IECSNode.IECSNodeFlag;
import junit.framework.TestCase;
import org.junit.Test;
import shared.hashring.Hash;
import shared.messages.KVMessage;
import shared.messages.KVMessage.StatusType;
import shared.messages.KVSimpleMessage;

import java.util.ArrayList;
import java.util.Collection;

import static java.lang.Thread.sleep;

public class ECSTest extends TestCase {
    private ECSNode kvServer1, kvServer2;
    private String serverHash1, serverHash2;
    private KVStore kvClient1, kvClient2;

    public void setUp() {
        AllTests.resetECS();
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
        AllTests.resetECS();
    }

    @Test
	public void testStop() {
        AllTests.ecs.stop();
        Exception ex = null;

        assertEquals(IECSNodeFlag.STOP ,kvServer1.getFlag());
        assertEquals(IECSNodeFlag.STOP, kvServer2.getFlag());

        KVMessage response1 = null;
        KVMessage response2 = null;
        try {
            response1 = kvClient1.get("key");
            response2 = kvClient2.get("key");
        } catch (Exception e) {
            ex = e;
            e.printStackTrace();
        }

        assertNull(ex);
        assertEquals(StatusType.SERVER_STOPPED, response1.getStatus());
        assertEquals(StatusType.SERVER_STOPPED, response2.getStatus());
	}

    @Test
	public void testStart() {
        AllTests.ecs.start();
        Exception ex = null;

        assertEquals(IECSNodeFlag.START ,kvServer1.getFlag());
        assertEquals(IECSNodeFlag.START, kvServer2.getFlag());

        KVMessage response1 = null;
        KVMessage response2 = null;
        try {
            response1 = kvClient1.get("key");
            response2 = kvClient2.get("key");
        } catch (Exception e) {
            ex = e;
            e.printStackTrace();
        }

        assertNull(ex);
        assertEquals(StatusType.GET_ERROR, response1.getStatus());
        assertEquals(StatusType.GET_ERROR, response2.getStatus());
    }

    @Test
	public void testShutdown() {
        AllTests.ecs.shutdown();
        Exception ex = null;

        assertEquals(IECSNodeFlag.SHUT_DOWN ,kvServer1.getFlag());
        assertEquals(IECSNodeFlag.SHUT_DOWN, kvServer2.getFlag());

        KVMessage response1 = null;
        KVMessage response2 = null;
        try {
            response1 = kvClient1.put("key", "value");
        } catch (Exception e) {
            ex = e;
        }
        assertNotNull(ex);
        assertNull(response1);

        try {
			response2 = kvClient2.put("key", "value");
		} catch (Exception e) {
			ex = e;
        }
        assertNotNull(ex);
        assertNull(response2);

        AllTests.resetECS();

        ex = null;
        try {
            kvClient1.connect();
            kvClient2.connect();
        } catch (Exception e) {
            ex = e;
            e.printStackTrace();
        }
        assertNull(ex);
    }

    @Test
	public void testRemoveNode() throws InterruptedException {
        Exception ex = null;

        KVSimpleMessage toServer2 = null;
        String key2 = "abc";
        String value2 = "hashes to kvServer2";
        assertEquals(Hash.MD5(key2), "900150983cd24fb0d6963f7d28e17f72");
        assertEquals("68d77f380f7e215676838eca9b90ebb8", kvServer1.getNodeHashRange()[1]);
        assertEquals("ee935ceeea3cd07c8937e5ad812759a8", kvServer2.getNodeHashRange()[1]);

        try {
            toServer2 = kvClient1.put(key2, value2);
        } catch (Exception e) {
            ex = e;
            e.printStackTrace();
        }
        assertNull(ex);
        assertEquals(StatusType.PUT_SUCCESS, toServer2.getStatus());
        assertEquals(kvServer2.getNodePort(), kvClient1.getServerPort());

        Collection<String> toRemove = new ArrayList<String>();
        toRemove.add(kvServer2.getNodeName());
        AllTests.ecs.removeNodes(toRemove);
        assertEquals("68d77f380f7e215676838eca9b90ebb8", kvServer1.getNodeHashRange()[0]);
        assertEquals("68d77f380f7e215676838eca9b90ebb8", kvServer1.getNodeHashRange()[1]);

        //wait for node to shutdown completely
        sleep(1000);

        ex = null;
        try {
            toServer2 = kvClient1.get(key2);
        } catch (Exception e) {
            ex = e;
            e.printStackTrace();
        }

        //Gets timeout exception user must retry
        assertNotNull(ex);

        ex = null;

        try {
            toServer2 = kvClient1.get(key2);
        } catch (Exception e) {
            ex = e;
            e.printStackTrace();
        }

        assertNull(ex);
        assertEquals(StatusType.GET_SUCCESS, toServer2.getStatus());
        assertEquals(kvServer1.getNodePort(), kvClient1.getServerPort());

        AllTests.resetECS();
    }

    @Test
	public void testAddTwoNodes() {
        Exception ex = null;

        Collection<String> toRemove = new ArrayList<String>();
        toRemove.add(kvServer2.getNodeName());
        AllTests.ecs.removeNodes(toRemove);

        KVSimpleMessage toServer2 = null;
        String key2 = "abc";
        String value2 = "hashes to kvServer2";
        assertEquals(Hash.MD5(key2), "900150983cd24fb0d6963f7d28e17f72");
        assertEquals("68d77f380f7e215676838eca9b90ebb8", kvServer1.getNodeHashRange()[0]);
        assertEquals("68d77f380f7e215676838eca9b90ebb8", kvServer1.getNodeHashRange()[1]);
        assertNull(kvServer2.getNodeHashRange()[0]);
        assertNull(kvServer2.getNodeHashRange()[1]);

        try {
            toServer2 = kvClient1.put(key2, value2);
        } catch (Exception e) {
            ex = e;
            e.printStackTrace();
        }
        assertNull(ex);
        assertEquals(StatusType.PUT_SUCCESS, toServer2.getStatus());
        assertEquals(kvServer1.getNodePort(), kvClient1.getServerPort());

        AllTests.ecs.addNode(CacheStrategy.FIFO.toString(), 0);
        AllTests.ecs.start();
        assertEquals("68d77f380f7e215676838eca9b90ebb8", kvServer1.getNodeHashRange()[1]);
        assertEquals("ee935ceeea3cd07c8937e5ad812759a8", kvServer2.getNodeHashRange()[1]);

        AllTests.ecs.addNode(CacheStrategy.FIFO.toString(), 0);
        AllTests.ecs.start();

        KVStore kvClient3 = new KVStore("localhost", 49992);

        ex = null;
        try {
            kvClient3.connect();
            toServer2 = kvClient3.get(key2);
        } catch (Exception e) {
            ex = e;
            e.printStackTrace();
        }
        assertNull(ex);
        assertEquals(StatusType.GET_SUCCESS, toServer2.getStatus());
        assertEquals(49992, kvClient3.getServerPort());

        AllTests.resetECS();
    }

    @Test
    public void testAddNode() {
        Exception ex = null;

        Collection<String> toRemove = new ArrayList<String>();
        toRemove.add(kvServer2.getNodeName());
        AllTests.ecs.removeNodes(toRemove);

        KVSimpleMessage toServer2 = null;
        String key2 = "abc";
        String value2 = "hashes to kvServer2";
        assertEquals(Hash.MD5(key2), "900150983cd24fb0d6963f7d28e17f72");
        assertEquals("68d77f380f7e215676838eca9b90ebb8", kvServer1.getNodeHashRange()[0]);
        assertEquals("68d77f380f7e215676838eca9b90ebb8", kvServer1.getNodeHashRange()[1]);
        assertNull(kvServer2.getNodeHashRange()[0]);
        assertNull(kvServer2.getNodeHashRange()[1]);

        try {
            toServer2 = kvClient1.put(key2, value2);
        } catch (Exception e) {
            ex = e;
            e.printStackTrace();
        }
        assertNull(ex);
        assertEquals(StatusType.PUT_SUCCESS, toServer2.getStatus());
        assertEquals(kvServer1.getNodePort(), kvClient1.getServerPort());

        AllTests.ecs.addNode(CacheStrategy.FIFO.toString(), 0);
        AllTests.ecs.start();
        assertEquals("68d77f380f7e215676838eca9b90ebb8", kvServer1.getNodeHashRange()[1]);
        assertEquals("ee935ceeea3cd07c8937e5ad812759a8", kvServer2.getNodeHashRange()[1]);

        KVStore kvClient3 = new KVStore(kvServer2.getNodeHost(), kvServer2.getNodePort());

        ex = null;
        try {
            kvClient3.connect();
            toServer2 = kvClient3.get(key2);
        } catch (Exception e) {
            ex = e;
            e.printStackTrace();
        }
        assertNull(ex);
        assertEquals(StatusType.GET_SUCCESS, toServer2.getStatus());
        assertEquals(kvServer2.getNodePort(), kvClient3.getServerPort());

        AllTests.resetECS();
    }
}
