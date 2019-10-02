package pl.siarko.websocket;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import org.json.JSONObject;
import pl.siarko.RemoteCmdClient;
import pl.siarko.json.JSON;

import java.io.IOException;

public class WebsocketConnection {

    public enum RequestType{
        RESPONSE("response"), STREAM("stream");

        private String id;
        private RequestType(String id){
            this.id = id;
        }

        public String toString(){
            return this.id;
        }
    }

    private boolean active = false;
    private WebSocket wsContext;

    public WebsocketConnection(String address) throws IOException {
        this.wsContext = (new WebSocketFactory()).createSocket(address);
    }

    public boolean start(){
        if(!this.active){
            this.active = true;
            try {
                this.wsContext.connect();
                return true;
            } catch (WebSocketException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /*
    * client_type: host
    * type: response
    * name: ''
    * data: {}
    *
    * client_type: host
    * type: stream
    * data: frame_data
    *
    * */

    public static String formatMesage(String name, Object data){
        try{
            if(data == null){
                data = new JSONObject();
            }
            JSON json = new JSON();
            json.put("client_type", "host");
            json.put("name", name);
            json.put("payload", data);
            json.put("hostid", RemoteCmdClient.ID);
            return json.toString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;

    }
}
