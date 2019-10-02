package pl.siarko;

import java.io.File;
import java.io.IOException;

public class Lockfile {

    private static File lockfile = new File(RemoteCmdClient.LOCKFILE);

    public Lockfile(){
        lockfile.deleteOnExit();
    }

    public boolean exists(){
        return lockfile.exists();
    }

    public void create(){
        try {
            lockfile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void delete(){
        lockfile.delete();
    }
}
