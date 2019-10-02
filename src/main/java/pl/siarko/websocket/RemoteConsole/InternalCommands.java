package pl.siarko.websocket.RemoteConsole;

import pl.siarko.websocket.RemoteConsole.internalCommands.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InternalCommands {

    private static HashMap<String, IInternalCommand> commandHashMap = new HashMap<>();
    private static ConsoleHandler consoleHandler = null;

    static {
        commandHashMap.put("cd", new Cd());
        commandHashMap.put("download", new Download());
        commandHashMap.put("ls", new Ls());
    }

    public static boolean isInternalCommand(String c){
        return commandHashMap.containsKey(c);
    }

    public static String runInternalCommand(ConsoleHandler h, ArrayList<String> params, Directory wd){
        consoleHandler = h;
        String result = commandHashMap.get(params.get(0)).run(params, wd);
        consoleHandler = null;
        return result;
    }

    public static ArrayList<String> splitCommand(String command){
        ArrayList<String> parts = new ArrayList<>();
        String regex = "\"([^\"]*)\"|(\\S+)";
        Matcher m = Pattern.compile(regex).matcher(command);
        while (m.find()) {
            parts.add(m.group((m.group(1) != null) ? 1 : 2));
        }
        return parts;
    }

    public static void sendChars(String chars){
        if(consoleHandler != null){
            consoleHandler.sendOut(chars, false);
        }
    }
}
