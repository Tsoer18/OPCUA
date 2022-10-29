package org.eclipse.milo.examples.server;

import javax.xml.crypto.Data;
import java.util.ArrayList;

public class LogicalDevice {
    public String key;
    public ArrayList<Datapoint> datapoints = new ArrayList<>();
    public LogicalDevice (String key){
        this.key = key;
    }

    public void setDatapoints(ArrayList<Datapoint> datapoints){
        this.datapoints = datapoints;
    }

    public String getKey(){
        return this.key;
    }
    public void addDatapoints (Datapoint datapoint){
        this.datapoints.add(datapoint);
    }
}
