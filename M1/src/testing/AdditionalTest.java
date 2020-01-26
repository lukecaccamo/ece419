package testing;

import app_kvServer.KVServer;
import org.junit.Test;

import client.KVStore;
import junit.framework.TestCase;

public class AdditionalTest extends TestCase {

	private KVStore kvClient;
	private KVServer kvServer;
	
	public void setUp() {
		kvClient = new KVStore("localhost", 50000);
		kvServer = new KVServer(50000, 1, "LFU");
		try {
			kvClient.connect();
		} catch (Exception e) {
		}
	}

	public void tearDown() {
		kvClient.disconnect();
	}
	
	// TODO: add your test cases, at least 3
	@Test
	public void testStoreDisconnect() {
		
		Exception ex = null;
		
		KVStore store = new KVStore("localhost", 50000);
		try {
			store.connect();
			store.disconnect();
		} catch (Exception e) {
			ex = e;
		}	
		
		assertNull(ex);
	}

	@Test
	public void testMultipleStoreConnectSuccess() {
		Exception ex = null;
		KVStore[] kvClients = new KVStore[10];

		for(int i = 0; i < 10; i++) {
			kvClients[i] = new KVStore("localhost", 50000);
			try {
				kvClients[i].connect();
			} catch (Exception e) {
				ex = e;
			}
		}
		assertNull(ex);

		for(int i = 0; i < 10; i++) {
			try {
				kvClients[i].disconnect();
			} catch (Exception e) {
				ex = e;
			}
		}
		assertNull(ex);
	}

	@Test
	public void testGetDisconnected() {
		kvClient.disconnect();
		String key = "foo";
		Exception ex = null;

		try {
			kvClient.get(key);
		} catch (Exception e) {
			ex = e;
		}

		assertNotNull(ex);
	}

	@Test
	public void testPutGet() {

		String key = "foo2";
		String key2 = "foo3";
		String value = "bar2";
		String value2 = "bar3";
		String return_value = null;
		boolean inCache = true;

		try {
			kvServer.putKV(key, value);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			return_value = kvServer.getKV(key);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			kvServer.putKV(key2, value2);
		} catch (Exception e) {
			e.printStackTrace();
		}

		inCache = kvServer.inCache(key);

		assertTrue(return_value.equals(value) && !inCache);
	}
}
