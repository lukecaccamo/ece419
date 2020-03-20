package testing;

import java.net.UnknownHostException;

import app_kvServer.IKVServer.CacheStrategy;
import client.KVStore;
import ecs.ECSNode;

import junit.framework.TestCase;

public class ConnectionTest extends TestCase {

	public void setUp() {

	}

	public void tearDown() {
		AllTests.resetDB();
	}

	public void testConnectionSuccess() {
		Exception ex = null;
		String nodeKey = AllTests.ecs.getNodeKeys()[0];
		ECSNode node = (ECSNode) AllTests.ecs.getNodeByKey(nodeKey);
		KVStore kvClient = new KVStore(node.getNodeHost(), node.getNodePort());

		try {
			kvClient.connect();
		} catch (Exception e) {
			ex = e;
		}

		assertNull(ex);
	}

	public void testUnknownHost() {
		Exception ex = null;
		String nodeKey = AllTests.ecs.getNodeKeys()[0];
		ECSNode node = (ECSNode) AllTests.ecs.getNodeByKey(nodeKey);
		KVStore kvClient = new KVStore("unknown", node.getNodePort());

		try {
			kvClient.connect();
		} catch (Exception e) {
			ex = e;
		}

		assertTrue(ex instanceof UnknownHostException);
	}

	public void testIllegalPort() {
		Exception ex = null;
		String nodeKey = AllTests.ecs.getNodeKeys()[0];
		ECSNode node = (ECSNode) AllTests.ecs.getNodeByKey(nodeKey);
		KVStore kvClient = new KVStore(node.getNodeHost(), 123456789);

		try {
			kvClient.connect();
		} catch (Exception e) {
			ex = e;
		}

		assertTrue(ex instanceof IllegalArgumentException);
	}

}
