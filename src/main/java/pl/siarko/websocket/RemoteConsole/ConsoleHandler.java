package pl.siarko.websocket.RemoteConsole;

import com.neovisionaries.ws.client.WebSocket;
import pl.siarko.json.JSON;
import sun.misc.Signal;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class ConsoleHandler {

    private WebSocket ws;
    private BufferedWriter processBuffer = null;
    private IConsoleOutBuff outputHandler = null;
    private Process p = null;

    private Directory workingDirectory = new Directory(Paths.get(System.getProperty("user.dir")));

    private boolean autocompleteRunning = false;
    private IConsoleAutocompleted autocompleteHandler = null;

    private String prompt = "$> ";

    public void setWsObject(WebSocket w){
        this.ws = w;
    }

    public void onAutocompleteDone(IConsoleAutocompleted handler){
        this.autocompleteHandler = handler;
    }

    private void sendAutocomplete(JSON data){
        if(this.autocompleteHandler != null){
            this.autocompleteHandler.handle(ws, data);
        }
    }

    public void autocompleteRequest(String partial){

        if(!autocompleteRunning){
            (new Thread(() -> {
                autocompleteRunning = true;
                try{
                    JSON output = new JSON();
                    if(processBuffer != null){
                        output.put("succeed", false);
                        output.put("reason", "Proces aktywny");
                    }else{
                        char last = partial.charAt(partial.length()-1);
                        if(last == ' '){
                            System.out.println("Full path autocomplete");

                            File d = workingDirectory.get().toFile();
                            if(d.isDirectory()){
                                for(final File f: Objects.requireNonNull(d.listFiles())){
                                    JSON fd = new JSON();
                                    fd.put("type", (f.isDirectory()?"DIR":"FILE"));
                                    fd.put("name", f.getName());
                                    output.put("result.#", fd.rawObject());
                                }
                            }

                        }else{
                            System.out.println("Partial path autocomplete");

                        }
                        output.put("succeed", true);

                    }

                    sendAutocomplete(output);
                }catch (Exception e){
                    System.out.println("Exception while resolving autocomplete: "+e.getMessage());
                }
                autocompleteRunning = false;

            })).start();
        }

    }

    public void input(String line, boolean isSignal){
        if(processBuffer == null){ //new command -> no subprocess
            if(line.length() == 0){
                this.sendOut(prompt, false);
                return;
            }
            ArrayList<String> parts = InternalCommands.splitCommand(line);
            if(InternalCommands.isInternalCommand(parts.get(0))){
                String out = InternalCommands.runInternalCommand(this, parts, workingDirectory);
                this.sendOut(out+"\n"+prompt, false);
                return;
            }
            if(isSignal){
                this.sendOut(line+" (NO PROCESS RUNNING)\n"+prompt, false);
                return;
            }
            System.out.println("Exec -> "+line);
            this.spawnProcess(line);
        }else{
            if(isSignal){
                if(line.equals("SIGKILL")){
                    System.out.println("SIGNAL -> "+line);
                    this.sendOut(line+"\n"+prompt, false);
                    p.destroy();
                }
            }else{
                System.out.println("STDIN -> "+line);
                try {
                    if(line.length() > 0){
                        processBuffer.write(line);
                    }
                    processBuffer.newLine();
                    processBuffer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void spawnProcess(String command){
        try {
            File dir;
            if(workingDirectory.get() == null) {
                sendOut("[WARN] Current working directory is invalid, running in CMD's initial\n", false);
                dir = null;
            }else{
                dir = workingDirectory.get().toFile();
            }
            String[] env = new String[0];
            p = Runtime.getRuntime().exec(command, env, dir);

            this.processBuffer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));

            BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            spawnReaderListener(stdout, false);
            spawnReaderListener(stderr, true);


        } catch (IOException e) {
            System.out.println("Error while spawning command: "+e.getMessage());
            sendOut(e.getMessage()+"\n"+prompt, false);
        }
    }

    private void spawnReaderListener(BufferedReader reader, boolean isErrorStream){
        new Thread(() -> {
            int c;
            boolean ready = false;
            StringBuilder bufferedOutput = new StringBuilder();
            while(true){
                try {
                    if(ready && !reader.ready()){ //buffer stopped being ready
                        ready = false;
                        sendOut(bufferedOutput.toString(), isErrorStream);
                        bufferedOutput = new StringBuilder();
                    }
                    if((c = reader.read()) == -1){
                        synchronized (ConsoleHandler.class){
                            if(processBuffer != null){
                                processBuffer = null;
                                sendOut("[JAVA SHELL] Process terminated\n"+prompt, false);
                            }
                        }
                        break;
                    } //end of stream
                    ready = true;
                    bufferedOutput.append((char)c);

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    void sendOut(String s, boolean isErr){
        if(this.outputHandler != null){
            s = s.replaceAll("\r", "");
            this.outputHandler.handle(ws, s, isErr);
        }
    }


    public void onOut(IConsoleOutBuff handler){
        this.outputHandler = handler;
    }
}
