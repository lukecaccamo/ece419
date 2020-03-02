package testing;

import org.junit.Test;

import client.KVStore;
import junit.framework.TestCase;
import shared.messages.IKVAdminMessage;
import shared.messages.KVAdminMessage;
import shared.messages.KVMessage;
import shared.messages.KVMessage.StatusType;
import shared.hashring.HashRing;
import ecs.ECSNode;

import java.io.File;
import java.math.BigInteger;

public class InteractionTest extends TestCase {

	private KVStore kvClient;
	
	public void setUp() {
		reset();
		kvClient = new KVStore("localhost", 50000);
		try {
			kvClient.connect();
		} catch (Exception e) {
		}
	}

	public void tearDown() {
		kvClient.disconnect();
	}

	public void reset() {

		File file = new File(50000 + "databaseFile.db");
		file.delete();

		file = new File(50000 + "index.txt");
		file.delete();
	}
	
	
	@Test
	public void testPut() {
		reset();
		String key = "foo2";
		String value = "bar2";
		KVMessage response = null;
		Exception ex = null;

		try {
			response = kvClient.put(key, value);
		} catch (Exception e) {
			ex = e;
		}

		assertNull(ex);
		assertEquals(StatusType.PUT_SUCCESS, response.getStatus());
	}
	
	@Test
	public void testPutDisconnected() {
		reset();
		kvClient.disconnect();
		String key = "foo";
		String value = "bar";
		Exception ex = null;

		try {
			kvClient.put(key, value);
		} catch (Exception e) {
			ex = e;
		}

		assertNotNull(ex);
	}

	@Test
	public void testUpdate() {
		reset();
		String key = "updateTestValue";
		String initialValue = "initial";
		String updatedValue = "updated";
		
		KVMessage response = null;
		Exception ex = null;

		try {
			kvClient.put(key, initialValue);
			response = kvClient.put(key, updatedValue);
			
		} catch (Exception e) {
			ex = e;
		}

		assertNull(ex);
		assertEquals(StatusType.PUT_UPDATE, response.getStatus());
		assertEquals(updatedValue, response.getValue());
	}
	
	@Test
	public void testDelete() {
		reset();
		String key = "deleteTestValue";
		String value = "toDelete";
		
		KVMessage response = null;
		Exception ex = null;

		try {
			kvClient.put(key, value);
			response = kvClient.put(key, "null");
			
		} catch (Exception e) {
			ex = e;
		}

		assertNull(ex);
		assertEquals(StatusType.DELETE_SUCCESS, response.getStatus());
	}
	
	@Test
	public void testGet() {
		reset();
		String key = "foo";
		String value = "bar";
		KVMessage response = null;
		Exception ex = null;

			try {
				kvClient.put(key, value);
				response = kvClient.get(key);
			} catch (Exception e) {
				ex = e;
			}
		
		assertNull(ex);
		assertEquals("bar", response.getValue());
	}

	@Test
	public void testGetUnsetValue() {
		reset();
		String key = "an unset value";
		KVMessage response = null;
		Exception ex = null;

		try {
			response = kvClient.get(key);
		} catch (Exception e) {
			ex = e;
		}

		assertNull(ex);
		assertEquals(StatusType.GET_ERROR, response.getStatus());
	}
}
