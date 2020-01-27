package testing;

import app_kvServer.KVServer;
import client.KVStore;
import junit.framework.TestCase;
import org.junit.Test;
import shared.messages.KVMessage.StatusType;
import shared.messages.KVSimpleMessage;

import static java.lang.Thread.sleep;


public class PerformanceTestLFU extends TestCase {

	private KVStore kvClient;
	private KVServer kvServer;
	private int NUM_OPS = 20000;
	private int CACHE_SIZE = 20000;
	private String POLICY = "LFU";

	public void tearDown() {
		kvClient.disconnect();
		kvServer.close();
	}

	@Test
	public void testPut() throws InterruptedException {
		kvServer = new KVServer(50502, CACHE_SIZE, POLICY);
		kvClient = new KVStore("localhost", 50502);

		Exception ex = null;
		KVSimpleMessage response = null;


		try {
			kvClient.connect();
		} catch (Exception e) {

		}

		long startTime = System.nanoTime();

		for (int i = 0; i < NUM_OPS; i++){
			try {
				response = kvClient.put(Integer.toString(i), Integer.toString(i));
			} catch (Exception e) {
				ex = e;
			}
		}

		long estimatedTime = System.nanoTime() - startTime;

		System.out.println("100 % PUT, ops: " + NUM_OPS +" time " + estimatedTime);

		assertNull(ex);
		assertEquals(StatusType.PUT_SUCCESS, response.getStatus());

		kvServer.clearCache();
		kvServer.clearStorage();

		startTime = System.nanoTime();

		for (int i = 0; i < NUM_OPS*0.8; i++){
			try {
				response = kvClient.put(Integer.toString(i), Integer.toString(i));
			} catch (Exception e) {
				ex = e;
			}
		}

		for (int i = 0; i < NUM_OPS*0.2; i++){
			try {
				response = kvClient.get(Integer.toString(i));
			} catch (Exception e) {
				ex = e;
			}
		}

		estimatedTime = System.nanoTime() - startTime;

		System.out.println("80 % PUT, ops: " + NUM_OPS +" time " + estimatedTime);

		assertNull(ex);
		assertEquals(StatusType.GET_SUCCESS, response.getStatus());

		kvServer.clearCache();
		kvServer.clearStorage();

		startTime = System.nanoTime();

		for (int i = 0; i < NUM_OPS*0.5; i++){
			try {
				response = kvClient.put(Integer.toString(i), Integer.toString(i));
			} catch (Exception e) {
				ex = e;
			}
		}

		for (int i = 0; i < NUM_OPS*0.5; i++){
			try {
				response = kvClient.get(Integer.toString(i));
			} catch (Exception e) {
				ex = e;
			}
		}

		estimatedTime = System.nanoTime() - startTime;

		System.out.println("50 % PUT, ops: " + NUM_OPS +" time " + estimatedTime);

		assertNull(ex);
		assertEquals(StatusType.GET_SUCCESS, response.getStatus());

		kvServer.clearCache();
		kvServer.clearStorage();

		startTime = System.nanoTime();

		for (int i = 0; i < NUM_OPS*0.2; i++){
			try {
				response = kvClient.put(Integer.toString(i), Integer.toString(i));
			} catch (Exception e) {
				ex = e;
			}
		}

		for (int j = 0; j < 4; j++) {
			for (int i = 0; i < NUM_OPS*0.2; i++){
				try {
					response = kvClient.get(Integer.toString(i));
				} catch (Exception e) {
					ex = e;
				}
			}
		}

		estimatedTime = System.nanoTime() - startTime;

		System.out.println("20 % PUT, ops: " + NUM_OPS +" time " + estimatedTime);

		assertNull(ex);
		assertEquals(StatusType.GET_SUCCESS, response.getStatus());

		kvServer.clearCache();
		kvServer.clearStorage();

		sleep(1000);
	}
}
