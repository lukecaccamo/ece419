package shared.communications;

import app_kvServer.IKVServer;
import app_kvServer.KVServer;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.zookeeper.*;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.data.Stat;
import java.util.concurrent.CountDownLatch;

import ecs.ECS;
import ecs.IECSNode;
import shared.hashring.HashRing;
import shared.messages.KVAdminMessage;
import shared.messages.IKVAdminMessage.ActionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.net.*;

public class KVAdminCommModule implements Runnable {

	private static Logger logger = Logger.getRootLogger();

	private String name;
	private String zkHost;
	private int zkPort;
	private String zkNodeName;
	private ZooKeeper zookeeper;
	private CountDownLatch connected;
	private String state;
	private ObjectMapper om;

	private KVServer server;
	private boolean running;

	public KVAdminCommModule(String name, String zkHost, int zkPort) {
		this.setRunning(true);
		this.logger.setLevel(Level.ERROR);
		this.name = name;
		this.zkHost = zkHost;
		this.zkPort = zkPort;
		this.zkNodeName = ECS.ZOOKEEPER_ADMIN_NODE_NAME + "/" + this.name;
		this.state = new String();
		this.om = new ObjectMapper();

		try {
			this.connected = new CountDownLatch(1);
			Watcher watcher = new Watcher() {
				@Override
				public void process(WatchedEvent we) {
					if (we.getState() == KeeperState.SyncConnected) {
						connected.countDown();
					}
				}
			};
			this.zookeeper = new ZooKeeper(this.zkHost + ":" + this.zkPort, 300000000, watcher);
			connected.await();
			this.zookeeper.create(zkNodeName, null, ZooDefs.Ids.OPEN_ACL_UNSAFE,
					CreateMode.PERSISTENT);
		} catch (IOException | KeeperException | InterruptedException e) {
			logger.error(e);
		}
	}

	private KVAdminMessage getAdminMessage() {
		try {
			Stat zkStat = zookeeper.exists(zkNodeName, false);
			byte[] jsonBytes = this.zookeeper.getData(zkNodeName, null, zkStat);

			if (jsonBytes == null)
				return null;

			String json = new String(jsonBytes);
			if (!this.state.equals(json)) {
				return this.om.readValue(json, KVAdminMessage.class);
			}
		} catch (JsonProcessingException | KeeperException | InterruptedException e) {
			logger.warn(e);
		}
		return null;
	}

	private String sendAdminMessage(KVAdminMessage msg) {
		// TODO: send response to ecs when complete
		HashRing metaData = msg.getMetaData();
		IECSNode node = metaData.getHashRing().get(msg.getHashKey());

		switch (msg.getAction()) {
			case INIT:
				// this.server.initKVServer(msg.getMetaData(), node.getCacheSize(),
				// node.getCacheStrategy());
				msg.setAction(ActionType.INIT_ACK);
				break;
			case START:
				this.server.start();
				msg.setAction(ActionType.START_ACK);
				break;
			case STOP:
				this.server.stop();
				msg.setAction(ActionType.STOP_ACK);
				break;
			case SHUTDOWN:
				this.server.shutDown();
				msg.setAction(ActionType.SHUTDOWN_ACK);
				break;
			case LOCK_WRITE:
				this.server.lockWrite();
				msg.setAction(ActionType.LOCK_WRITE_ACK);
				break;
			case UNLOCK_WRITE:
				this.server.unlockWrite();
				msg.setAction(ActionType.UNLOCK_WRITE_ACK);
				break;
			case IS_WRITER_LOCKED:
				this.server.isWriterLocked();
				msg.setAction(ActionType.IS_WRITER_LOCKED_ACK);
				break;
			case MOVE_DATA:
				// must be range and key of newly added/removed node
				this.server.moveData(node.getNodeHashRange(), msg.getHashKey());
				msg.setAction(ActionType.MOVE_DATA_ACK);
				break;
			case UPDATE:
				this.server.updateMetaData(msg.getMetaData());
				msg.setAction(ActionType.UPDATE_ACK);
				break;
			case GET_METADATA:
				this.server.getMetaData();
				msg.setAction(ActionType.GET_METADATA_ACK);
				break;
			case GET_SERVER_STATE:
				this.server.getServerState();
				msg.setAction(ActionType.GET_SERVER_STATE_ACK);
				break;
		}

		try {
			Stat zkStat = zookeeper.exists(zkNodeName, false);
			String json = this.om.writeValueAsString(msg);
			byte[] jsonBytes = KVCommModule.toByteArray(json);
			zookeeper.setData(zkNodeName, jsonBytes, zkStat.getVersion());
			return json;
		} catch (JsonProcessingException | KeeperException | InterruptedException e) {
			logger.error(e);
		}

		return null;
	}

	// Called by server only
	public void run() {
		try {
			while (running) {
				KVAdminMessage adminMsg = getAdminMessage();
				if (adminMsg != null)
					this.state = sendAdminMessage(adminMsg);
			}
		} catch (Exception e) {
			logger.error(e);
		} finally {
			// TODO
		}
	}

	public boolean isRunning() {
		return this.running;
	}

	public void setRunning(boolean run) {
		this.running = run;
	}
}
