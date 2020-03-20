package shared;

public class Prompt {
    public static final String ANSI_BOLD = "\u001B[1m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_WHITE = "\u001B[37m";

    private String line;

    public Prompt(String line) {
        this.line = ANSI_BOLD + ANSI_CYAN + line + ANSI_WHITE + " % " + ANSI_RESET;
    }

    public void print() {
        System.out.print(this.line);
    }

    public String get() {
        return this.line;
    }

    public void printPrimary(String info) {
        System.out.println(this.line + ANSI_BOLD + ANSI_BLUE + info + ANSI_RESET);
    }

    public void printSecondary(String info) {
        System.out.println(this.line + ANSI_BOLD + ANSI_PURPLE + info + ANSI_RESET);
    }

    public void printError(String error) {
        System.out.println(this.line + ANSI_BOLD + ANSI_RED + "Error! " + error + ANSI_RESET);
    }

    public void printPossibleLogLevels() {
        System.out.println(this.line + "Possible log levels are:");
        System.out.println(this.line + "ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF");
    }
}