package testing;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Level;

import app_kvServer.KVDatabase;
import junit.framework.Test;
import junit.framework.TestSuite;
import logger.LogSetup;

import ecs.ECS;

public class AllTests {
	static final boolean DEBUG = false;
	static {
		try {
			new LogSetup("logs/testing/test.log", Level.ERROR);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static ECS ecs = new ECS(AllTests.CONFIG_FILE_PATH, DEBUG);

	static void resetECS() {
		AllTests.ecs.shutdown();
		AllTests.resetDB();
		AllTests.ecs.setupNodes(2, "FIFO", 0);
		AllTests.ecs.start();
	}

	static void resetDB() {
		try {
			InputStream configFile = new FileInputStream(AllTests.CONFIG_FILE_PATH);
			Properties properties = new Properties();
			properties.load(configFile);
			configFile.close();
			for (String key : properties.stringPropertyNames()) {
				String[] value = properties.getProperty(key).split("\\s+");
				String host = value[0];
				String port = value[1];

				File file = new File(KVDatabase.parseIndexFilePath(port));
				file.delete();

				file = new File(KVDatabase.parseDatabaseFilePath(port));
				file.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static final String CONFIG_FILE_PATH = "./ecs.config";

	public static Test suite() {
		AllTests.resetECS();

		TestSuite clientSuite = new TestSuite("Advanced Storage Server Test-Suite");
		clientSuite.addTestSuite(ConnectionTest.class);
		clientSuite.addTestSuite(InteractionTest.class);
		clientSuite.addTestSuite(ECSTest.class);
		clientSuite.addTestSuite(KVClientTest.class);
		clientSuite.addTestSuite(KVServerTest.class);
		return clientSuite;
	}
}
