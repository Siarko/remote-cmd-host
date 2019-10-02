package pl.siarko.websocket;

import com.neovisionaries.ws.client.*;
import org.json.JSONObject;
import pl.siarko.Profiler.Profiler;
import pl.siarko.RemoteCmdClient;
import pl.siarko.json.JSON;
import pl.siarko.websocket.RemoteConsole.ConsoleHandler;
import pl.siarko.websocket.RemoteConsole.IConsoleOutBuff;
import pl.siarko.websocket.desktopStream.DesktopStream;

import java.util.List;
import java.util.Map;

public class WebsocketsHandler {

    private boolean active = true;

    private DesktopStream desktopStream;
    private ConsoleHandler consoleHandler;

    public WebsocketsHandler(){

        this.desktopStream = new DesktopStream();
        this.consoleHandler = new ConsoleHandler();

        this.consoleHandler.onOut((ws, msg, stdErr) -> {
            JSONObject data = new JSONObject();
            data.put("lines", msg);
            data.put("is_err", stdErr);
            ws.sendText(WebsocketConnection.formatMesage(
                    "console_output", data
            ));
        });

        this.consoleHandler.onAutocompleteDone((ws, data) -> {
            ws.sendText(WebsocketConnection.formatMesage(
                    "console_autocomplete", data.rawObject()
            ));
        });

        try {
            WebSocket ws = new WebSocketFactory()
                    .createSocket(RemoteCmdClient.websocketServiceUrl)
                    .addListener(new WebSocketAdapter(){
                        @Override
                        public void onTextMessage(WebSocket ws, String message){
                            try{
                                JSON json = new JSON(message);
                                if(json.get("name").equals("user_disconnect")){
                                    System.out.println("Disconnect");
                                    active = false;
                                    ws.disconnect();

                                }else if(json.get("name").equals("stop_stream")){
                                    System.out.println("STOP STREAM");
                                    desktopStream.stop();
                                } else if(json.get("name").equals("start_stream")){
                                    if(desktopStream.isActive()){return;}
                                    int fps = (int) json.get("payload.fps");
                                    int fpc = (int) json.get("payload.fpc");
                                    int fw = (int) json.get("payload.fw");
                                    int fh = (int) json.get("payload.fh");
                                    float compression = json.getFloat("payload.cmp");
                                    desktopStream.start(ws, fps, fpc, fw, fh, compression);
                                }else if(json.get("name").equals("console_command")){
                                    String command = json.getString("payload.command");
                                    boolean isSignal = json.getBoolean("payload.signal");
                                    consoleHandler.input(command, isSignal);
                                }else if(json.get("name").equals("console_autocomplete")){
                                    String command = json.getString("payload.command");
                                    consoleHandler.autocompleteRequest(command);
                                }else if(json.get("name").equals("host_ready")){
                                    if(json.getBoolean("payload.succeed")){
                                        System.out.println("Server confirmed host ready");
                                    }else{
                                        System.out.println("Server denies host ready");
                                    }
                                }else{
                                    System.out.println("Received unknown message: ");
                                    System.out.println(json.prettyPrint());
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
                            System.out.println("Connected to server");
                            Profiler p = new Profiler();
                            websocket.sendText(WebsocketsHandler.this.getHostReadyMessage().toString());
                            p.end("HANDSHAKE");
                        }

                        @Override
                        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
                            System.out.println("Disconnected from server");
                            active = false;
                        }
                    }).connect();
            this.consoleHandler.setWsObject(ws);

        } catch (Exception e) {
            System.out.println("Cannot connect to websocket server...");
            active = false;
        }
    }

    private String getHostReadyMessage(){
        return WebsocketConnection.formatMesage("host_ready", null);
    }

    public boolean isActive(){
        return this.active;
    }
}
