package shared.communications;

import app_kvServer.IKVServer;
import app_kvServer.KVServer;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.zookeeper.*;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.data.Stat;
import java.util.concurrent.CountDownLatch;

import ecs.IECS;
import ecs.IECSNode;
import ecs.IECSNode.IECSNodeFlag;
import ecs.ECSNode;
import shared.hashring.HashRing;
import shared.messages.KVAdminMessage;
import shared.messages.IKVAdminMessage.ActionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.net.*;

public class KVAdminCommModule implements Runnable {

	private static Logger logger = Logger.getRootLogger();

	private boolean running;
	private KVServer server;
	private String nodeName;
	private String nodeHost;
	private int nodePort;

	private ZooKeeper zk;
	private String zkHost;
	private int zkPort;
	private String zkNodeName;
	private CountDownLatch connected;
	private String state;
	private ObjectMapper om;

	public KVAdminCommModule(String nodeName, String zkHost, int zkPort, KVServer server) {
		this.running = true;
		this.server = server;
		this.nodeName = nodeName;

		this.zkHost = zkHost;
		this.zkPort = zkPort;

		this.zkNodeName = IECS.ZOOKEEPER_ADMIN_NODE_NAME + "/" + this.nodeName;
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
			this.zk = new ZooKeeper(this.zkHost + ":" + this.zkPort, 30000, watcher);
			this.connected.await();
			this.zk.create(zkNodeName, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		} catch (IOException | KeeperException | InterruptedException e) {
			this.logger.error(e);
			e.printStackTrace();
		}
	}

	public void run() {
		while (this.running) {
			KVAdminMessage nextState = getNextState();
			if (nextState != null)
				this.state = setNextState(nextState);
		}
		try {
			if (this.zk.exists(zkNodeName, false) != null)
				this.zk.delete(zkNodeName, -1);
		} catch (InterruptedException | KeeperException e) {
			this.logger.error(e);
			e.printStackTrace();
		}
	}

	public void close() {
		this.running = false;
	}

	private KVAdminMessage getNextState() {
		try {
			Stat zkStat = this.zk.exists(zkNodeName, false);
			byte[] jsonBytes = this.zk.getData(zkNodeName, null, zkStat);

			if (jsonBytes != null) {
				String json = new String(jsonBytes).trim();
				if (!this.state.equals(json))
					return this.om.readValue(json, KVAdminMessage.class);
			}
		} catch (JsonProcessingException | KeeperException | InterruptedException e) {
			this.logger.error(e);
			e.printStackTrace();
		}
		return null;
	}

	private String setNextState(KVAdminMessage nextState) {
		String key = nextState.getHashKey();
		HashRing metadata = nextState.getMetaData();
		IECSNode node = metadata.getHashRing().get(key);

		switch (nextState.getAction()) {
			case INIT:
				this.server.initializeServer(key, metadata, node.getCacheSize(), node.getCacheStrategy());
				this.nodeHost = node.getNodeHost();
				this.nodePort = node.getNodePort();
				nextState.setAction(ActionType.INIT_ACK);
				break;
			case START:
				this.server.start();
				node.setFlag(IECSNodeFlag.START);
				nextState.setAction(ActionType.START_ACK);
				break;
			case STOP:
				this.server.stop();
				node.setFlag(IECSNodeFlag.STOP);
				nextState.setAction(ActionType.STOP_ACK);
				break;
			case SHUTDOWN:
				node = new ECSNode(this.nodeName, this.nodeHost, this.nodePort);
				metadata.removeServer(key);
				metadata.addServer(key, node);
				nextState.setMetaData(metadata);
				nextState.setAction(ActionType.SHUTDOWN_ACK);
				this.server.shutDown();
				break;
			case LOCK_WRITE:
				this.server.lockWrite();
				nextState.setAction(ActionType.LOCK_WRITE_ACK);
				break;
			case UNLOCK_WRITE:
				this.server.unlockWrite();
				nextState.setAction(ActionType.UNLOCK_WRITE_ACK);
				break;
			case IS_WRITER_LOCKED:
				this.server.isWriterLocked();
				nextState.setAction(ActionType.IS_WRITER_LOCKED_ACK);
				break;
			case MOVE_DATA:
				try {
					this.server.moveData(node.getNodeHashRange(), key);
				} catch (Exception e) {
					this.logger.error(e);
					e.printStackTrace();
				}
				nextState.setAction(ActionType.MOVE_DATA_ACK);
				break;
			case UPDATE:
				this.server.setMetaData(metadata);
				nextState.setAction(ActionType.UPDATE_ACK);
				break;
			case GET_METADATA:
				this.server.getMetaData();
				nextState.setAction(ActionType.GET_METADATA_ACK);
				break;
			case GET_SERVER_STATE:
				this.server.getServerState();
				nextState.setAction(ActionType.GET_SERVER_STATE_ACK);
				break;
		}

		try {
			Stat zkStat = this.zk.exists(zkNodeName, false);
			String json = this.om.writeValueAsString(nextState).trim();
			byte[] jsonBytes = KVCommModule.toByteArray(json);
			this.zk.setData(zkNodeName, jsonBytes, zkStat.getVersion());
			return json;
		} catch (JsonProcessingException | KeeperException | InterruptedException e) {
			this.logger.error(e);
			e.printStackTrace();
		}

		return null;
	}
}
