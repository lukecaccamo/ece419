package testing;

import app_kvServer.KVServer;
import app_kvServer.IKVServer.CacheStrategy;

import org.junit.Test;

import app_kvClient.KVClient;
import client.KVStore;
import ecs.ECSNode;
import ecs.IECSNode;
import junit.framework.TestCase;
import shared.hashring.Hash;
import shared.messages.KVMessage;
import shared.messages.KVSimpleMessage;
import shared.messages.KVMessage.StatusType;

import java.io.File;
import java.util.concurrent.TimeoutException;

import static java.lang.Thread.sleep;

public class KVClientTest extends TestCase {

	private ECSNode kvServer1, kvServer2;
	private KVStore kvClient;

	public void setUp() {
		AllTests.resetECS();
		Exception ex = null;

		String nodeKey = AllTests.ecs.getNodeKeys()[0];
		kvServer1 = (ECSNode) AllTests.ecs.getNodeByKey(nodeKey);

		nodeKey = AllTests.ecs.getNodeKeys()[1];
        kvServer2 = (ECSNode) AllTests.ecs.getNodeByKey(nodeKey);

		kvClient = new KVStore(kvServer1.getNodeHost(), kvServer1.getNodePort());

		try {
			kvClient.connect();
		} catch (Exception e) {
			ex = e;
		}

		assertNull(ex);
	}

	public void tearDown() {
		AllTests.resetECS();
	}

	@Test
	public void testStoreDisconnect() {
		Exception ex = null;
		KVStore store = new KVStore(kvServer1.getNodeHost(), kvServer1.getNodePort());

		try {
			store.connect();
			store.disconnect();
		} catch (Exception e) {
			ex = e;
		}

		assertNull(ex);
	}

	@Test
	public void testMultipleClientConnectAndDisconnect() {
		Exception ex = null;
		KVStore[] kvClients = new KVStore[10];

		for (int i = 0; i < 10; i++) {
			kvClients[i] = new KVStore(kvServer1.getNodeHost(), kvServer1.getNodePort());
			try {
				kvClients[i].connect();
			} catch (Exception e) {
				ex = e;
			}
		}
		assertNull(ex);

		for (int i = 0; i < 10; i++) {
			try {
				kvClients[i].disconnect();
			} catch (Exception e) {
				ex = e;
			}
		}
		assertNull(ex);
	}

	@Test
	public void testMultipleClientPutAndGet() {
		Exception ex = null;
		KVStore[] kvClients = new KVStore[10];
		KVMessage[] kvMessages = new KVMessage[10];

		String key = "foofoo";
		String value = "bar";

		for (int i = 0; i < 10; i++) {
			kvClients[i] = new KVStore(kvServer1.getNodeHost(), kvServer1.getNodePort());
			try {
				kvClients[i].connect();
				kvMessages[i] = kvClients[i].put(key + Integer.toString(i), value);
			} catch (Exception e) {
				ex = e;
			}
		}
		assertNull(ex);

		for (int i = 0; i < 10; i++) {
			try {
				assertTrue(kvMessages[i].getStatus() == StatusType.PUT_SUCCESS);
				kvClients[i].disconnect();
			} catch (Exception e) {
				ex = e;
			}
		}
		assertNull(ex);

		for (int i = 0; i < 10; i++) {
			kvClients[i] = new KVStore(kvServer1.getNodeHost(), kvServer1.getNodePort());
			try {
				kvClients[i].connect();
				kvMessages[i] = kvClients[i].get(key + Integer.toString(i));
			} catch (Exception e) {
				ex = e;
			}
		}
		assertNull(ex);

		for (int i = 0; i < 10; i++) {
			try {
				assertTrue(kvMessages[i].getStatus() == StatusType.GET_SUCCESS);
				assertEquals(value, kvMessages[i].getValue());
				kvClients[i].disconnect();
			} catch (Exception e) {
				ex = e;
			}
		}
		assertNull(ex);
	}

	@Test
	public void testGetDisconnected() {
		Exception ex = null;

		kvClient.disconnect();
		String key = "foo";

		try {
			kvClient.get(key);
		} catch (Exception e) {
			ex = e;
		}

		assertNotNull(ex);
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
		Exception ex = null;
		KVMessage response = null;

		String key = "uniqueuniqueuniqueuniqueunique";
		String value = "bar2 bar3";

		try {
			response = kvClient.put(key, value);
		} catch (Exception e) {
			ex = e;
		}

		assertNull(ex);
		assertEquals(KVMessage.StatusType.PUT_ERROR, response.getStatus());
	}

