package testing;

import java.io.IOException;
import java.math.BigInteger;

import ecs.ECSNode;
import org.apache.log4j.Level;

import app_kvServer.KVServer;
import junit.framework.Test;
import junit.framework.TestSuite;
import logger.LogSetup;
import shared.hashring.Hash;
import shared.hashring.HashRing;


public class AllTests {

	static {
		try {
			new LogSetup("logs/testing/test.log", Level.ERROR);
			KVServer server = new KVServer(50000, 10, "FIFO");
			server.start();

			HashRing metadata = new HashRing();
			ECSNode node = new ECSNode("Server1", server.getHostname(), server.getPort());

			server.setServerHash(node.getHashKey());
			metadata.addServer(node.getHashKey(), node);

			server.setMetaData(metadata);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public static Test suite() {
		TestSuite clientSuite = new TestSuite("Basic Storage ServerTest-Suite");
		clientSuite.addTestSuite(ConnectionTest.class);
		clientSuite.addTestSuite(InteractionTest.class); 
		clientSuite.addTestSuite(AdditionalTest.class); 
		clientSuite.addTestSuite(MultipleServersTest.class);
		return clientSuite;
	}
	
}
