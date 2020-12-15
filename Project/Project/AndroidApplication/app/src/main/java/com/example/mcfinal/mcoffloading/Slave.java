package com.example.mcfinal.mcoffloading;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class Slave {
    public static Slave data;
    private Slave(){}
    public HashMap<String, String> getSlaveInfoMap() {
        return slaveInfoMap;
    }

    public HashMap<String, String> slaveInfoMap = new HashMap<>();
    public static Slave getInstance() {
        if (data == null) {
            data=new Slave();
        }
        return data;
    }
    public void viewMapContents() {
        Map<String,String> map=slaveInfoMap;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            Log.i("Slave Information Map: ", (entry.getKey() + " = " + entry.getValue()));
        }
    }
}

