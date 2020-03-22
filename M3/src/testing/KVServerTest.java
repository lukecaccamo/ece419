package testing;

import app_kvServer.KVServer;
import app_kvServer.IKVServer.CacheStrategy;

import org.junit.Test;

import app_kvClient.KVClient;
import client.KVStore;
import ecs.ECSNode;
import junit.framework.TestCase;
import shared.hashring.HashRing;
import shared.messages.KVMessage;
import shared.messages.KVMessage.StatusType;

import java.io.File;

public class KVServerTest extends TestCase {

	public void setUp() {
		
	}

	public void tearDown() {
	}

	@Test
	public void testMoveData() throws Exception {
		int port1 = 50001;
		int port2 = 50002;

		HashRing metaData = new HashRing();
        ECSNode node1 = new ECSNode("Server1", "localhost", port1);
		ECSNode node2 = new ECSNode("Server2", "localhost", port2);
		node1.stopKVServer();
		node2.stopKVServer();

		KVServer kvServer1 = new KVServer(port1, 0, CacheStrategy.FIFO.toString());
		KVServer kvServer2 = new KVServer(port2, 0, CacheStrategy.FIFO.toString());
		kvServer1.start();
		kvServer2.start();

		kvServer1.setServerHash("b05e2b689f6eff6650abea85f180ebd0");
		kvServer2.setServerHash("43cd9550c83c977b451a47535f10884c");
		String serverHash1 = kvServer1.getServerHash();
        String serverHash2 = kvServer2.getServerHash();

        metaData.addServer(serverHash1, node1);
        metaData.addServer(serverHash2, node2);
        kvServer1.setMetaData(metaData);
		kvServer2.setMetaData(metaData);

		String key = "key";
		String value = "value";

		for (int i = 0; i < 20; i++) {
			kvServer1.putKV(key + String.valueOf(i), value + String.valueOf(i));
		}

		String[] range = { "0", "ffffffffffffffffffffffffffffffff" };
		kvServer1.moveData(range, serverHash2);

		for (int i = 0; i < 20; i++) {
			String s2value = kvServer2.getKV(key + String.valueOf(i));
			assertEquals(value + String.valueOf(i), s2value);
		}

		String s2value = kvServer1.getKV("key0");
		assertNull(s2value);
	}

	@Test
	public void testPersistence() throws Exception {
		// Persistence test goes here
	}
}