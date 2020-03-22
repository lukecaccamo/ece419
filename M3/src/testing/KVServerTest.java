package testing;

import app_kvServer.KVServer;
import app_kvServer.IKVServer.CacheStrategy;

import org.junit.Test;

import app_kvClient.KVClient;
import client.KVStore;
import junit.framework.TestCase;
import shared.messages.KVMessage;
import shared.messages.KVMessage.StatusType;

import java.io.File;

public class KVServerTest extends TestCase {

	private KVServer kvServer;

	public void setUp() {
		AllTests.resetDB();
	}

	public void tearDown() {
		AllTests.resetDB();
	}

	@Test
	public void testPersistence() throws Exception {
		String storedValue = null;

		String key = "foo2";
		String value = "bar2";
		KVMessage message = null;

		kvServer = new KVServer(50000, 0, CacheStrategy.FIFO.toString());
		kvServer.putKV(key, value);
		storedValue = kvServer.getKV(key);
		assertEquals(value, storedValue);

		kvServer.clearCache();
		kvServer.close();

		kvServer = new KVServer(50000, 0, CacheStrategy.FIFO.toString());
		storedValue = kvServer.getKV(key);
		assertEquals(value, storedValue);
	}

	@Test
	public void testMoveData() throws Exception {
		KVStore kvClient = new KVStore("localhost", 50001);
		KVServer kvServer1 = new KVServer(50001, 0, CacheStrategy.FIFO.toString());
		KVServer kvServer2 = new KVServer(50002, 0, CacheStrategy.FIFO.toString());
		kvServer1.start();
		kvServer2.start();
		kvClient.connect();

		kvServer1.setServerHash("b05e2b689f6eff6650abea85f180ebd0");
		kvServer2.setServerHash("43cd9550c83c977b451a47535f10884c");
		String serverHash1 = kvServer1.getServerHash();
		String serverHash2 = kvServer2.getServerHash();

		String key = "key";
		String value = "value";

		for (int i = 0; i < 20; i++) {
			kvClient.put(key + String.valueOf(i), value + String.valueOf(i));
		}

		String[] range = { "0", "ffffffffffffffffffffffffffffffff" };
		kvServer1.moveData(range, serverHash2);

		for (i = 0; i < 20; i++) {
			String s2value = kvServer2.getKV(key + String.valueOf(i));
			assertEquals(value + String.valueOf(i), s2value);
		}

		String s2value = kvServer1.getKV("key0");
		assertNull(s2value);

		kvClient.disconnect();
		kvServer1.clearCache();
		kvServer1.close();
		kvServer2.clearCache();
		kvServer2.close();
	}
}