package pl.siarko.http;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.util.Scanner;

public class PostRequest {

    private CloseableHttpClient client;
    private HttpPost postRequest;
    MultipartEntityBuilder builder;


    public PostRequest(String url){
        client = HttpClients.createDefault();
        postRequest = new HttpPost(url);
        builder = MultipartEntityBuilder.create();
    }

    public void addFile(File f){
        if(!f.exists() || f.isDirectory()){ return;}
        FileBody fb = new FileBody(f);
        builder.addPart(f.getName(), fb);

    }

    public void addText(String name, String value){
        builder.addTextBody(name, value);
    }

    public String send(){
        postRequest.setEntity(builder.build());

        try (CloseableHttpResponse response = client.execute(postRequest)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                client.close();
                return "Unexpected status from server: "+response.getStatusLine();
            } else {
                HttpEntity resEntity = response.getEntity();
                String result = "No server response entity found";
                if (resEntity != null) {
                    Scanner s = new Scanner(resEntity.getContent());
                    result = "Server response: "+s.useDelimiter("\\A").next();
                }
                EntityUtils.consume(resEntity);
                client.close();

                return result;
            }
        }catch (Exception e){
            return "Exception in upload: "+e.getMessage();
        }

    }
}
