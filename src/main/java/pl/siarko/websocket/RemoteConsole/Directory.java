package pl.siarko.websocket.RemoteConsole;

import java.nio.file.Path;

public class Directory {
    private Path path;

    public Directory(){};

    public Directory(Path path) {
        this.path = path;
    }

    public void set(Path p){
        this.path = p;
    }

    public Path get(){
        return this.path;
    }

    public String toString(){
        if(path == null){
            return "/DISK";
        }
        return path.toString();
    }
}
