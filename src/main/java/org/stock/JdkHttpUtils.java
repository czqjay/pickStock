package org.stock;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class JdkHttpUtils {


    /**
     * 获取 POST请求
     *
     * @return 请求结果
     */
    private static HttpURLConnection byPost(String url, Integer connectTimeout,
                               Integer readTimeout, String contentType, Map<String, String> heads,
                               Map<String, String> params) throws IOException {

        URL u;
        HttpURLConnection connection = null;
        OutputStream out;
        InputStream is = null;
        try {
            u = new URL(url);
            connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setRequestProperty("Content-Type", contentType);
            // POST请求必须设置该属性
            connection.setDoOutput(true);
            connection.setDoInput(true);
            if (heads != null) {
                for (Map.Entry<String, String> stringStringEntry : heads.entrySet()) {
                    connection.setRequestProperty(stringStringEntry.getKey(),
                            stringStringEntry.getValue());
                }
            }

            out = connection.getOutputStream();
            if (params != null && !params.isEmpty()) {
                out.write(toJSONString(params).getBytes());
            }
            out.flush();
            out.close();

            // 获取请求返回的数据流
//            is = connection.getInputStream();
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            // 封装输入流is，并指定字符集
//            int i;
//            while ((i = is.read()) != -1) {
//                baos.write(i);
//            }
//            return baos.toString("UTF-8");
            return connection;
        } catch (Exception e) {
            System.out.println("e = " + e.getMessage());

        }
        return connection;
    }


    public static String getPost(String url, Integer connectTimeout,
                                 Integer readTimeout, String contentType, Map<String, String> heads,
                                 Map<String, String> params) throws IOException {

        InputStream is = byPost(url, connectTimeout, readTimeout, contentType, heads, params).getInputStream();;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // 封装输入流is，并指定字符集
            int i;
            while ((i = is.read()) != -1) {
                baos.write(i);
            }
            return baos.toString("UTF-8");

    }


    public static String getGzip(String url, Integer connectTimeout,
                                     Integer readTimeout, String contentType, Map<String, String> heads,
                                     Map<String, String> params, String method) throws Exception {
        System.out.println("url = " + url);
        if(method.equals("POST")){
            return  getGzipPost(url, connectTimeout, readTimeout, contentType, heads, params);
        }
        return   getGzipGet(url, connectTimeout, readTimeout, contentType, heads, params);
    }


    public static String getGzipPost(String url, Integer connectTimeout,
                                     Integer readTimeout, String contentType, Map<String, String> heads,
                                     Map<String, String> params) throws Exception {
        HttpURLConnection con = byPost(url, connectTimeout, readTimeout, contentType, heads, params);

        Map<String, List<String>> map = con.getHeaderFields();

        InputStream in = con.getInputStream();
        final GZIPInputStream gzin = new GZIPInputStream(in);
        final InputStreamReader isr = new InputStreamReader(gzin,"UTF-8");
        String responseText = getStringFromStream(isr);

        return responseText;
    }
    public static String getStringFromStream(final InputStreamReader isr) throws Exception {
        final BufferedReader br = new BufferedReader(isr);
        final StringBuilder sb = new StringBuilder();
        String tmp;
        while ((tmp = br.readLine()) != null) {
            sb.append(tmp);
            sb.append("\r\n");
        }
        br.close();
        isr.close();
        return sb.toString();

    }


    public static String getGzipGet(String url, Integer connectTimeout,
                                     Integer readTimeout, String contentType, Map<String, String> heads,
                                     Map<String, String> params) throws Exception {
        HttpURLConnection con = byGet(url, connectTimeout, readTimeout, contentType, heads, params);

        InputStream in = con.getInputStream();
        final GZIPInputStream gzin = new GZIPInputStream(in);
        final InputStreamReader isr = new InputStreamReader(gzin,"UTF-8");
        String responseText = getStringFromStream(isr);

        return responseText;
    }


    public static HttpURLConnection byGet(String url, Integer connectTimeout,
                                Integer readTimeout, String contentType, Map<String, String> heads,
                                Map<String, String> params) throws IOException, java.io.IOException {

        // 拼接请求参数
        if (params != null && !params.isEmpty()) {
            url += "?";
            if (params != null && !params.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> stringObjectEntry : params.entrySet()) {
                    try {
                        sb.append(stringObjectEntry.getKey()).append("=").append(
                                        URLEncoder.encode(stringObjectEntry.getValue(), "UTF-8"))
                                .append("&");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                sb.delete(sb.length() - 1, sb.length());
                url += sb.toString();
            }
        }

        URL u;
        HttpURLConnection connection;
        u = new URL(url);
        connection = (HttpURLConnection) u.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        connection.setRequestProperty("Content-Type", contentType);
        if (heads != null) {
            for (Map.Entry<String, String> stringStringEntry : heads.entrySet()) {
                connection.setRequestProperty(stringStringEntry.getKey(),
                        stringStringEntry.getValue());
            }
        }

        // 获取请求返回的数据流
//        InputStream is = connection.getInputStream();
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        // 封装输入流is，并指定字符集
//        int i;
//        while ((i = is.read()) != -1) {
//            baos.write(i);
//        }
//        return baos.toString();

        return connection;
    }


    /**
     * 获取 POST请求
     *
     * @return 请求结果
     */
    public static String getGet(String url, Integer connectTimeout,
                                Integer readTimeout, String contentType, Map<String, String> heads,
                                Map<String, String> params) throws IOException, java.io.IOException {

        System.out.println("url = " + url);
        HttpURLConnection connection = byGet(url, connectTimeout, readTimeout, contentType, heads, params);
        // 获取请求返回的数据流
        InputStream is = connection.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 封装输入流is，并指定字符集
        int i;
        while ((i = is.read()) != -1) {
            baos.write(i);
        }
        return baos.toString();

    }


    /**
     * Map转Json字符串
     */
    public static String toJSONString(Map<String, String> map) {
        Iterator<Map.Entry<String, String>> i = map.entrySet().iterator();
        if (!i.hasNext()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (; ; ) {
            Map.Entry<String, String> e = i.next();
            String key = e.getKey();
            String value = e.getValue();
            sb.append("\"");
            sb.append(key);
            sb.append("\"");
            sb.append(':');
            sb.append("\"");
            sb.append(value);
            sb.append("\"");
            if (!i.hasNext()) {
                return sb.append('}').toString();
            }
            sb.append(',').append(' ');
        }
    }


}
