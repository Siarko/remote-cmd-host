package pl.siarko.websocket.RemoteConsole.internalCommands.download;

import java.io.*;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipFile {

    private ZipOutputStream zos;
    private FileOutputStream fos;
    private String name;

    public ZipFile(String name){
        try {
            this.name = name+".zip";
            fos = new FileOutputStream(this.name);
            zos = new ZipOutputStream(fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public String getName(){
        return this.name;
    }

    public void addFile(File f, File refDir){
        this.addEntry(f, refDir);
    }

    public void addFiles(ArrayList<File> fileList, File refDir) {
        for (File f : fileList) {
            this.addEntry(f, refDir);
        }
    }

    public boolean save(){
        try {
            zos.finish();
            fos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;

    }

    private void addEntry(File f, File refDir){
        String entryName = f.getAbsolutePath().substring(refDir.getAbsolutePath().length()+1);

        ZipEntry entry = new ZipEntry(entryName);
        try {
            zos.putNextEntry(entry);

            FileInputStream fis = new FileInputStream(f);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
