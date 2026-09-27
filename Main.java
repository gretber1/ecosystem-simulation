package alife;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equalsIgnoreCase("console")) ConsoleApp.run();
        else GuiApp.run();
    }
}
