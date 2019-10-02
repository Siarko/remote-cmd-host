package pl.siarko.websocket.RemoteConsole;

import com.neovisionaries.ws.client.WebSocket;

public interface IConsoleOutBuff {
    void handle(WebSocket ws, String msg, boolean stdErr);
}