	@Test
	public void testPutWithSpacesInKey() {
		Exception ex = null;
		KVMessage response = null;

		String key = "uni que";
		String value = "bar2";

		try {
			response = kvClient.put(key, value);
		} catch (Exception e) {
			ex = e;
		}

		assertNull(ex);
		assertEquals(KVMessage.StatusType.PUT_ERROR, response.getStatus());
	}

	@Test
	public void testPutWithSpacesInValue() {
		Exception ex = null;
		KVMessage response = null;

		String key = "unique";
		String value = "bar2 bar3";

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
	public void testDeleteNonexistingKey() {
		Exception ex = null;
		KVMessage response = null;

		String key = "deleteTestValue123";

		try {
			response = kvClient.put(key, "null");
		} catch (Exception e) {
			ex = e;
		}

		assertNull(ex);
		assertEquals(KVMessage.StatusType.DELETE_ERROR, response.getStatus());
	}

	@Test
	public void testDeleteWithEmptyValue() {
		Exception ex = null;
		KVMessage response = null;

		String key = "plsDelete";
		String value = "deleteMe";

		try {
			kvClient.put(key, value);
			response = kvClient.put(key, "");
		} catch (Exception e) {
			ex = e;
		}

		assertNull(ex);
		assertEquals(KVMessage.StatusType.DELETE_SUCCESS, response.getStatus());
	}

	@Test
    public void testNoServerSwitch() throws InterruptedException {
        KVSimpleMessage toServer1 = null;

        String key1 = "123";
        String value1 = "hashes to kvServer1";

        assertEquals(Hash.MD5(key1), "202cb962ac59075b964b07152d234b70");

        assertEquals("68d77f380f7e215676838eca9b90ebb8", kvServer1.getNodeHashRange()[1]);
        assertEquals("ee935ceeea3cd07c8937e5ad812759a8", kvServer2.getNodeHashRange()[1]);

        try {
            toServer1 = kvClient.put(key1, value1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertEquals(StatusType.PUT_SUCCESS, toServer1.getStatus());
        assertEquals(kvServer1.getNodePort(), kvClient.getServerPort());
    }

    @Test
    public void testServerSwitch() {
        KVSimpleMessage toServer2 = null;

        String key2 = "abc";
        String value2 = "hashes to kvServer2";

        assertEquals(Hash.MD5(key2), "900150983cd24fb0d6963f7d28e17f72");

        assertEquals("68d77f380f7e215676838eca9b90ebb8", kvServer1.getNodeHashRange()[1]);
        assertEquals("ee935ceeea3cd07c8937e5ad812759a8", kvServer2.getNodeHashRange()[1]);

        try {
            toServer2 = kvClient.put(key2, value2);
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertEquals(StatusType.PUT_SUCCESS, toServer2.getStatus());
        assertEquals(kvServer2.getNodePort(), kvClient.getServerPort());
    }

	@Test
	public void testReplication() {
		KVSimpleMessage toServer2 = null;
		KVSimpleMessage toServer1 = null;


		String key1 = "123";
		String value1 = "hashes to kvServer1";

		String key2 = "abc";
		String value2 = "hashes to kvServer2";

		assertEquals(Hash.MD5(key2), "900150983cd24fb0d6963f7d28e17f72");

		assertEquals("68d77f380f7e215676838eca9b90ebb8", kvServer1.getNodeHashRange()[1]);
		assertEquals("ee935ceeea3cd07c8937e5ad812759a8", kvServer2.getNodeHashRange()[1]);

		try {
			toServer2 = kvClient.put(key2, value2);
		} catch (Exception e) {
			e.printStackTrace();
		}

		assertEquals(StatusType.PUT_SUCCESS, toServer2.getStatus());
		assertEquals(kvServer2.getNodePort(), kvClient.getServerPort());

		try {
			toServer1 = kvClient.put(key1, value1);
		} catch (Exception e) {
			e.printStackTrace();
		}

		assertEquals(StatusType.PUT_SUCCESS, toServer1.getStatus());
		assertEquals(kvServer1.getNodePort(), kvClient.getServerPort());

		try {
			toServer1 = kvClient.get(key2);
		} catch (Exception e) {
			e.printStackTrace();
		}

		assertEquals(StatusType.GET_SUCCESS, toServer1.getStatus());
		assertEquals(kvServer1.getNodePort(), kvClient.getServerPort());
	}

	@Test
	public void testClientTimeout() throws Exception {

		Exception ex = null;
		String key1 = "123";

		AllTests.ecs.shutdown();

		sleep(2000);

		try {
			KVSimpleMessage msg = kvClient.get(key1);
		} catch (Exception e) {
			ex = e;
			e.printStackTrace();
		}

		assertTrue(ex instanceof TimeoutException);
	}
}
