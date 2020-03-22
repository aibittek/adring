package com.example.ad.utils;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.*;
import okio.ByteString;

public class WebTTSWS {

    private static final String hostUrl = "http://tts-api.xfyun.cn/v2/tts";
    private static final String appid = "5e02d61f";
    private static final String apiSecret = "aeaa04a94d9dccce66de3e13becce8b2";
    private static final String apiKey = "8f4aed44e398cae9923fa76543011d51";
    private static String mVcn;
    private static ArrayList<byte[]> arrayList = new ArrayList<byte[]>();
    private static String mText;
    private static IResponseResult mResponseResult;
    public static void getTTSData(String text, String vcnString, int volume, int speed, @NonNull IResponseResult permissionsResult) throws Exception {
        mText = text;
        mVcn = vcnString;
        mResponseResult = permissionsResult;
        // 获取到服务端要求的URL请求
        String authUrl = getAuthUrl(hostUrl, apiKey, apiSecret);
        // 构造http客户端
        OkHttpClient client = new OkHttpClient.Builder().build();
        // 把schema中的http(s)替换成ws(s)
        String url = authUrl.toString().replace("http://", "ws://").replace("https://", "wss://");
        // 构造连接请求request
        Request request = new Request.Builder().url(url).build();
        // 发起websocket连接，异步接收结果
        WebSocket webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);
                arrayList.clear();
                // 调用到这个地方，说明服务端返回了101消息，类似http返回的200消息，这里可以发送请求了
                // 查看语音合成文档，组装消息
//                String format = "{\"common\":{\"app_id\":\"%s\"},\"business\":{\"vcn\":\"%s\",\"aue\":\"raw\",\"speed\":\"50\"},\"data\":{\"status\":2,\"encoding\":\"UTF8\",\"text\":\"%s\"}}";
                String format = "{\"common\":{\"app_id\":\"%s\"},\"business\":{\"aue\":\"raw\",\"tte\":\"UTF8\",\"ent\":\"intp65\",\"vcn\":\"%s\",\"volume\":%s,\"speed\":%s},\"data\":{\"status\":2,\"text\":\"%s\"}}";
                try {
                    String reqData = String.format(format, appid, mVcn, volume, speed, Base64.encodeToString(mText.getBytes("utf8"), Base64.NO_WRAP));
                    webSocket.send(reqData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
                System.out.println("receive=>" + text);
                // 当服务器有数据返回时，调用到这里，注意服务器的audio数据可能分多次返回，当status为2时代表时最后一块音频数据，websocket连接也可以关闭了。
                ResponseData resp = null;
                Gson json = new Gson();
                try {
                    resp = json.fromJson(text, ResponseData.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (resp.getData() != null) {
                    // 先把每次获取到的音频base64 decode后放入mArrayList
                    String result = resp.getData().audio;
                    byte[] audio = Base64.decode(result, Base64.NO_WRAP);
                    arrayList.add(audio);
                    // 看文档得知，当status为2时，为最后一块音频
                    if (resp.getData().status == 2) {
                        int length = 0;
                        for (int i =0; i<arrayList.size(); i++) {
                            length += arrayList.get(i).length;
                        }

                        // 通过以下代码，pcm就是服务端返回给客户端的合成语音了，调用播放接口播放
                        byte[] pcm = new byte[length];
                        int curLength = 0;
                        for (int i =0; i<arrayList.size(); i++) {
                            System.arraycopy(arrayList.get(i), 0, pcm, curLength, arrayList.get(i).length);
                            curLength += arrayList.get(i).length;
                        }
//                        AudioTrackManager.getInstance().startPlay(pcm);
                        mResponseResult.setAudio(pcm);
                        // 数据接收完毕，关闭连接，释放资源
                        webSocket.close(1000, "");
                    }
                }
            }
            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                super.onMessage(webSocket, bytes);
            }
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                super.onClosing(webSocket, code, reason);
                System.out.println("socket closing");
            }
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                super.onClosed(webSocket, code, reason);
                System.out.println("socket closed");
            }
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
                System.out.println("connection failed");
            }
        });
    }

    /**
     * 语音合成URL拼接，满足调用需求
     * @param hostUrl(string) host的http地址
     * @param apiKey(string) 应用的apikey
     * @param apiSecret(string) 应用的apiSecret
     * @return 服务端要求的URL
     */
    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        // 设置时间格式，满足date的要求
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        // 设置时间为GMT时间
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        // 基于以上的配置信息获取到当前的满足格式的GMT时间
        String date = format.format(new Date());

        // 拼接signature_origin
        StringBuilder builder = new StringBuilder("host: ").append(url.getHost()).append("\n").//
                append("date: ").append(date).append("\n").//
                append("GET ").append(url.getPath()).append(" HTTP/1.1");

        // 拼接signature_sha
        Charset charset = Charset.forName("UTF-8");
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
        // 拼接signature
        String sha = Base64.encodeToString(hexDigits, Base64.NO_WRAP);
        // 拼接authorization
        String authorization = String.format("hmac username=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
        // 拼接URL
        HttpUrl httpUrl = HttpUrl.parse("https://" + url.getHost() + url.getPath()).newBuilder().//
                addQueryParameter("authorization", Base64.encodeToString(authorization.getBytes(), Base64.NO_WRAP)).//
                addQueryParameter("date", date).//
                addQueryParameter("host", url.getHost()).//
                build();
        return httpUrl.toString();
    }

    public static class ResponseData {
        private int code;
        private String message;
        private String sid;
        private Data data;
        public int getCode() {
            return code;
        }
        public String getMessage() {
            return this.message;
        }
        public String getSid() {
            return sid;
        }
        public Data getData() {
            return data;
        }
    }
    public static class Data {
        private int status;  //标志音频是否返回结束  status=1，表示后续还有音频返回，status=2表示所有的音频已经返回
        private String audio;  //返回的音频，base64 编码
        private String ced;  // 合成进度
    }
    public interface IResponseResult {
        void setAudio(byte[] audio);
    }
}
