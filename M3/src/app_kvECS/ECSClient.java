package app_kvECS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import logger.LogSetup;

import ecs.ECS;
import ecs.ECSNode;
import ecs.IECSNode;
import shared.Prompt;
import shared.Prompt.*;

public class ECSClient implements IECSClient {

    private static Logger logger;
    private Prompt prompt = new Prompt("ECSAdmin");
    private BufferedReader stdin;
    private boolean stop = false;
    private ECS ecs;

    public ECSClient(String configFilePath) {
        this.logger = Logger.getRootLogger();
        this.ecs = new ECS(configFilePath);
    }

    public void run() {
        this.printUsedServers();
        this.printFreeServers();
        while (!this.stop) {
            this.stdin = new BufferedReader(new InputStreamReader(System.in));
            this.prompt.print();

            try {
                String cmdLine = this.stdin.readLine();
                this.handleCommand(cmdLine);
            } catch (IOException e) {
                this.stop = true;
                this.prompt.printError("CLI does not respond - Application terminated ");
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
                this.prompt.printPrimary("Stopped all server instances!");
                break;
            case "start":
                this.start();
                break;
            case "stop":
                this.stop();
                break;
            case "add":
                if (tokens.length == 3) {
                    String cacheStrategy = tokens[1];
                    int cacheSize = Integer.parseInt(tokens[2]);
                    this.addNode(cacheStrategy, cacheSize);
                } else if (tokens.length == 4) {
                    int count = Integer.parseInt(tokens[1]);
                    String cacheStrategy = tokens[2];
                    int cacheSize = Integer.parseInt(tokens[3]);
                    this.addNodes(count, cacheStrategy, cacheSize);
                } else {
                    this.prompt.printError("Invalid number of parameters!");
                }
                break;
            case "remove":
                if (tokens.length == 2) {
                    String nodeName = tokens[1];
                    Collection<String> nodeNames = new ArrayList<>();
                    nodeNames.add(nodeName);
                    this.removeNodes(nodeNames);
                } else {
                    this.prompt.printError("Invalid number of parameters!");
                }
                break;
            case "logLevel":
                if (tokens.length == 2) {
                    String level = setLevel(tokens[1]);
                    if (level.equals(LogSetup.UNKNOWN_LEVEL)) {
                        this.prompt.printError("No valid log level!");
                        this.prompt.printPossibleLogLevels();
                    } else {
                        this.prompt.printPrimary("Log level changed to level " + level);
                    }
                } else {
                    this.prompt.printError("Invalid number of parameters!");
                }
                break;
            case "help":
                this.printHelp();
                break;
            default:
                this.prompt.printError("Unknown command");
                this.printHelp();
        }

        this.printUsedServers();
        this.printFreeServers();
    }

    /**
     * Starts the storage service by calling start() on all KVServer instances that
     * participate in the service.\
     * 
     * @throws Exception some meaningfull exception on failure
     * @return true on success, false on failure
     */
    @Override
    public boolean start() {
        return this.ecs.start();
    }

    /**
     * Stops the service; all participating KVServers are stopped for processing
     * client requests but the processes remain running.
     * 
     * @throws Exception some meaningfull exception on failure
     * @return true on success, false on failure
     */
    @Override
    public boolean stop() {
        return this.ecs.stop();
    }

    /**
     * Stops all server instances and exits the remote processes.
     * 
     * @throws Exception some meaningfull exception on failure
     * @return true on success, false on failure
     */
    @Override
    public boolean shutdown() {
        return this.ecs.shutdown();
    }

    /**
     * Create a new KVServer with the specified cache size and replacement strategy
     * and add it to the storage service at an arbitrary position.
     * 
     * @return name of new server
     */
    @Override
    public IECSNode addNode(String cacheStrategy, int cacheSize) {
        return this.ecs.addNode(cacheStrategy, cacheSize);
    }

    /**
     * Randomly choose <numberOfNodes> servers from the available machines and start
     * the KVServer by issuing an SSH call to the respective machine. This call
     * launches the storage server with the specified cache size and replacement
     * strategy. For simplicity, locate the KVServer.jar in the same directory as
     * the ECS. All storage servers are initialized with the metadata and any
     * persisted data, and remain in state stopped. NOTE: Must call setupNodes
     * before the SSH calls to start the servers and must call awaitNodes before
     * returning
     * 
     * @return set of strings containing the names of the nodes
     */
    @Override
    public Collection<IECSNode> addNodes(int count, String cacheStrategy, int cacheSize) {
        return this.ecs.addNodes(count, cacheStrategy, cacheSize);
    }

    /**
     * Sets up `count` servers with the ECS (in this case Zookeeper)
     * 
     * @return array of strings, containing unique names of servers
     */
    @Override
    public Collection<IECSNode> setupNodes(int count, String cacheStrategy, int cacheSize) {
        return this.ecs.setupNodes(count, cacheStrategy, cacheSize);
    }

    /**
     * Wait for all nodes to report status or until timeout expires
     * 
     * @param count   number of nodes to wait for
     * @param timeout the timeout in milliseconds
     * @return true if all nodes reported successfully, false otherwise
     */
    @Override
    public boolean awaitNodes(int count, int timeout) throws Exception {
        return this.ecs.awaitNodes(count, timeout);
    }

    /**
     * Removes nodes with names matching the nodeNames array
     * 
     * @param nodeNames names of nodes to remove
     * @return true on success, false otherwise
     */
    @Override
    public boolean removeNodes(Collection<String> nodeNames) {
        return this.ecs.removeNodes(nodeNames);
    }

    /**
     * Get a map of all nodes
     */
    @Override
    public Map<String, IECSNode> getNodes() {
        return this.ecs.getNodes();
    }

    /**
     * Get the specific node responsible for the given key
     */
    @Override
    public IECSNode getNodeByKey(String Key) {
        return this.ecs.getNodeByKey(Key);
    }

    private void printUsedServers() {
        this.prompt.printPrimary("UsedServers:");
        for (Map.Entry<String, IECSNode> e : this.getNodes().entrySet()) {
            IECSNode node = e.getValue();
            String str = String.format("\t%s(%s:%d) flag: %s range: (%s,%s) cache: %s(%d)", node.getNodeName(),
                    node.getNodeHost(), node.getNodePort(), node.getFlag().toString(), node.getNodeHashRange()[0],
                    node.getNodeHashRange()[1], node.getCacheStrategy().toString(), node.getCacheSize());
            this.prompt.printPrimary(str);
        }
    }

    private void printFreeServers() {
        this.prompt.printSecondary("FreeServers:");
        for (IECSNode node : this.ecs.freeServers) {
            String str = String.format("\t%s(%s:%d) flag: %s range: (%s,%s) cache: %s(%d)", node.getNodeName(),
                    node.getNodeHost(), node.getNodePort(), node.getFlag().toString(), node.getNodeHashRange()[0],
                    node.getNodeHashRange()[1], node.getCacheStrategy().toString(), node.getCacheSize());
            this.prompt.printSecondary(str);
        }
    }

    private void printHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.prompt.get() + "\n");

