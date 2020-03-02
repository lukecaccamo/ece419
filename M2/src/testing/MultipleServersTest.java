package testing;

import app_kvServer.IKVServer;
import app_kvServer.KVServer;
import ecs.IECSNode;
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

public class MultipleServersTest extends TestCase {
    private KVStore kvClient;
    private KVServer kvServer1, kvServer2;
    private int port1, port2;
    private String serverHash1, serverHash2;
    private ECSNode node1, node2;
    private HashRing metaData;
    private int NUM_OPS = 5000;
    private int CACHE_SIZE = 1000;
    private String POLICY = "FIFO";
    private ECSNode node = null;

    public void setUp() {

        port1 = 50501;
        port2 = port1 + 1;

        kvServer1 = new KVServer(port1, CACHE_SIZE, POLICY);
        kvServer2 = new KVServer(port2, CACHE_SIZE, POLICY);

        kvServer1.start();
        kvServer2.start();

        metaData = new HashRing();
        node1 = new ECSNode("Server1", "127.0.0.1", port1);
        node2 = new ECSNode("Server2", "127.0.0.1", port2);

        kvServer1.setServerHash("b05e2b689f6eff6650abea85f180ebd0");
        kvServer2.setServerHash("43cd9550c83c977b451a47535f10884c");
        serverHash1 = kvServer1.getServerHash();
        serverHash2 = kvServer2.getServerHash();

        System.out.println("Server1 Hash: " + serverHash1);
        System.out.println("Server2 Hash: " + serverHash2);

        metaData.addServer(serverHash1, node1);
        metaData.addServer(serverHash2, node2);

        kvServer1.updateMetaData(metaData);
        kvServer2.updateMetaData(metaData);

    }

    public void tearDown() {
        kvServer1.kill();
        kvServer2.kill();
        reset();
    }

    public void reset() {

        File file = new File(port1 + "databaseFile.db");
        file.delete();

        file = new File(port1 + "index.txt");
        file.delete();

        file = new File(port2 + "databaseFile.db");
        file.delete();

        file = new File(port2 + "index.txt");
        file.delete();
    }

    private static String name = "nodeName";
    private static String host = "nodeHost";
    private static int port = 55;

    @Test
    public void testResponsibleServers() throws InterruptedException {

        //initially connect to first port
        kvClient = new KVStore("localhost", 50501);

        // convert serverhashes to big int, get keys that would go into each server
        BigInteger sh1 = new BigInteger(serverHash1, 32);
        BigInteger sh2 = new BigInteger(serverHash2, 32);

        String keyHash1 = (sh1.subtract(BigInteger.ONE)).toString(32);
        System.out.println("keyHash1: " + keyHash1);
        String keyHash2 = (sh2.subtract(BigInteger.ONE)).toString(32);
        System.out.println("keyHash2: " + keyHash2);

        KVSimpleMessage response = null;

        try {
            kvClient.connect();
        } catch (Exception e) {
        }

        try {
            //f03eb3e2b053dfafe3acda7c20226730
            response = kvClient.put("nothingmuch", "value2");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // client's connection should be to server 2 now
        assertEquals(StatusType.PUT_SUCCESS, response.getStatus());
        assertEquals(port2, kvClient.getServerPort());
        assertNotNull(kvClient.getMetaData());

        kvClient.disconnect();

        kvServer1.clearStorage();;
        kvServer2.clearStorage();
    }

    public void testNoServerSwitch() throws InterruptedException {

        KVSimpleMessage response = null;
        kvClient = new KVStore("localhost", 50502);

        try {
            kvClient.connect();
        } catch (Exception e) {
        }

        try {
            //2071e5eb7fd74592bcfacb3e9ecc4bd1
           response = kvClient.put("ehh", "value2");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // client's connection should be to server 2 now
        assertEquals(port2, kvClient.getServerPort());
        assertEquals(StatusType.PUT_SUCCESS, response.getStatus());
        assertNull(kvClient.getMetaData());

        kvClient.disconnect();

        kvServer1.clearStorage();
        kvServer2.clearStorage();
    }

    @Test
    public void testMoveData() throws InterruptedException {

        //initially connect to first port
        kvClient = new KVStore("localhost", 50501);

        try {
            kvClient.connect();
        } catch (Exception e) {
        }

        String key = "key";
        String value = "value";

        // put 100 keys into server 1--just large enough so that at least some will get in
        int i = 0;
        for (i = 0; i < 20; i++){
            //System.out.println("in loop1");
            try {
                kvClient.put(key + String.valueOf(i), value + String.valueOf(i));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String[] range = {"0", "ffffffffffffffffffffffffffffffff"};
        //String[] range = {serverHash1, serverHash2}
        try {
            kvServer1.moveData(range, serverHash2);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (i = 0; i < 20; i++){
            try {
                //System.out.println("in loop2");
                String s2value = kvServer2.getKV(key + String.valueOf(i));
                assertEquals(value + String.valueOf(i), s2value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            System.out.println("in loop3");
            String s2value = kvServer1.getKV("key0");
            assertNull(s2value);
        } catch (Exception e) {
            e.printStackTrace();
        }

        kvClient.disconnect();
        //tearDown();
    }

    @Test
    public void testGetServerName() {
        String actualVal = node1.getNodeName();
        assertEquals("Server1", actualVal);
    }

    @Test
    public void testGetPortName() {
        int actualVal = node1.getNodePort();
        assertEquals(port1, actualVal);
    }

    @Test
    public void testGetHostName() {
        String actualVal = node1.getNodeHost();
        assertEquals("127.0.0.1", actualVal);
    }

    @Test
    public void testGetHashKey() {
        String goodVal = Hash.MD5("127.0.0.1" + ":" + port1);
        String actualVal = node1.getHashKey();
        assertEquals(goodVal, actualVal);
    }

    @Test
    public void testFlagDefaultShutdown() {
        IECSNode.IECSNodeFlag actualVal = node1.getFlag();
        assertEquals(IECSNode.IECSNodeFlag.SHUT_DOWN, actualVal);
    }

    @Test
    public void testSetCacheSize() {
        node1.setCacheSize(33);
        assertEquals(33, node1.getCacheSize());
    }
    @Test
    public void testSetCachePolicy() {
        node1.setCacheStrategy("LRU");
        assertEquals(IKVServer.CacheStrategy.LRU, node1.getCacheStrategy());
    }


}
