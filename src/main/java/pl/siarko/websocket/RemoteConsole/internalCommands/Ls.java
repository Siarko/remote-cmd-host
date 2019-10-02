package pl.siarko.websocket.RemoteConsole.internalCommands;

import org.apache.commons.lang3.StringUtils;
import pl.siarko.websocket.RemoteConsole.Directory;
import pl.siarko.websocket.RemoteConsole.IInternalCommand;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Ls implements IInternalCommand {
    @Override
    public String run(ArrayList<String> params, Directory workingDirectory) {


        File[] files;
        if(workingDirectory.get() == null){
            //space above all directories
            files = File.listRoots();
        }else{
            File dir = workingDirectory.get().toFile();
            files = dir.listFiles();
        }

        HashMap<String, File> fileList = new HashMap<>();
        int longestName = 8;

        if(files == null){
            return "NULL";
        }

        StringBuilder list = new StringBuilder();
        for (File f : Objects.requireNonNull(files)) {
            if(f.getName().length() == 0){
                fileList.put(f.getAbsolutePath(), f);
            }else{
                fileList.put(f.getName(), f);
            }
            if(f.getName().length() > longestName){
                longestName = f.getName().length();
            }
        }


        longestName += 2;
        list.append("TYPE");
        list.append(StringUtils.rightPad("  NAME", longestName+2));
        list.append("SIZE").append("\n");
        for (Map.Entry<String, File> p :fileList.entrySet()) {
            if(p.getValue().isDirectory()){
                list.append("[D] ");
            }else{
                list.append("[ ] ");
            }
            list.append(StringUtils.rightPad(p.getKey(), longestName));
            list.append(p.getValue().length());
            list.append("\n");
        }

        return list.toString();
    }
}
