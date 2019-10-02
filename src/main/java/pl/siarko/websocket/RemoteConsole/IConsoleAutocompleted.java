package pl.siarko.websocket.RemoteConsole;

import com.neovisionaries.ws.client.WebSocket;
import pl.siarko.json.JSON;

public interface IConsoleAutocompleted {
    void handle(WebSocket ws, JSON data);
}
