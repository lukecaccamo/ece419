package app_kvECS;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import logger.LogSetup;

import java.util.Map;
import java.util.Collection;
import java.util.Properties;
import java.io.*;

import ecs.IECSNode;

public class ECSClient implements IECSClient {

    private static Logger logger = Logger.getRootLogger();
    private static final String PROMPT = "ECSClient> ";
    private BufferedReader stdin;
    private boolean stop = false;

    public ECSClient(String configFilePath) {
        Properties prop = new Properties();
        try {
            InputStream configFile = new FileInputStream(configFilePath);
            prop.load(configFile);
            configFile.close();
            for (String key : prop.stringPropertyNames()) {
                String[] value = prop.getProperty(key).split("\\s+");
                String hostName = value[0];
                int port = Integer.parseInt(value[1]);
                System.out.println(key + " => " + hostName + ":" + port);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (!this.stop) {
            this.stdin = new BufferedReader(new InputStreamReader(System.in));
            System.out.print(PROMPT);

            try {
                String cmdLine = this.stdin.readLine();
                this.handleCommand(cmdLine);
            } catch (IOException e) {
                this.stop = true;
                printError("CLI does not respond - Application terminated ");
            } catch (Exception e) {
                this.logger.error(e);
                e.printStackTrace();
            }
        }
    }

    private void handleCommand(String cmdLine) throws Exception {
        String[] tokens = cmdLine.split("\\s+");

        switch (tokens[0]) {
            case "quit":
                this.stop = true;
                this.shutdown();
                System.out.println(PROMPT + "Stopped all server instances!");
                break;
            case "start":
                if (tokens.length == 1) {
                    this.start();
                } else {
                    printError("Invalid number of parameters!");
                }
                break;
            case "stop":
                if (tokens.length == 1) {
                    this.stop();
                } else {
                    printError("Invalid number of parameters!");
                }
                break;
            case "add":
                if (tokens.length == 3) {
                } else {
                    this.printError("Invalid number of parameters!");
                }
                break;
            case "remove":
                if (tokens.length == 2) {
                } else {
                    this.printError("Invalid number of parameters!");
                }
                break;
            case "logLevel":
                if (tokens.length == 2) {
                    String level = setLevel(tokens[1]);
                    if (level.equals(LogSetup.UNKNOWN_LEVEL)) {
                        this.printError("No valid log level!");
                        this.printPossibleLogLevels();
                    } else {
                        System.out.println(PROMPT + "Log level changed to level " + level);
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
    public boolean start() {
        /*
         * Starts the storage service by calling start() on all KVServer instances that participate
         * in the service.
         */
        return false;
    }

    @Override
    public boolean stop() {
        /*
         * Stops the service; all participating KVServers are stopped for processing client requests
         * but the processes remain running.
         */
        return false;
    }

    @Override
    public boolean shutdown() {
        /*
         * Stops all server instances and exits the remote processes.
         */
        return false;
    }

    @Override
    public IECSNode addNode(String cacheStrategy, int cacheSize) {
        /*
         * Randomly choose <numberOfNodes> servers from the available machines and start the
         * KVServer by issuing an SSH call to the respective machine. This call launches the storage
         * server with the specified cache size and replacement strategy. For simplicity, locate the
         * KVServer.jar in the same directory as the ECS. All storage servers are initialized with
         * the metadata and any persisted data, and remain in state stopped.
         */
        return null;
    }

    @Override
    public Collection<IECSNode> addNodes(int count, String cacheStrategy, int cacheSize) {
        /*
         * Create a new KVServer with the specified cache size and replacement strategy and add it
         * to the storage service at an arbitrary position.
         */
        return null;
    }

    @Override
    public Collection<IECSNode> setupNodes(int count, String cacheStrategy, int cacheSize) {
        // TODO
        return null;
    }

    @Override
    public boolean awaitNodes(int count, int timeout) throws Exception {
        // TODO
        return false;
    }

    @Override
    public boolean removeNodes(Collection<String> nodeNames) {
        /*
         * Remove a server from the storage service at an arbitrary position.
         */
        return false;
    }

    @Override
    public Map<String, IECSNode> getNodes() {
        // TODO
        return null;
    }

    @Override
    public IECSNode getNodeByKey(String Key) {
        // TODO
        return null;
    }

    private void printHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append(PROMPT).append("KV CLIENT HELP (Usage):\n");
        sb.append(PROMPT);
        sb.append("::::::::::::::::::::::::::::::::");
        sb.append("::::::::::::::::::::::::::::::::\n");
        sb.append(PROMPT).append("start");
        sb.append("\t\t\t starts the storage service on all KVServer instances that participate \n");
        sb.append(PROMPT).append("stop");
        sb.append("\t\t\t\t all participating KVServers are stopped for processing client requests but the processes remain running \n");

        sb.append(PROMPT).append("add <n> <size> <strategy>");
        sb.append("\t choose <n> servers from available machines and start them with cache options <size> and <strategy> \n");
        sb.append(PROMPT).append("remove <i>");
        sb.append("\t\t\t remove a server from the storage service at position <i> \n");

        sb.append(PROMPT).append("logLevel");
        sb.append("\t\t\t changes the logLevel \n");
        sb.append(PROMPT).append("\t\t\t\t ");
        sb.append("ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF \n");

        sb.append(PROMPT).append("quit ");
        sb.append("\t\t\t exits the program");
        System.out.println(sb.toString());
    }

    private void printPossibleLogLevels() {
        System.out.println(PROMPT + "Possible log levels are:");
        System.out.println(PROMPT + "ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF");
    }

    private String setLevel(String levelString) {

        if (levelString.equals(Level.ALL.toString())) {
            this.logger.setLevel(Level.ALL);
            return Level.ALL.toString();
        } else if (levelString.equals(Level.DEBUG.toString())) {
            this.logger.setLevel(Level.DEBUG);
            return Level.DEBUG.toString();
        } else if (levelString.equals(Level.INFO.toString())) {
            this.logger.setLevel(Level.INFO);
            return Level.INFO.toString();
        } else if (levelString.equals(Level.WARN.toString())) {
            this.logger.setLevel(Level.WARN);
            return Level.WARN.toString();
        } else if (levelString.equals(Level.ERROR.toString())) {
            this.logger.setLevel(Level.ERROR);
            return Level.ERROR.toString();
        } else if (levelString.equals(Level.FATAL.toString())) {
            this.logger.setLevel(Level.FATAL);
            return Level.FATAL.toString();
        } else if (levelString.equals(Level.OFF.toString())) {
            this.logger.setLevel(Level.OFF);
            return Level.OFF.toString();
        } else {
            return LogSetup.UNKNOWN_LEVEL;
        }
    }

    private void printError(String error) {
        System.out.println(PROMPT + "Error! " + error);
    }

    public static void main(String[] args) {
        try {
            new LogSetup("logs/ecs.log", Level.ALL);
            if (args.length != 1) {
                System.out.println("Error! Invalid number of arguments!");
                System.out.println("Usage: ECS <configFilePath>");
            } else {
                ECSClient ecsClient = new ECSClient(args[0]);
                ecsClient.run();
            }
        } catch (IOException e) {
            System.out.println("Error! Unable to initialize logger!");
            e.printStackTrace();
            System.exit(1);
        } catch (NumberFormatException nfe) {
            System.out.println("Error! Invalid argument <port>! Not a number!");
            System.out.println("Usage: Server <port>!");
            System.exit(1);
        }
    }
}
