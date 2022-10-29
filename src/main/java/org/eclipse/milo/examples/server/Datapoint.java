package org.eclipse.milo.examples.server;

public class Datapoint {
    public String dpkey;
    public String name;
    public String access;

    public Datapoint (String dpkey){
        this.dpkey = dpkey;
    }

    public void setName (String name){
        this.name = name;
    }
    public void setAccess(String access){
        this.access =access;
    }

}
