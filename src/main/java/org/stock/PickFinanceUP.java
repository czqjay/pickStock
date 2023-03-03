package org.stock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;


@RestController
public class PickFinanceUP {

    public List stockList = new LinkedList();

    String mcode = "MTY3NzY4MTI3Ng==";
    public String headersOfFinance = "";

    public String headersOfDayli = "";


    public String urlOfFinance = "http://webapi.cninfo.com.cn/api/sysapi/p_sysapi1138?scode=";

    public String urlOfDayli = "http://webapi.cninfo.com.cn/api-data/cube/dailyLine?stockCode={stockID}&_={timeStamp}";

    public void refershMcode() {
        this.headersOfFinance = "Accept: application/json, text/javascript, */*; q=0.01\n" +
                "Accept-Encoding: gzip, deflate\n" +
                "Accept-Language: zh-CN,zh;q=0.9\n" +
                "Connection: keep-alive\n" +
                "Content-Length: 0\n" +
                "Cookie: Hm_lvt_489bd07e99fbfc5f12cbb4145adb0a9b=1677640171; Hm_lpvt_489bd07e99fbfc5f12cbb4145adb0a9b=1677688757; JSESSIONID=AFAE1345260BA45F569B7546B2B30BE1\n" +
                "Host: webapi.cninfo.com.cn\n" +
                "mcode: " + this.mcode + "\n" +
                "Origin: http://webapi.cninfo.com.cn\n" +
                "Referer: http://webapi.cninfo.com.cn/\n" +
                "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36\n" +
                "X-Requested-With: XMLHttpRequest";


        this.headersOfDayli = "Accept: application/json, text/javascript, */*; q=0.01\n" +
                "Accept-Encoding: gzip, deflate\n" +
                "Accept-Language: zh-CN,zh;q=0.9\n" +
                "Connection: keep-alive\n" +
                "Cookie: Hm_lvt_489bd07e99fbfc5f12cbb4145adb0a9b=1677640171,1677719495; Hm_lpvt_489bd07e99fbfc5f12cbb4145adb0a9b=1677737993; JSESSIONID=EBB859ACB30EE89C85173AF0E0AC6A5D\n" +
                "Host: webapi.cninfo.com.cn\n" +
                "Referer: http://webapi.cninfo.com.cn/\n" +
                "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36\n" +
                "X-Requested-With: XMLHttpRequest";

    }

