package org.eclipse.milo.examples.server;

import java.net.URL;

public class Datapoint {
    public String dpkey;
    public String name;
    public String access;

    public URL geturl;
    public String type;
    public Object value;


    public Datapoint (String dpkey){
        this.dpkey = dpkey;
    }

    public void setName (String name){
        this.name = name;
    }
    public void setAccess(String access){
        this.access =access;
    }

    public void setGeturl (URL geturl){
        this.geturl = geturl;
    }

    public void setType (String type){
        this.type = type;
    }
    public void setValue (Object value){
        this.value = value;
    }

}
