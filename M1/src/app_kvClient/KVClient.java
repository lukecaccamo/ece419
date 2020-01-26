package app_kvClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import logger.LogSetup;

import client.KVCommInterface;
import client.KVStore;
import shared.communications.KVCommModule;
import shared.messages.KVSimpleMessage;

public class KVClient implements IKVClient {

    private static Logger logger = Logger.getRootLogger();
    private static final String PROMPT = "KVClient> ";
    private BufferedReader stdin;
    private KVStore store = null;
	private boolean stop = false;

	private String serverAddress;
	private int serverPort;

    public void run() {
		while(!this.stop) {
			this.stdin = new BufferedReader(new InputStreamReader(System.in));
			System.out.print(PROMPT);
			
			try {
				String cmdLine = this.stdin.readLine();
				this.handleCommand(cmdLine);
			} catch (IOException e) {
				this.stop = true;
				printError("CLI does not respond - Application terminated ");
			}
		}
    }
    
    private void handleCommand(String cmdLine) {
        String[] tokens = cmdLine.split("\\s+");

        switch (tokens[0]) {
            case "quit":
                this.stop = true;
			    this.store.disconnect();
			    System.out.println(PROMPT + "Application exit!");
                break;
            case "connect":
                if(tokens.length == 3) {
			    	try{
			    		serverAddress = tokens[1];
						serverPort = Integer.parseInt(tokens[2]);
						this.newConnection(serverAddress, serverPort);
			    	} catch (NumberFormatException nfe) {
						printError("No valid address. Port must be a number!");
						logger.info("Unable to parse argument <port>", nfe);
			    	} catch (UnknownHostException e) {
						printError("Unknown Host!");
						logger.info("Unknown Host!", e);
			    	} catch (IOException e) {
						printError("Could not establish connection!");
						logger.warn("Could not establish connection!", e);
			    	} catch (Exception e) {
						// TODO: Remove this?
						logger.warn(e);
						e.printStackTrace();
					}
			    } else {
			    	printError("Invalid number of parameters!");
			    }
				break;
			case "put":
                if(tokens.length >= 3) {
			    	if(this.store != null && this.store.isRunning()){
						String key = tokens[1];
						StringBuilder value = new StringBuilder();
						for(int i = 2; i < tokens.length; i++) {
							value.append(tokens[i]);
							if (i != tokens.length -1 ) {
								value.append(" ");
							}
						}
						try {
							KVSimpleMessage message = this.store.put(key, value.toString());
							this.handleNewMessage(message);
						} catch (IOException e) {
							printError("Unable to send message!");
							this.store.disconnect();
						} catch (Exception e) {
							// TODO: Remove this?
							logger.warn(e);
							e.printStackTrace();
						}
			    	} else {
			    		this.printError("Not connected!");
			    	}
			    } else {
			    	this.printError("Invalid number of parameters!");
			    }
                break;
            case "get":
                if(tokens.length == 2) {
			    	if(this.store != null && this.store.isRunning()){
						String key = tokens[1];
						try {
							KVSimpleMessage message = this.store.get(key);
							this.handleNewMessage(message);
						} catch (IOException e) {
							printError("Unable to send message!");
							this.store.disconnect();
						} catch (Exception e) {
							// TODO: Remove this?
							logger.warn(e);
							e.printStackTrace();
						}
			    	} else {
			    		this.printError("Not connected!");
			    	}
			    } else {
			    	this.printError("Invalid number of parameters!");
			    }
                break;
            case "disconnect":
				this.store.disconnect();
                break;
            case "logLevel":
                if(tokens.length == 2) {
			    	String level = setLevel(tokens[1]);
			    	if(level.equals(LogSetup.UNKNOWN_LEVEL)) {
			    		this.printError("No valid log level!");
			    		this.printPossibleLogLevels();
			    	} else {
			    		System.out.println(PROMPT + 
			    				"Log level changed to level " + level);
			    	}
			    } else {
			    	printError("Invalid number of parameters!");
			    }
                break;
            case "help":
                this.printHelp();
                break;
            default:
                printError("Unknown command");
                this.printHelp();
        }
    }

    @Override
    public void newConnection(String hostname, int port) throws Exception {
		this.store = new KVStore(hostname, port);
        this.store.connect();
    }

    @Override
    public KVCommInterface getStore(){
        return this.store;
    }
    
    private void printHelp() {
		StringBuilder sb = new StringBuilder();
		sb.append(PROMPT).append("KV CLIENT HELP (Usage):\n");
		sb.append(PROMPT);
		sb.append("::::::::::::::::::::::::::::::::");
		sb.append("::::::::::::::::::::::::::::::::\n");
		sb.append(PROMPT).append("connect <host> <port>");
		sb.append("\t establishes a connection to a server\n");
		sb.append(PROMPT).append("disconnect");
		sb.append("\t\t\t disconnects from the server \n");

		sb.append(PROMPT).append("put <key> <value>");
		sb.append("\t\t inserts/updates/deletes a key-value pair in the storage server \n");
		sb.append(PROMPT).append("get <key>");
		sb.append("\t\t retrieves a key-value pair from the storage server \n");
		
		sb.append(PROMPT).append("logLevel");
		sb.append("\t\t\t changes the logLevel \n");
		sb.append(PROMPT).append("\t\t\t\t ");
		sb.append("ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF \n");
		
		sb.append(PROMPT).append("quit ");
		sb.append("\t\t\t exits the program");
		System.out.println(sb.toString());
    }
    
    private void printPossibleLogLevels() {
		System.out.println(PROMPT 
				+ "Possible log levels are:");
		System.out.println(PROMPT 
				+ "ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF");
    }

    private String setLevel(String levelString) {
		
		if(levelString.equals(Level.ALL.toString())) {
			this.logger.setLevel(Level.ALL);
			return Level.ALL.toString();
		} else if(levelString.equals(Level.DEBUG.toString())) {
			this.logger.setLevel(Level.DEBUG);
			return Level.DEBUG.toString();
		} else if(levelString.equals(Level.INFO.toString())) {
			this.logger.setLevel(Level.INFO);
			return Level.INFO.toString();
		} else if(levelString.equals(Level.WARN.toString())) {
			this.logger.setLevel(Level.WARN);
			return Level.WARN.toString();
		} else if(levelString.equals(Level.ERROR.toString())) {
			this.logger.setLevel(Level.ERROR);
			return Level.ERROR.toString();
		} else if(levelString.equals(Level.FATAL.toString())) {
			this.logger.setLevel(Level.FATAL);
			return Level.FATAL.toString();
		} else if(levelString.equals(Level.OFF.toString())) {
			this.logger.setLevel(Level.OFF);
			return Level.OFF.toString();
		} else {
			return LogSetup.UNKNOWN_LEVEL;
		}
	}

	public void handleNewMessage(KVSimpleMessage msg) {
		if(!stop) {
			System.out.println(msg.getMsg());
			System.out.print(PROMPT);
		}
	}

    private void printError(String error){
		System.out.println(PROMPT + "Error! " +  error);
    }
    
    /**
     * Main entry point for the KV client application. 
     * @param args contains the port number at args[0].
     */
    public static void main(String[] args) {
    	try {
			new LogSetup("logs/client.log", Level.OFF);
			KVClient client = new KVClient();
            client.run();
		} catch (IOException e) {
			System.out.println("Error! Unable to initialize logger!");
			e.printStackTrace();
			System.exit(1);
		}
    }
}
