package testing;

import app_kvServer.KVServer;
import org.junit.Test;

import client.KVStore;
import junit.framework.TestCase;
import shared.messages.KVMessage;

public class AdditionalTest extends TestCase {

	private KVStore kvClient;
	private KVServer kvServer;
	
	public void setUp() {
		kvClient = new KVStore("localhost", 50000);
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
		kvServer = new KVServer(5000, 2, "LFU");
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
			kvServer.putKV(key2, value2);
		} catch (Exception e) {
			e.printStackTrace();
		}

		inCache = kvServer.inCache(key);

		try {
			return_value = kvServer.getKV(key);
			System.out.println(return_value);
		} catch (Exception e) {
			e.printStackTrace();
		}

		assertTrue(return_value.equals(value) && inCache);
	}

	@Test
	public void testDeleteNonexistingKey() {
		String key = "deleteTestValue";

		KVMessage response = null;
		Exception ex = null;

		try {

			response = kvClient.put(key, "null");

		} catch (Exception e) {
			ex = e;
		}

		assertNull(ex);
		assertEquals(KVMessage.StatusType.DELETE_ERROR, response.getStatus());
	}

	@Test
	public void testPutWithSpacesInValue() {
		String key = "unique";
		String value = "bar2 bar3";
		KVMessage response = null;
		Exception ex = null;

		try {
			response = kvClient.put(key, value);
		} catch (Exception e) {
			ex = e;
		}

		assertNull(ex);
		assertEquals(KVMessage.StatusType.PUT_SUCCESS, response.getStatus());
		assertEquals(value, response.getValue());
	}

	@Test
	public void testPutWithSpacesInKey() {
		String key = "uni que";
		String value = "bar2";
		KVMessage response = null;
		Exception ex = null;

		try {
			response = kvClient.put(key, value);
		} catch (Exception e) {
			ex = e;
		}

		assertNull(ex);
		assertEquals(KVMessage.StatusType.PUT_ERROR, response.getStatus());
	}

	@Test
	public void testPutWithEmptyKey() {
		String key = "";
		String value = "bar2";
		KVMessage response = null;
		Exception ex = null;

		try {
			response = kvClient.put(key, value);
		} catch (Exception e) {
			ex = e;
		}

		assertNull(ex);
		assertEquals(KVMessage.StatusType.PUT_ERROR, response.getStatus());
	}

	@Test
	public void testPutWithLongKey() {
		String key = "uniqueuniqueuniqueuniqueunique";
		String value = "bar2 bar3";
		KVMessage response = null;
		Exception ex = null;

		try {
			response = kvClient.put(key, value);
		} catch (Exception e) {
			ex = e;
		}

		assertNull(ex);
		assertEquals(KVMessage.StatusType.PUT_ERROR, response.getStatus());
	}

	@Test
	public void testDeleteWithEmptyValue() {
		String key = "plsDelete";
		String value = "deleteMe";
		KVMessage response = null;
		Exception ex = null;

		try {
			kvClient.put(key, value);
			response = kvClient.put(key, "");
		} catch (Exception e) {
			ex = e;
		}

		assertNull(ex);
		assertEquals(KVMessage.StatusType.DELETE_SUCCESS, response.getStatus());
	}
}
