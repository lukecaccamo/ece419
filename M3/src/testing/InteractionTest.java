package testing;

import org.junit.Test;

import app_kvServer.IKVServer.CacheStrategy;
import client.KVStore;
import junit.framework.TestCase;
import shared.messages.IKVAdminMessage;
import shared.messages.KVAdminMessage;
import shared.messages.KVMessage;
import shared.messages.KVMessage.StatusType;
import ecs.ECSNode;

import java.io.File;
import java.math.BigInteger;

public class InteractionTest extends TestCase {

	private KVStore kvClient;

	public void setUp() {
		String nodeKey = AllTests.ecs.getNodeKeys()[0];
		ECSNode node = (ECSNode) AllTests.ecs.getNodeByKey(nodeKey);
		kvClient = new KVStore(node.getNodeHost(), node.getNodePort());
		try {
			kvClient.connect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void tearDown() {
		AllTests.resetDB();
	}

	@Test
	public void testPut() {
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
		assertEquals(value, response.getValue());
	}

	@Test
	public void testPutDisconnected() {
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
		assertEquals(StatusType.GET_SUCCESS, response.getStatus());
		assertEquals(value, response.getValue());
	}

	@Test
	public void testGetUnsetValue() {
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
