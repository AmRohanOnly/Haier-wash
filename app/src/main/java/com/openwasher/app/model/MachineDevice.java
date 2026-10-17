package com.openwasher.app.model;

import java.io.Serializable;

public class MachineDevice implements Serializable {
    private String id;
    private String name;
    private String type; // "WASHER", "DRYER", "SHOES"
    private String bleAddress;
    private boolean isAvailable;

    public MachineDevice(String id, String name, String type, String bleAddress, boolean isAvailable) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.bleAddress = bleAddress;
        this.isAvailable = isAvailable;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getBleAddress() { return bleAddress; }
    public boolean isAvailable() { return isAvailable; }
}
