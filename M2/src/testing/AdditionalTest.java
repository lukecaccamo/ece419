package testing;

import app_kvServer.KVServer;
import org.junit.Test;

import app_kvClient.KVClient;
import client.KVStore;
import junit.framework.TestCase;
import shared.messages.KVMessage;
import shared.messages.KVMessage.StatusType;

import java.io.File;

public class AdditionalTest extends TestCase {

	private KVStore kvClient;
	private KVServer kvServer;
	
	public void setUp() {
		kvClient = new KVStore("localhost", 50000);
		try {
			kvClient.connect();
		} catch (Exception e) {
		}
		reset();
	}

	public void tearDown() {
		kvClient.disconnect();
		reset();
	}

	public void reset() {
		for (int i = 0; i < 4; i++) {
			File file = new File((50000 + i) + "databaseFile.db");
			file.delete();

			file = new File((50000+ i) + "index.txt");
			file.delete();
		}
	}
	
	// TODO: add your test cases, at least 3
	@Test
	public void testClientDisconnect() {
		
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
	public void testMultipleClientConnectSuccess() {
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
	public void testMultipleClientPutSuccess() {
		String key = "foofoo";
		String value = "bar";
		Exception ex = null;
		KVStore[] kvClients = new KVStore[10];
		KVMessage[] kvMessages = new KVMessage[10];

		for(int i = 0; i < 10; i++) {
			kvClients[i] = new KVStore("localhost", 50000);
			try {
				kvClients[i].connect();
				kvMessages[i] = kvClients[i].put(key + Integer.toString(i), value);
			} catch (Exception e) {
				ex = e;
			}
		}
		assertNull(ex);

		for(int i = 0; i < 10; i++) {
			try {
				assertTrue(kvMessages[i].getStatus() == StatusType.PUT_SUCCESS);
				kvClients[i].disconnect();
			} catch (Exception e) {
				ex = e;
			}
		}
		assertNull(ex);
	}

	@Test
	public void testPersistence() {
		KVServer kvServer = new KVServer(50001, 10, "FIFO");

		String key = "foo2";
		String value = "bar2";
		String storedValue = null;
		KVMessage message = null;
		Exception ex = null;

		try {
			kvServer.putKV(key, value);
			storedValue = kvServer.getKV(key);
		} catch (Exception e) {
			ex = e;
		}
		assertTrue(ex == null && storedValue == value);

		kvServer.clearCache();
		kvServer.close();
		kvServer = null;
		storedValue = null;
		System.gc();

		kvServer = new KVServer(50001, 10, "FIFO");

		try {
			storedValue = kvServer.getKV(key);
		} catch (Exception e) {
			ex = e;
		}
		//System.out.println(storedValue.equals((value)));
		
		assertTrue(ex == null && storedValue.equals(value));
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
		kvServer = new KVServer(50003, 2, "LFU");
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
