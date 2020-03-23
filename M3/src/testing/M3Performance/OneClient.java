package testing.M3Performance;

import client.KVStore;
import junit.framework.TestCase;
import org.junit.Test;
import shared.messages.KVMessage;

public class OneClient extends TestCase {

    private static final String address = "localhost";
    private static final int numClients = 1;
    private static final int numPuts = 80;
    private static final int numGets = 160;

    private static final int server1port = 49990;

    Exception ex = null;
    KVStore[] kvClients = new KVStore[numClients];
    KVMessage[] kvMessages = new KVMessage[numClients];

    String key = "email";
    String value = "";

    @Test
    public void testPerf() {

        Emails emails = new Emails();
        String[] messages = emails.messages;

        //connect clients
        for (int i = 0; i < numClients; i++) {
            kvClients[i] = new KVStore(address, server1port);
            try {
                kvClients[i].connect();
            } catch (Exception e) {
                ex = e;
            }
        }

        assertNull(ex);

        int puts = numPuts / numClients;
        for (int k = 0; k < puts; k++) {
            for (int i = 0; i < numClients; i++) {
                try {
                    for (int j = 0; j < messages.length; j++) {
                        kvMessages[i] = kvClients[i].put(key + i, messages[j]);
                        assertTrue(KVMessage.StatusType.PUT_SUCCESS == kvMessages[i].getStatus() || KVMessage.StatusType.PUT_UPDATE == kvMessages[i].getStatus());
                    }
                } catch (Exception e) {
                    ex = e;
                }
            }

        }

        assertNull(ex);

        int gets = numGets / numClients;
        for (int k = 0; k < gets; k++) {
            for (int i = 0; i < numClients; i++) {
                try {
                    for (int j = 0; j < messages.length; j++) {
                        kvMessages[i] = kvClients[i].get(key + i);
                        //assertEquals(KVMessage.StatusType.GET_SUCCESS, kvMessages[i].getStatus());
                    }
                } catch (Exception e) {
                    ex = e;
                }
            }

        }

        assertNull(ex);

        //disconnect clients
        for (int i = 0; i < numClients; i++) {
            try {
                kvClients[i].disconnect();
            } catch (Exception e) {
                ex = e;
            }
        }

    }
}
