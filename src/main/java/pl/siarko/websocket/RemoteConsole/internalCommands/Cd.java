package pl.siarko.websocket.RemoteConsole.internalCommands;

import pl.siarko.websocket.RemoteConsole.Directory;
import pl.siarko.websocket.RemoteConsole.IInternalCommand;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Cd implements IInternalCommand {
    @Override
    public String run(ArrayList<String> params, Directory workingDirectory) {


        //cd -> show path
        //cd .. -> go up
        //cd ../../.. -> go up several times
        //cd / -> go to root
        //cd ../sasad/../asdasdas/asda

        if(params.size() == 1){ //show path
            return workingDirectory.toString();
        }
        if(params.get(1).equals("/")){ //go to root
            workingDirectory.set(null);
            return workingDirectory.toString();
        }

        if(workingDirectory.get() == null){
            this.traverse(workingDirectory, params.get(1));
            return workingDirectory.toString();
        }

        String[] parts = params.get(1).split("\\/");
        for (String part : parts) {
            this.traverse(workingDirectory, part);
        }
        return workingDirectory.toString();

    }

    private void traverse(Directory dir, String location){
        if(dir.get() == null){
            File f = Paths.get(location).toFile();
            if(f.exists() && f.isDirectory()){
                dir.set(f.toPath());
            }
        }else{
            if(location.equals("..")){
                dir.set(dir.get().getParent());
            }else{
                File f = dir.get().resolve(location).toFile();
                if(f.exists() && f.isDirectory()){
                    dir.set(f.toPath());
                }
            }
        }
    }
}
