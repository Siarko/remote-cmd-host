package pl.siarko.websocket.RemoteConsole;

import java.nio.file.Path;
import java.util.ArrayList;

public interface IInternalCommand {
    String run(ArrayList<String> params, Directory workingDirectory);
}
