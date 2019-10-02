package pl.siarko.http;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.util.EntityUtils;
import pl.siarko.RemoteCmdClient;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import java.util.Scanner;

public class Http {

    public static String get(String url, UrlParam ...params) throws Exception {
        StringBuilder paramsString = new StringBuilder("?");
        int i = 0;
        for (UrlParam p :params) {
            paramsString.append(p.toString());
            if(i < params.length-1){
                paramsString.append("&");
            }
            i++;
        }

        url += paramsString;

        URLConnection connection = (new URL(url)).openConnection();

        connection.setRequestProperty("Accept-Charset", "UTF-8");
        InputStream response = connection.getInputStream();

        Scanner scanner = new Scanner(response);
        return scanner.useDelimiter("\\A").next();
    }

    public static String uploadFile(String url, File f){
        return uploadFile(url, f, null);
    }

    public static String uploadFile(String url, File f, String description){
        if(!f.exists()){ return "Cannot find file to upload: "+f.getAbsolutePath(); }

        try{
            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpPost httppost = new HttpPost(url);

            FileBody fb = new FileBody(f);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                    .addTextBody("ID", RemoteCmdClient.ID)
                    .addPart("FILE_"+f.getName(), fb);
            if(description != null){
                builder.addTextBody("description", description);
            }

            HttpEntity reqEntity = builder.build();


            httppost.setEntity(reqEntity);


            try (CloseableHttpResponse response = httpclient.execute(httppost)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    httpclient.close();
                    return "Unexpected status from server: "+response.getStatusLine();
                } else {
                    HttpEntity resEntity = response.getEntity();
                    String result = "No server response entity found";
                    if (resEntity != null) {
                        Scanner s = new Scanner(resEntity.getContent());
                        result = "Server response: "+s.useDelimiter("\\A").next();
                    }
                    EntityUtils.consume(resEntity);
                    httpclient.close();

                    return result;
                }
            }

        }catch (Exception e){
            return "Exception in upload: "+e.getMessage();
        }

    }
}
