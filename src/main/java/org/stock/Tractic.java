package org.stock;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Tractic {

    /**
     *  收盘价不超过历史(近1年多)最低价格的 1 * %threshold%
     * @param result
     * @return
     * @throws IOException
     */
    public static  boolean calcPramisingByPrice(String result , double  threshold) throws IOException {
        ObjectMapper om = new ObjectMapper();
        Map map = om.readValue(result, Map.class);

        List<ArrayList> list = (List) map.get("line");

        float minClose = 0;
        float close = 0;
        for (int i = list.size()/10*7; i < list.size(); i++) {
            close = Float.valueOf(list.get(i).get(2).toString());
            minClose = minClose == 0 ? close : minClose > close ? close : minClose;
        }
        System.out.print(minClose +" * "+threshold + " = "  );
        System.out.print(minClose * threshold);
        System.out.print("  >  ");
        System.out.println(close );
        return minClose * threshold > close;

    }

    /**
     *  收盘价不超过历史(近1年多)最低价格的 110%
     * @param result
     * @return
     * @throws IOException
     */
    public static  boolean calcPramisingByPrice(String result ) throws IOException {
        return calcPramisingByPrice(result , 1.1);
    }

    /**
     *  当日融券余量
     * @param result
     * @return
     * @throws IOException
     */
    public static List getF008N(String result) throws IOException {
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

        return financeBuket;

    }

    /**
     * 融券余量减少一半  当日融券余量 小于昨日一半
     * @param result
     * @return
     * @throws IOException
     */
    public static  boolean isPromising3(String  result) throws IOException {

        List<Double> financeBuket = getF008N(result);
        Double currentStockLaon = financeBuket.get(0);
        Double yestodayStockLaon = financeBuket.get(1);



        return yestodayStockLaon < 5000000 ? false : currentStockLaon * 2 < yestodayStockLaon;
    }


    /**
     * 当日融资买入
     * @param result
     * @return
     * @throws IOException
     */
    public  static List  getF002N(String result) throws IOException {

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

        return financeBuyBuket;
    }

    /**
     * 返回 5日均线数据
     * @param result
     * @return
     * @throws IOException
     */

    public  static List  getAvgsForF001(String result) throws IOException {

        String stockID = null;
        List<Double> financeBuyBuket = new ArrayList<Double>();

        ObjectMapper om = new ObjectMapper();
        Map map = om.readValue(result, Map.class);

        List records = (List) map.get("records");
        Map  lineDate;

        int offset = records.size()-5 ;
        for (int i = 0; i <= offset; i++) {
            int j = i ;
            Double finance = 0D;
            while(j-i<5){
                lineDate = (Map) records.get(j++);
                finance+= (Double) lineDate.get("F001N");
            }
            Double avg = finance / 5D ;
            financeBuyBuket.add(avg);

        }

        return financeBuyBuket;
    }




    /**
     * 融资余量
     * @param result
     * @return
     * @throws IOException
     */
    public static List  getF001N(String result) throws IOException {

        String stockID = null;
        List<Double> financeBuyBuket = new ArrayList<Double>();

        ObjectMapper om = new ObjectMapper();
        Map map = om.readValue(result, Map.class);

        List re = (List) map.get("records");
        Map re2;


        for (int i = 0; i < 5; i++) {

            re2 = (Map) re.get(i);
            stockID = (String) re2.get("SECCODE");
            Double dailyBuyFinance = (Double) re2.get("F001N");
            financeBuyBuket.add(dailyBuyFinance);
        }

        return financeBuyBuket;
    }



    /**
     * 返回 5日均线数据多头排列
     * @param result
     * @return
     * @throws IOException
     */
    public  static boolean isPromising(String result) throws IOException {


        List<Double> financeBuyBuket = getAvgsForF001(result);

        for (int i = 1; i < financeBuyBuket.size()-1 ; i++) {

            //  误差在 0.005 内
            if(! (financeBuyBuket.get(i) * 1.005 > financeBuyBuket.get(i+1))){
                return false;
            }
        }
        return true;
    }




    /**
     * 融资余量 dailyFinanceBuy > yestoday and keep for 4 day
     * @param
     * @return
     */
    public static  boolean isPromising2(String result) throws IOException {


        List<Double> financeBuyBuket = getF001N(result);
        Double today = financeBuyBuket.get(0);
        for(int i =1 ; i< financeBuyBuket.size() ; i++ ){

            if(today <  financeBuyBuket.get(i)){
                return false;
            }
            today  =   financeBuyBuket.get(i);
        }
        return true;
    }

}
