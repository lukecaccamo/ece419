package testing;

import app_kvServer.KVServer;
import junit.framework.Test;
import junit.framework.TestSuite;
import logger.LogSetup;
import org.apache.log4j.Level;

import java.io.IOException;


public class PerformanceTests {

	static {
		try {
			new LogSetup("logs/testing/test.log", Level.ERROR);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static Test suite() {
		TestSuite perfSuite = new TestSuite("Performance Test-Suite");
		// perfSuite.addTestSuite(PerformanceTestLRU.class);
		// perfSuite.addTestSuite(PerformanceTestLFU.class);
		// perfSuite.addTestSuite(PerformanceTestFIFO.class);
		perfSuite.addTestSuite(PerformanceTestNone.class);
		return perfSuite;
	}

}
