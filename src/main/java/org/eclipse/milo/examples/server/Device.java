package org.eclipse.milo.examples.server;

import java.util.ArrayList;

public class Device {
    public String ID;
    public ArrayList<LogicalDevice> logicalDevices = new ArrayList();

    public Device (String ID){
        this.ID = ID;

    }

    public void setLogicalDevices (ArrayList<LogicalDevice> logicalDevices) {
        this.logicalDevices = logicalDevices;
    }
    public void addLogicalDevice(LogicalDevice logicalDevice){
        this.logicalDevices.add(logicalDevice);
    }

    public String getID (){
        return this.ID;
    }
    public ArrayList<LogicalDevice> getLogicalDevices(){
        return this.logicalDevices;
    }



}
