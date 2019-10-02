package pl.siarko;

import pl.siarko.http.Http;
import pl.siarko.http.UrlParam;
import pl.siarko.json.JSON;
import pl.siarko.websocket.WebsocketsHandler;

import java.io.*;
import java.util.Arrays;
import java.util.UUID;

/*TODO instalator i autostart z systemem*/
/*TODO aktualizacje*/

public class RemoteCmdClient {

    public static final String VERSION = "1.0";
    public static final String LOCKFILE = "FILE.lock";
    public static final String ID_FILE = "UNIQUE_ID";

    public static  String staticRequestUrl = "http://example.deployed.pl/remote_cmd";
    public static  String websocketServiceUrl = "ws://ws.example.deployed.pl:80";

    public static final String staticRequestUrlDev = "http://mk.pl/remote_cmd";
    public static final String websocketServiceUrlDev = "ws://mk.pl:1025";

    public static String HOSTNAME = "UNKNOWN";
    public static String JRE_VERSION = System.getProperty("java.version");
    public static String ID = "";

    public static Lockfile lockfile = new Lockfile();

    public static void main(String arg[]){
        System.out.println("INIT");

        if(Arrays.asList(arg).contains("dev")){
            System.out.println("====== DEV MODE ======");
            staticRequestUrl = staticRequestUrlDev;
            websocketServiceUrl = websocketServiceUrlDev;
        }

        boolean runSuspected = false;

        try {

            if(!lockfile.exists()){
                System.out.println("Lockfile n/e, creating");
                lockfile.create();
            }else{
                System.out.println("Lockfile exists, checking server activity");
                runSuspected = true;
            }

            if(!loadID()){
                System.exit(0);
            }

            System.out.println("ID: "+ID);

            java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();

            HOSTNAME = localMachine.getHostName();
            System.out.println("Hostname: " + HOSTNAME);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Activity query utl: "+RemoteCmdClient.staticRequestUrl);

        try {
            if(runSuspected){
                String response = Http.get(RemoteCmdClient.staticRequestUrl+"/hostapi/isrunning",
                        new UrlParam("id", ID)
                );
                JSON json = new JSON(response);
                if(json.getBoolean("result.running")){
                    System.out.println("Server confirms already running instance -> exiting");
                    System.exit(0);
                }else{
                    System.out.println("Server fails to find running instance -> running");
                }
            }

            System.out.println("===== RUNNING =====");

            boolean skipSleep = false;
            while(true){

                String response = Http.get(RemoteCmdClient.staticRequestUrl+"/hostapi/queryactivity",
                        new UrlParam("hostname", HOSTNAME),
                        new UrlParam("id", ID)
                );
                try{
                    JSON json = new JSON(response);

                    if((boolean)json.get("result.body")){
                        WebsocketsHandler handler = new WebsocketsHandler();
                        while (handler.isActive()){Thread.sleep(100);}
                        skipSleep = true;
                    }
                }catch (Exception e){
                    System.out.println("JSON EXCEPTION");
                    System.out.println(e.getMessage());
                    System.out.println(response);
                }


                if(!skipSleep){
                    Thread.sleep(5000);
                }else{
                    skipSleep = false;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean loadID(){
        try{
            File idFile = new File(ID_FILE);
            if(!idFile.exists()){
                String uniqueId = UUID.randomUUID().toString().replace("-", "");
                if(idFile.createNewFile()){
                    PrintWriter writer = new PrintWriter(idFile.getAbsolutePath(), "UTF-8");
                    writer.println(uniqueId);
                    writer.close();
                }else{
                    System.out.println("CANNOT CREATE ID FILE");
                }
                ID = uniqueId;
            }else{
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(new FileInputStream(idFile))
                );
                ID = br.readLine();
            }
            return true;
        }catch (Exception e){
            System.out.println("Exception when loading ID");
            System.out.println(e.getMessage());
        }
        return false;

    }
}