    public PickFinanceUP() {


        InputStream in = this.getClass().getResourceAsStream("/all_stock.txt");
        BufferedReader br = new BufferedReader(new java.io.InputStreamReader(in));
        while (true) {
            try {
                if (br.ready()) {
                    stockList.add(br.readLine());
                } else {
                    break;
                }
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }


    }


    @GetMapping("test")
    public void getUpList() {

        System.out.println("stockList.get(0) = " + stockList.get(0));


    }

    @GetMapping("analysisData")
    public void analysisData() throws Exception {

//        this.parseByTostring(this.getRemoteData());


        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = sdf.format(new Date());
        String filestr = "E:\\ideaWorkSpace\\pickStock\\src\\main\\resources\\" + dateStr + ".txt";


        String stockID = "";
        String stockIDOfPramising;
        for (int i = 0; i < stockList.size(); i++) {

            String mill = String.valueOf(new Date().getTime());
            this.mcode = this.generateMcode(mill.substring(0, mill.length() - 3));
            refershMcode();
            stockID = (String) stockList.get(i);
            String url = this.urlOfFinance + stockID;

//            if (calcPramising(this.getRemoteDataByUtil(this.headersOfFinance, url, "POST"))) {


            String result = JdkHttpUtils
                    .getGzip(url, 60000, 60000, "application/json",
                            this.getHeaderAsMap(this.headersOfFinance), null, "POST");
            if (calcPramising(result)) {

                url = this.urlOfDayli;
                url = url.replace("{stockID}", stockID);
                url = url.replace("{timeStamp}", mill.substring(0, mill.length() - 3));
                Map m = this.getHeaderAsMap(this.headersOfDayli);

                if (calcPramisingByStockLoan(result))
                    if (calcPramisingByPrice(JdkHttpUtils
                            .getGet(url, 60000, 60000, "application/json", m, null))) {

                        RandomAccessFile ra = new RandomAccessFile(filestr, "rws");
                        ra.seek(ra.length());
                        ra.write("\r\n".getBytes());
                        ra.write(stockID.getBytes());
                        ra.close();
                    }
            }

            Random r = new Random();
            Thread.sleep(r.nextInt(5) * 1000);
        }
    }


    public String getRemoteDataByUtil(String headers, String url, String method) throws Exception {
//        @limit=10

        System.out.println("url = " + url);
        String result = "";
        Map m = this.getHeaderAsMap(headers);
        try {
            PickFinanceUP that = this;
            result = JdkHttpUtils
                    .getGzip(url, 60000, 60000, "application/json", m, null, method);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("result = " + result);

        return result;
    }


    // 收盘价不超过历史最低价格的 130%
    public boolean calcPramisingByPrice(String result) throws IOException {
        ObjectMapper om = new ObjectMapper();
        Map map = om.readValue(result, Map.class);

        List<ArrayList> list = (List) map.get("line");

        float minClose = 0;
        float close = 0;
        for (int i = 0; i < list.size(); i++) {
            close = Float.valueOf(list.get(i).get(2).toString());
            minClose = minClose == 0 ? close : minClose > close ? close : minClose;
        }
        System.out.print(close * 0.7);
        System.out.print("  <  ");
        System.out.println(minClose);
        return close * 0.7 < minClose;

    }


    // 当日融券余额  * 2  < 昨日融券余量
    public boolean calcPramisingByStockLoan(String result) throws IOException {
        String stockID = null;
        List<Double> financeBuket = new ArrayList<Double>();

        ObjectMapper om = new ObjectMapper();
        Map map = om.readValue(result, Map.class);

        List re = (List) map.get("records");
        Map re2;


        for (int i = 0; i < 2; i++) {

            re2 = (Map) re.get(i);
            stockID = (String) re2.get("SECCODE");
            Double dailyStockLoan = (Double) re2.get("F008N");
            financeBuket.add(dailyStockLoan);
        }

        Double currentStockLaon = financeBuket.get(0);
        Double yestodayStockLaon = financeBuket.get(1);

        return currentStockLaon * 2 < yestodayStockLaon;


    }

    //融资买入策略
    public boolean calcPramising(String result) throws IOException {

        String stockID = null;
        List<Double> financeBuyBuket = new ArrayList<Double>();

        ObjectMapper om = new ObjectMapper();
        Map map = om.readValue(result, Map.class);

        List re = (List) map.get("records");
        Map re2;


        for (int i = 0; i < 5; i++) {

            re2 = (Map) re.get(i);
            stockID = (String) re2.get("SECCODE");
            Double dailyBuyFinance = (Double) re2.get("F002N");
            financeBuyBuket.add(dailyBuyFinance);
        }

        if (isPromising2(financeBuyBuket)) {
            return true;
        }
        return false;
    }

    // 当日融资买入 大于前4天的平均值*  threshold（ =2）
    public static boolean isPromising(List<Double> financeBuyBuket) {
        int threshold = 2;
        Double today = financeBuyBuket.get(0);
        Double avgAllofAgo = 0D;
        for (int i = 0; i < financeBuyBuket.size() - 1; i++) {
            avgAllofAgo = avgAllofAgo + financeBuyBuket.get(i);
        }
        avgAllofAgo = avgAllofAgo / (financeBuyBuket.size() - 1);
        System.out.println("today = " + today);
        System.out.println("avgAllofAgo = " + avgAllofAgo);
        System.out.println(today > avgAllofAgo * threshold);
        return today > avgAllofAgo * threshold;

    }


    // dailyFinanceBuy > yestoday
    public static boolean isPromising2(List<Double> financeBuyBuket) {
        int threshold = 2;
        Double today = financeBuyBuket.get(0);
        Double yestoday = financeBuyBuket.get(1);

        System.out.println("today = " + today);
        System.out.println("yestoday = " + yestoday);
        System.out.println(today > yestoday);
        return today > yestoday;

    }


    public HttpResponse getRemoteData() {


        try {

//            HttpHost proxy = new HttpHost("127.0.0.1",8080);
            HttpClient client = new DefaultHttpClient();//定义client对象

//            client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);


            HttpPost method = new HttpPost("http://webapi.cninfo.com.cn/api/sysapi/p_sysapi1138?scode=000001");//


//            HttpGet method = new HttpGet("http://webapi.cninfo.com.cn/#/company?companyid=000001");
            Header[] hs = new Header[this.getHeader().size()];
            this.getHeader().toArray(hs);
            method.setHeaders(hs);
            HttpResponse res = client.execute(method);
            int statusCode = res.getStatusLine().getStatusCode();//状态，一般200为OK状态，其他情况会抛出如404,500,403等错误
            if (statusCode != HttpStatus.SC_OK) {
                System.out.println("远程访问失败。");
            }


            return res;

        } catch (Exception e) {
            System.err.println(e);

        }

        return null;
    }


    public void parseByTostring(HttpResponse res) throws IOException {

        System.out.println(EntityUtils.toString(res.getEntity(), "utf-8"));//输出反馈结果//
        EntityUtils.consume(res.getEntity());

    }


    public void parseByGzip(HttpResponse res) throws Exception {
        getStringFromResponseUzip(res);
        EntityUtils.consume(res.getEntity());
    }


    public List<Header> getHeader() {

        List<Header> list = new ArrayList<Header>();

        String[] headers = headersOfFinance.split("\n");

        int i = 0;
        while (i < headers.length) {
            String str = headers[i++];
            String[] arr = str.split(":");
            if (arr.length > 2) {
                arr[0] = str.substring(0, str.indexOf(":"));
                arr[1] = str.substring(str.indexOf(":") + 1, str.length());
            }
            Header header = new BasicHeader(arr[0], arr[1]);
            list.add(header);
        }
        return list;
    }

    public Map<String, String> getHeaderAsMap(String Headers) {

        Map map = new HashMap();

        String[] headers = Headers.split("\n");

        int i = 0;
        while (i < headers.length) {
            String str = headers[i++];
            String[] arr = str.split(":");
            if (arr.length > 2) {
                arr[0] = str.substring(0, str.indexOf(":"));
                arr[1] = str.substring(str.indexOf(":") + 1, str.length());
            }
            map.put(arr[0], arr[1]);

        }
        return map;
    }


    public void getByJsoup() {

        Document doc = Jsoup.parse("https://www.baidu.com/");
//获取页面下id="content"的标签
        Element content = doc.getElementById("wrapper");
//获取页面下的a标签
        Elements links = content.getElementsByTag("a");
        for (Element link : links) {
            //获取a标签下的href的属性值
            String linkHref = link.attr("href");
            //获取a标签下的文本内容
            System.out.println("linkHref = " + linkHref);
        }


    }


    public static String getStringFromResponseUzip(final HttpResponse response) throws Exception {
        if (response == null) {
            return null;
        }
        String responseText = "";
        //InputStream in = response.getEntity().getContent();
        final InputStream in = response.getEntity().getContent();
        final Header[] headers = response.getHeaders("Content-Encoding");
        for (final Header h : headers) {
            System.out.println(h.getValue());
            if (h.getValue().indexOf("gzip") > -1) {
                //For GZip response
                try {
                    final GZIPInputStream gzin = new GZIPInputStream(in);
                    final InputStreamReader isr = new InputStreamReader(gzin, "UTF-8");
                    responseText = getStringFromStream(isr);
                    //responseText = URLDecoder.decode(responseText, "utf-8");
                } catch (final IOException exception) {
                    exception.printStackTrace();
                }
                return responseText;
            }
        }
        responseText = EntityUtils.toString(response.getEntity(), "utf-8");
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

    public static void main(String[] args) throws Exception {

        PickFinanceUP p = new PickFinanceUP();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = sdf.format(new Date());


        String mill = String.valueOf(new Date().getTime());
        p.mcode = p.generateMcode(mill.substring(0, mill.length() - 3));
        p.refershMcode();

//        String url = p.urlOfDayli;
//        url = url.replace("{stockID}", "300020");
//        url = url.replace("{timeStamp}", mill.substring(0, mill.length() - 3));


//        Map m = p.getHeaderAsMap(p.headersOfDayli);
//        String result = JdkHttpUtils
//                .getGet(url, 60000, 60000, "application/json", m, null);
//
//        ObjectMapper om = new ObjectMapper();
//        Map map = om.readValue(result, Map.class);
//
//        List<ArrayList> list = (List)map.get("line");
//
//        float  minClose = 0;
//        float  close = 0;
//        for(int i = 0 ; i< list.size();i++){
//            close = Float.valueOf(list.get(i).get(2).toString());
//            minClose = minClose ==0 ? close: minClose > close? close: minClose ;
//            System.out.println("list.get(i).get(2) = " + i +" - "+  list.get(i).get(2));
//        }
//        System.out.println("minClose = " + minClose);

//        float minClose = 5.76F;
//        float close = 7.8F;
//
//        System.out.println(close - minClose);
//        System.out.println(close * 0.7);
//        System.out.println(close * 0.7 < minClose);

//        System.out.println("result = " + result);


        String url = p.urlOfFinance + "000001";
        String result = p.getRemoteDataByUtil(p.headersOfFinance, url, "POST");


        System.out.println("result = " + result);
    }

    public String generateMcode(String input) {

        String keyStr = "ABCDEFGHIJKLMNOP" + "QRSTUVWXYZabcdef" + "ghijklmnopqrstuv" + "wxyz0123456789+/" + "=";
        String output = "";
        int chr1, chr2, chr3;
        int enc1, enc2, enc3, enc4;
        int i = 0;
        do {


            chr1 = Character.codePointAt(input, i++);
            chr2 = (i < input.length()) ? Character.codePointAt(input, i++) : 0;
            chr3 = (i < input.length()) ? Character.codePointAt(input, i++) : 0;
            enc1 = chr1 >> 2;
            enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
            enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
            enc4 = chr3 & 63;


            if (!((chr2 > 47) && (chr2 < 58))) {
                enc3 = enc4 = 64;
            } else if (!((chr3 > 47) && (chr3 < 58))) {
                enc4 = 64;
            }
            output = output + keyStr.charAt(enc1) + keyStr.charAt(enc2)
                    + keyStr.charAt(enc3) + keyStr.charAt(enc4);
            chr1 = chr2 = chr3;
            enc1 = enc2 = enc3 = enc4;
        } while (i < input.length());

        return output;

    }


    public void calcMHXY() {


//        召唤兽气血=等级*体力资质/1000+体质点数*成长*6；
//
//        召唤兽魔法=等级*法力资质/500+法力点数*成长*3；
//
//        召唤兽伤害=等级*攻击资质*(14+10*成长)/7500+力量点数*成长；
//
//        召唤兽防御=等级*防御资质*(9.4+19/3*成长)/7500+耐力点数*成长*4/3；
//
//        召唤兽速度=敏捷点数*速度资质/1000
//
//        召唤兽灵力=等级*(法力资质+1662)*(1+成长)/7500+体质点数*0.3+耐力点数*0.2+力量点数*0.4+法力点数*0.7。

        double i = 119 * (2609 + 1640) * (1.252 + 1) / 7500 + 133 * 0.3 + 810 * 0.7 + 137 * .4 + 151 * 0.2;

        i = 119 * (2639 + 1640) * (1.264 + 1) / 7500 + 203 * 0.3 + 776 * 0.7 + 145 * .4 + 129 * 0.2;

        i = 119 * (3000 + 1640) * (1.224 + 1) / 7500 + 150 * 0.3 + 879 * 0.7 + 134 * .4 + 134 * 0.2;

        i = 119 * 3133 / 1000 + 203 * 1.3 * 6;

        System.out.println("i = " + i);
    }

    public interface DoAction {

        public void action(String mcode);
    }


}
