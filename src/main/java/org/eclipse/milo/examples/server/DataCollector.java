package org.eclipse.milo.examples.server;


import org.json.simple.JSONObject;
import org.json.simple.parser.*;
import org.json.simple.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class DataCollector {
    private static final String USER_AGENT = "Mozilla/5.0";

    private static final String GET_URL = "http://gw-6c9c.sandbox.tek.sdu.dk/ssapi/zb/dev";

    private static final String POST_URL = "https://localhost:9090/SpringMVCExample/home";

    private static final String POST_PARAMS = "userName=Pankaj";

    public static void main(String[] args) throws IOException, ParseException {

        URL obj = new URL(GET_URL);
        StringBuffer response = sendGET(obj);
        ArrayList list = getId(response);
        for (Object i : list){
            System.out.println(i.toString());
        }
        ArrayList<URL> deviceURLs = new ArrayList();
        for (int i = 0; i<list.size(); i++){
            URL test = new URL (GET_URL +"/"+ list.get(i)+ "/ldev");
            deviceURLs.add(test);
        }
        StringBuffer test1= sendGET(deviceURLs.get(1));
        ArrayList m = getLogicalDevices(test1);
        for (Object i : m){
            System.out.println(i.toString());
        }
        URL almostFinal = new URL(GET_URL+"/"+list.get(1)+"/ldev/"+m.get(0)+"/data");
        System.out.println(almostFinal.toString());
        StringBuffer finalresponse = sendGET(almostFinal);
        ArrayList nodeKeys = getNodes(finalresponse);
        for(Object key : nodeKeys) {
            System.out.println(key.toString());
        }
        URL final1 = new URL(GET_URL+"/"+list.get(1)+"/ldev/"+m.get(0)+"/data/onoff");
        StringBuffer datapoints = sendGET(final1);
        ArrayList datapoints1 = getDatapoints(datapoints);
        for(Object x : datapoints1){
            System.out.println(x.toString());
        }






    }

    private static StringBuffer sendGET(URL obj) throws IOException, ParseException {

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);
        int responseCode = con.getResponseCode();
        System.out.println("GET Response Code :: " + responseCode);
        StringBuffer response = new StringBuffer();

        if (responseCode == HttpURLConnection.HTTP_OK) { // success
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    con.getInputStream()));
            String inputLine;


            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response;
        } else {
            System.out.println("GET request not worked");
            return response;

        }



    }

    public static ArrayList getDatapoints(StringBuffer response) throws  ParseException{
        JSONParser parser = new JSONParser();
        ArrayList list = new ArrayList();
        Object object = parser.parse(response.toString());
        JSONArray jo = (JSONArray) object;
        for (int i = 0; i<jo.size(); i++){
            JSONObject jsonObject = (JSONObject) jo.get(i);
            String x = (jsonObject).get("key").toString();
            list.add(x);
        }

        return list;
    }
    public static ArrayList getNodes(StringBuffer response) throws  ParseException{
        JSONParser parser = new JSONParser();
        ArrayList list = new ArrayList();
        Object object = parser.parse(response.toString());
        JSONArray jo = (JSONArray) object;
        for (int i = 0; i<jo.size(); i++){
            JSONObject jsonObject = (JSONObject) jo.get(i);
            String x = (jsonObject).get("key").toString();
            list.add(x);
        }

        return list;
    }
    public static ArrayList getLogicalDevices (StringBuffer response) throws ParseException{
        JSONParser parser = new JSONParser();
        ArrayList list = new ArrayList();
        Object object = parser.parse (response.toString());
        JSONArray jo = (JSONArray) object;
        for (int i = 0; i<jo.size();i++){
            JSONObject jsonObject = (JSONObject) jo.get(i);
            String key = (jsonObject).get("key").toString();
            list.add(key);
        }
        return list;

    }

    public static ArrayList getId(StringBuffer response) throws ParseException {
        JSONParser parser = new JSONParser();
        ArrayList list = new ArrayList();

            Object object = parser.parse(response.toString());
            JSONArray jo = (JSONArray) object;

            for (int i = 0; i < jo.size(); i++){
            JSONObject jsonObject = (JSONObject) jo.get(i);

            String id = (jsonObject.get("id")).toString();
            list.add(id);

            //System.out.println(jsonObject.get("id"));
            // print result
            //System.out.println(response.toString());
        }
        return list;
        //System.out.println(list);
    }

    private static void sendPOST() throws IOException {
        URL obj = new URL(POST_URL);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);

        // For POST only - START
        con.setDoOutput(true);
        OutputStream os = con.getOutputStream();
        os.write(POST_PARAMS.getBytes());
        os.flush();
        os.close();
        // For POST only - END

        int responseCode = con.getResponseCode();
        System.out.println("POST Response Code :: " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) { //success
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // print result
            System.out.println(response.toString());
        } else {
            System.out.println("POST request not worked");
        }
    }

}