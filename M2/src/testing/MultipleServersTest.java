package testing;

import app_kvServer.KVServer;
import ecs.IECSNode;
import org.junit.Test;

import client.KVStore;
import junit.framework.TestCase;
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

    public void setUp() {
        port1 = 50501;
        port2 = port1 + 1;

        kvServer1 = new KVServer(port1, CACHE_SIZE, POLICY);
        kvServer2 = new KVServer(port2, CACHE_SIZE, POLICY);
        //initially connect to first port
        kvClient = new KVStore("localhost", 50501);

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
        kvClient.disconnect();
        kvServer1.close();
        kvServer2.close();
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

    @Test
    public void testResponsibleServers() throws InterruptedException {

        // convert serverhashes to big int, get keys that would go into each server
        BigInteger sh1 = new BigInteger(serverHash1, 32);
        BigInteger sh2 = new BigInteger(serverHash2, 32);

        String keyHash1 = (sh1.subtract(BigInteger.ONE)).toString(32);
        System.out.println("keyHash1: " + keyHash1);
        String keyHash2 = (sh2.subtract(BigInteger.ONE)).toString(32);
        System.out.println("keyHash2: " + keyHash2);

        try {
            kvClient.connect();
        } catch (Exception e) {
        }

        try {
            //f03eb3e2b053dfafe3acda7c20226730
            kvClient.put("nothingmuch", "value2");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // client's connection should be to server 2 now
        assertEquals(port2, kvClient.getServerPort());
        assertNotNull(kvClient.getMetaData());
    }
}
