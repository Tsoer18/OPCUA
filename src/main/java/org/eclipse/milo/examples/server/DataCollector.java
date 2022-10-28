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
import java.util.Iterator;

public class DataCollector {
    private static final String USER_AGENT = "Mozilla/5.0";

    private static final String GET_URL = "http://gw-6c9c.sandbox.tek.sdu.dk/ssapi/zb/dev";

    private static final String POST_URL = "https://localhost:9090/SpringMVCExample/home";

    private static final String POST_PARAMS = "userName=Pankaj";

    public static void main(String[] args) throws IOException, ParseException {

        sendGET();
        System.out.println("GET DONE");

    }

    private static void sendGET() throws IOException, ParseException {
        URL obj = new URL(GET_URL);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);
        int responseCode = con.getResponseCode();
        System.out.println("GET Response Code :: " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) { // success
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONParser parser = new JSONParser();


            ArrayList list = new ArrayList();

            for (int i = 0; i < 5; i++){
            Object object = parser.parse(response.toString());
            JSONArray jo = (JSONArray) object;
            JSONObject jsonObject = (JSONObject) jo.get(i);

            String id = (jsonObject.get("id")).toString();
            list.add(id);

            //System.out.println(jsonObject.get("id"));
            // print result
            //System.out.println(response.toString());
            }
            System.out.println(list);
        } else {
            System.out.println("GET request not worked");
        }


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