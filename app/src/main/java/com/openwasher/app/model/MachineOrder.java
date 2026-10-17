package com.openwasher.app.model;

import java.io.Serializable;

public class MachineOrder implements Serializable {
    private String orderId;
    private String machineId;
    private String programName;
    private int durationMinutes;
    private int costRupees;
    private String paymentType; // "FREE", "VOLUNTARY", "COUPON"
    private long timestamp;

    public MachineOrder(String orderId, String machineId, String programName, int durationMinutes, int costRupees, String paymentType, long timestamp) {
        this.orderId = orderId;
        this.machineId = machineId;
        this.programName = programName;
        this.durationMinutes = durationMinutes;
        this.costRupees = costRupees;
        this.paymentType = paymentType;
        this.timestamp = timestamp;
    }

    public String getOrderId() { return orderId; }
    public String getMachineId() { return machineId; }
    public String getProgramName() { return programName; }
    public int getDurationMinutes() { return durationMinutes; }
    public int getCostRupees() { return costRupees; }
    public String getPaymentType() { return paymentType; }
    public long getTimestamp() { return timestamp; }
}
