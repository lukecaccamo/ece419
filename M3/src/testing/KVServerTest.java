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
	public void testPersistence() {
		Exception ex = null;
		String storedValue = null;

		String key = "foo2";
		String value = "bar2";
		KVMessage message = null;

		try {
			kvServer = new KVServer(50000, 0, CacheStrategy.FIFO.toString());
			kvServer.putKV(key, value);
			storedValue = kvServer.getKV(key);
		} catch (Exception e) {
			ex = e;
		}

		assertNull(ex);
		assertEquals(value, storedValue);

		kvServer.clearCache();
		kvServer.close();

		try {
			kvServer = new KVServer(50000, 0, CacheStrategy.FIFO.toString());
			storedValue = kvServer.getKV(key);
		} catch (Exception e) {
			ex = e;
		}
		
		assertNull(ex);
		assertEquals(value, storedValue);
	}
}