package pl.siarko.websocket.RemoteConsole.internalCommands;

import pl.siarko.RemoteCmdClient;
import pl.siarko.http.PostRequest;
import pl.siarko.json.JSON;
import pl.siarko.websocket.RemoteConsole.Directory;
import pl.siarko.websocket.RemoteConsole.IInternalCommand;
import pl.siarko.websocket.RemoteConsole.InternalCommands;
import pl.siarko.websocket.RemoteConsole.internalCommands.download.ZipFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.UUID;

public class Download implements IInternalCommand {

    @Override
    public String run(ArrayList<String> params, Directory workingDirectory) {
        if(params.size() < 2){
            return "Usage: download path [-nr non recursive]";
        }
        if(workingDirectory.get() == null){
            return "CANNOT DOWNLOAD FROM ROOT DIRECTORY";
        }
        File f = workingDirectory.get().resolve(params.get(1)).toFile();
        if(f.exists()){
            String outputFile;
            if(f.isFile()){
                outputFile = this.zipFile(f);
            }else{
                boolean recursive = true;
                if(params.size() == 3 && params.get(2).equals("-nr")){
                    recursive = false;
                }
                outputFile = zipDir(f, recursive);
            }

            if(outputFile != null){
                File of = new File(outputFile);

                JSON fileInfo = new JSON();
                fileInfo.put("path", f.getAbsolutePath(), false);

                String result = "[UPLOAD] "+upload(of, fileInfo.toString());
                try {
                    Files.delete(of.toPath());
                } catch (IOException e) {
                    InternalCommands.sendChars("Cannot delete zip file\n"+e.getMessage()+"\n");
                }
                return result;
            }
        }
        return "FILE NOT EXISTS";
    }

    private String upload(File file, String description){
        PostRequest pr = new PostRequest(RemoteCmdClient.staticRequestUrl+"/hostapi/upload");
        pr.addFile(file);
        pr.addText("ID", RemoteCmdClient.ID);
        pr.addText(file.getName()+"_DESC", description);
        return pr.send();
    }

    private String zipFile(File f){
        InternalCommands.sendChars("[ZIP] Zipping...\n");
        File parent = f.getParentFile();
        ZipFile zipFile = new ZipFile(UUID.randomUUID().toString());
        zipFile.addFile(f, parent);
        zipFile.save();
        InternalCommands.sendChars("[ZIP] Zip file created: "+zipFile.getName()+"\n");
        return zipFile.getName();
    }

    private String zipDir(File f, boolean recursive){
        String r = "";
        r += "[ZIP] Compressing directory contents "+(recursive?"recursive ":"non-recursive ");
        r += f.getAbsolutePath();
        InternalCommands.sendChars(r+"\n");

        InternalCommands.sendChars("[ZIP] Scanning directory...\n");
        ArrayList<File> fileList = this.scanDirectory(f, recursive);
        InternalCommands.sendChars("[ZIP] File count: "+fileList.size()+"\n[ZIP] Zipping...\n");

        ZipFile zipFile = new ZipFile(UUID.randomUUID().toString());
        zipFile.addFiles(fileList,f);
        zipFile.save();
        InternalCommands.sendChars("[ZIP] Zip file created: "+zipFile.getName()+"\n");

        return zipFile.getName();
    }

    private ArrayList<File> scanDirectory(File dir, boolean recursive){
        ArrayList<File> result = new ArrayList<>();
        File[] files = (recursive)?dir.listFiles():dir.listFiles(File::isFile);
        if(files == null){ return result; }
        for (File f : files) {
            if(f.isDirectory()){
                result.addAll(this.scanDirectory(f, recursive));
            }else{
                result.add(f);
            }
        }
        return result;
    }
}
