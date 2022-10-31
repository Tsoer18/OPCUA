package org.eclipse.milo.examples.server;


import org.json.simple.JSONObject;
import org.json.simple.parser.*;
import org.json.simple.JSONArray;
import org.junit.experimental.theories.DataPoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class DataCollector {
    private static final String USER_AGENT = "Mozilla/5.0";

    public static final String GET_URL = "http://gw-6c9c.sandbox.tek.sdu.dk/ssapi/zb/dev";

    private static final String POST_URL = "https://localhost:9090/SpringMVCExample/home";

    private static final String POST_PARAMS = "userName=Pankaj";
    private String name;

    private ArrayList<Device> devices = new ArrayList();
    public DataCollector () throws IOException, ParseException {
        URL deviceURL = new URL(GET_URL);
        StringBuffer response = sendGET(deviceURL);
        ArrayList ids = getId(response);
        for (Object i : ids){

            Device device = new Device((String) i);
            System.out.println("Device Created with ID: "+ i.toString());
            URL logicalDeviceURL = new URL(GET_URL+ "/"+ i + "/ldev");
            StringBuffer logicalDeviceResponse = sendGET(logicalDeviceURL);
            ArrayList logicalDevicesRaw = getLogicalDevices(logicalDeviceResponse);
            for (Object lDEV: logicalDevicesRaw){
                LogicalDevice logicalDevice = new LogicalDevice((String)lDEV);
                System.out.println("Device has a logical device with key: " + lDEV.toString());
                device.addLogicalDevice(logicalDevice);
                URL nodeURL = new URL(GET_URL+ "/"+ i + "/ldev/" + logicalDevice.getKey()+"/data");
                StringBuffer nodeResponse = sendGET(nodeURL);
                ArrayList nodeRaw = getNodes(nodeResponse);
                for (Object nodeMediumRare : nodeRaw){
                    Datapoint datapoint = new Datapoint((String) nodeMediumRare);
                    System.out.println( "logical Device has a datapoint with key: " +nodeMediumRare);
                    logicalDevice.addDatapoints(datapoint);
                    URL datapointURL = new URL(GET_URL+ "/"+ i + "/ldev/" + logicalDevice.getKey()+"/data/" + nodeMediumRare);
                    StringBuffer datapointResponse = sendGET(datapointURL);
                    ArrayList datapointAccessAndName = getDatapoints(datapointResponse);
                    datapoint.setName((String) datapointAccessAndName.get(0));
                    datapoint.setAccess((String) datapointAccessAndName.get(1));
                    datapoint.setGeturl(datapointURL);
                    datapoint.setValue (datapointAccessAndName.get(2));
                    datapoint.setType((String) datapointAccessAndName.get(3));
                    System.out.println("Datapoint with key: " + nodeMediumRare + " Has the name: " + datapoint.name + " and access level: " + datapoint.access);

                }

            }
            devices.add(device);
        }

    }
    public static void main(String[] args) throws IOException, ParseException {
        DataCollector dataCollector = new DataCollector();

        }

        public String getName(){
        return this.name;
        }

    public static StringBuffer sendGET(URL obj) throws IOException, ParseException {

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);
        int responseCode = con.getResponseCode();

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
        JSONObject jo = (JSONObject)object;
            String name = (jo).get("name").toString();
            String access = (jo).get("access").toString();
            Object value = (jo).get("value");
            String type = (String) (jo).get("type");

            list.add(name);
            list.add(access);
            if (value != null) {
                    list.add(value);
            }else{
                list.add(2, 0);
            }
            list.add(3, type.toString());

            Object l = (jo).get("key");
            list.add(4, l.toString());
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
    public URL getGetUrl(){
        return this.getGetUrl();
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
    public ArrayList<Device> getDevices(){
        return this.devices;
    }


}