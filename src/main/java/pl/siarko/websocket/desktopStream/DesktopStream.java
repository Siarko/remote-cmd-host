package pl.siarko.websocket.desktopStream;

import com.neovisionaries.ws.client.WebSocket;
import pl.siarko.Profiler.Profiler;
import pl.siarko.json.JSON;
import pl.siarko.websocket.WebsocketConnection;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class DesktopStream {
    private boolean active = false;
    private int fps;
    private int fpc;
    private float compresion;
    private WebSocket webSocket;
    private Robot robot;
    private Rectangle screenRect;
    private Dimension frameSize;

    private boolean captureFinished = true;

    private List<BufferedImage> screenBuffer = Collections.synchronizedList(new ArrayList<BufferedImage>());

    public DesktopStream(){
        Profiler.setShowPartial(false);
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.screenRect = new Rectangle((int)screenSize.getWidth(), (int)screenSize.getHeight());
    }

    public boolean isActive(){
        return this.active;
    }

    public void start(WebSocket ws, int fps, int fpc, int w, int h, float compression) {

        this.frameSize = new Dimension(w,h);
        this.fps = fps;
        this.fpc = fpc;
        this.compresion = compression;
        this.webSocket = ws;

        if(this.fpc < 5){
            System.out.println("[ERROR] Received incorrect fpc value, backing up to default");
            this.fpc = 20;
        }

        System.out.println("Starting video stream at Q: "+compression+" "+fps+" FPS, "+fpc+" FPC, res: "+frameSize.width+
                "x"+frameSize.height);

        getScreenCapture().start();
        getBufferTransmitter().start();

    }

    public void stop(){
        this.active = false;
    }

    private Thread getScreenCapture(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                Profiler p = new Profiler();
                DesktopStream.this.active = true;
                float sleepTime = 1000f/DesktopStream.this.fps;
                DesktopStream.this.captureFinished = false;
                screenBuffer.clear();

                while(DesktopStream.this.active && DesktopStream.this.webSocket.isOpen()){
                    long startTime = System.currentTimeMillis();
                    Profiler p1 = new Profiler();
                    screenBuffer.add(robot.createScreenCapture(DesktopStream.this.screenRect));
                    p1.end("SINGLE FRAME CAPTURE");

                    try {
                        int elapsed = (int) (System.currentTimeMillis()-startTime);
                        int sleep = (int) (sleepTime-elapsed);
                        if(sleep > 0){
                            Thread.sleep(sleep);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                p.end("FRAME CAPTURE");

                DesktopStream.this.captureFinished = true;
            }
        });
    }

    private Thread getBufferTransmitter(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();

                JPEGImageWriteParam params = new JPEGImageWriteParam(null);
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(DesktopStream.this.compresion);

                ImageWriter iw = ImageIO.getImageWritersByFormatName("jpg").next();

                Profiler.resetSummary();
                while (!DesktopStream.this.captureFinished){
                    if(DesktopStream.this.screenBuffer.size() < DesktopStream.this.fpc){ continue;}
                    Profiler frameProfiler = new Profiler();
                    try {
                        JSON json = new JSON();
                        for(int i = 0; i < DesktopStream.this.fpc; i++){
                            json.put("frames.#", base64Image(
                                    iw, params,
                                    DesktopStream.this.screenBuffer.remove(0),
                                    frameSize.width, frameSize.height)
                            );
                        }
                        json.put("sendTime", System.currentTimeMillis()/1000l);
                        DesktopStream.this.webSocket.sendText(WebsocketConnection.formatMesage(
                                "stream_clip", json.rawObject()
                                ));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    frameProfiler.end("CLIP FRAMES");
                    bos.reset();
                }
                System.out.println("Buffer send end");
                Profiler.printSummary();
            }
        });
    }

    private String base64Image(ImageWriter iw, JPEGImageWriteParam p, BufferedImage i, int w, int h) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageOutputStream ios = new MemoryCacheImageOutputStream(bos);
        iw.setOutput(ios);
        BufferedImage i2 = resize(i ,w, h);
        iw.write(null, new IIOImage(i2, null, null), p);
        return Base64.getEncoder().encodeToString(bos.toByteArray());
    }

    private BufferedImage resize(BufferedImage img, int newW, int newH) {
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_FAST);
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }
}
