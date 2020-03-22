package shared.replication;

import app_kvServer.KVServer;
import ecs.IECSNode;
import org.apache.log4j.Logger;
import shared.communications.KVCommModule;
import shared.hashring.HashRing;
import shared.messages.KVMessage;
import shared.messages.KVSimpleMessage;

import java.io.IOException;
import java.net.Socket;

import static shared.messages.KVMessage.StatusType;

public class Replicator {

    private Logger logger = Logger.getRootLogger();


    private KVCommModule firstReplica;
    private String firstReplicaName;
    private String firstReplicaHash;
    private String[] firstReplicaRange;

    private KVCommModule secondReplica;
    private String secondReplicaName;
    private String secondReplicaHash;
    private String[] secondReplicaRange;

    private KVServer server;
    private String coordinatorHash;

    public Replicator(KVServer server) {
        this.server = server;
    }

    // Return true if success, false on failure
    public boolean replicate(String key, String value) {

        try {

            if(this.firstReplica != null){
                firstReplica.sendKVMessage(StatusType.REPLICATE, key, value);
                KVSimpleMessage response = firstReplica.receiveKVMessage();
                if (response.getStatus() != StatusType.REPLICATION_DONE) {
                    logger.error("Failed to replicate key: " + key + " on server " + firstReplicaName);
                    return false;
                }

                logger.info("Replicated key: " + key + " on server " + firstReplicaName);
            }

            if(this.secondReplica != null) {
                secondReplica.sendKVMessage(StatusType.REPLICATE, key, value);
                KVSimpleMessage response = secondReplica.receiveKVMessage();
                if (response.getStatus() != StatusType.REPLICATION_DONE) {
                    logger.error("Failed to replicate key: " + key + " on server " + secondReplicaName);
                    return false;
                }

                logger.info("Replicated key: " + key + " on server " + secondReplicaName);
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void bulkReplicate() {
        try {
            server.moveData(firstReplicaRange, firstReplicaHash);
            server.moveData(secondReplicaRange, secondReplicaHash);
        } catch (Exception e) {
            logger.error("Error replicating data");
            e.printStackTrace();
        }
    }

    public void connect() {
        HashRing metaData = server.getMetaData();
        this.coordinatorHash = server.getServerHash();

        if(metaData == null || coordinatorHash == null)
            return;

        disconnect();

        IECSNode successor = metaData.getSucc(coordinatorHash);

        //there is a successor that isnt itself
        if (successor != null && !successor.getHashKey().equals(coordinatorHash)) {
            Socket succSocket = null;

            try {
                succSocket = new Socket(successor.getNodeHost(), successor.getNodePort());
                this.firstReplica = new KVCommModule(succSocket, null);
                this.firstReplicaName = successor.getNodeName();
                this.firstReplicaHash = successor.getHashKey();
                this.firstReplicaRange = successor.getNodeHashRange();
                this.firstReplica.connect();
                logger.info("Connected to server: " + firstReplicaName);
            } catch (Exception e) {
                e.printStackTrace();
            }

            IECSNode nextSuccessor = metaData.getSucc(successor.getHashKey());

            if (nextSuccessor != null && !nextSuccessor.getHashKey().equals(coordinatorHash)) {
                Socket nextSuccSocket = null;

                try {
                    nextSuccSocket = new Socket(nextSuccessor.getNodeHost(), nextSuccessor.getNodePort());
                    this.secondReplica = new KVCommModule(nextSuccSocket, null);
                    this.secondReplicaName = nextSuccessor.getNodeName();
                    this.secondReplicaHash = nextSuccessor.getHashKey();
                    this.secondReplicaRange = nextSuccessor.getNodeHashRange();
                    this.secondReplica.connect();
                    logger.info("Connected to server: " + secondReplicaName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void disconnect() {
        if(this.firstReplica != null)
            firstReplica.disconnect();

        if(this.secondReplica != null)
            secondReplica.disconnect();
    }
}