        sb.append(this.prompt.get()).append(Prompt.ANSI_BOLD + Prompt.ANSI_BLUE + "ECS CLIENT HELP (Usage):\n" + Prompt.ANSI_RESET);

        sb.append(this.prompt.get()).append(Prompt.ANSI_BOLD + "start" + Prompt.ANSI_RESET);
        sb.append("\t\t\t starts the storage service on all KVServer instances that participate \n");

        sb.append(this.prompt.get()).append(Prompt.ANSI_BOLD + "stop" + Prompt.ANSI_RESET);
        sb.append(
                "\t\t\t all participating KVServers are stopped for processing client requests but the processes remain running \n");

        sb.append(this.prompt.get()).append(Prompt.ANSI_BOLD + "add " + Prompt.ANSI_BLUE + "<strategy> <size>" + Prompt.ANSI_RESET);
        sb.append("\t choose a server from available machines and start it with cache options " + Prompt.ANSI_BLUE
                + "<strategy> " + Prompt.ANSI_RESET + "and " + Prompt.ANSI_BLUE + "<size> \n");

        sb.append(this.prompt.get()).append(Prompt.ANSI_BOLD + "add " + Prompt.ANSI_BLUE + "<m> <strategy> <size>" + Prompt.ANSI_RESET);
        sb.append("\t choose " + Prompt.ANSI_BLUE + "<m> " + Prompt.ANSI_RESET
                + "servers from available machines and start them with cache options " + Prompt.ANSI_BLUE + "<strategy> "
                + Prompt.ANSI_RESET + "and " + Prompt.ANSI_BLUE + "<size> \n");

        sb.append(this.prompt.get()).append(Prompt.ANSI_BOLD + "remove " + Prompt.ANSI_BLUE + "<name>" + Prompt.ANSI_RESET);
        sb.append(
                "\t\t remove a server from the storage service with name " + Prompt.ANSI_BLUE + "<name>" + Prompt.ANSI_RESET + "\n");

        sb.append(this.prompt.get()).append(Prompt.ANSI_BOLD + "logLevel" + Prompt.ANSI_RESET);
        sb.append("\t\t\t changes the logLevel: \n");
        sb.append(this.prompt.get()).append("\t\t\t\t ");
        sb.append(Prompt.ANSI_BLUE + "ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF \n" + Prompt.ANSI_RESET);

        sb.append(this.prompt.get()).append(Prompt.ANSI_BOLD + "quit " + Prompt.ANSI_RESET);
        sb.append("\t\t\t exits the program \n");

        sb.append(this.prompt.get());
        System.out.println(sb.toString());
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

    public static void main(String[] args) {
        try {
            new LogSetup("logs/ecs.log", Level.WARN);
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