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

    public void tearDown() {
        kvClient.disconnect();
        kvServer1.close();
        kvServer2.close();
    }

    @Test
    public void testResponsibleServers() throws InterruptedException {
        port1 = 50501;
        port2 = port1 + 1;
        kvServer1 = new KVServer(port1, CACHE_SIZE, POLICY);
        kvServer2 = new KVServer(port2, CACHE_SIZE, POLICY);
        //initially connect to first port
        kvClient = new KVStore("localhost", 50501);

        kvServer1.start();
        kvServer2.start();

        metaData = new HashRing();
        node1 = new ECSNode("Server1", kvServer1.getHost(), port1);
        node2 = new ECSNode("Server2", kvServer2.getHost(), port2);

        serverHash1 = kvServer1.getServerHash();
        serverHash2 = kvServer2.getServerHash();
        System.out.println("Server1 Hash: " + serverHash1);
        System.out.println("Server2 Hash: " + serverHash2);
        metaData.addServer(serverHash1, node1);
        metaData.addServer(serverHash2, node2);
        kvServer1.updateMetaData(metaData);
        kvServer2.updateMetaData(metaData);

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
            kvClient.put(keyHash2, "key2", "value2");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // client's connection should be to server 2 now
        assertEquals(port2, kvClient.getServerPort());
        assertNotNull(kvClient.getMetaData());

        // after receiving metadata, it'll always return server not responsible
        // bc actual hash is different than hash that we provide the function
        /*
        try {
            kvClient.put(keyHash1, "key1", "value1");
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertEquals(port1, kvClient.getServerPort());


        try {
            kvSimpleMessage = kvClient.get(keyHash2, "key2");
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertEquals("value2", kvSimpleMessage.getValue());*/
    }
}
