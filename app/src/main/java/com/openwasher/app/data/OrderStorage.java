package com.openwasher.app.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.openwasher.app.model.MachineOrder;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class OrderStorage {
    private static final String PREF_NAME = "open_washer_prefs";
    private static final String KEY_ORDERS = "saved_orders";
    private static final String KEY_TOTAL_SAVINGS = "total_savings";

    private SharedPreferences prefs;

    public OrderStorage(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public synchronized void saveOrder(MachineOrder order) {
        List<MachineOrder> list = getOrders();
        list.add(0, order); // Add to top

        JSONArray array = new JSONArray();
        for (MachineOrder o : list) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("orderId", o.getOrderId());
                obj.put("machineId", o.getMachineId());
                obj.put("programName", o.getProgramName());
                obj.put("durationMinutes", o.getDurationMinutes());
                obj.put("costRupees", o.getCostRupees());
                obj.put("paymentType", o.getPaymentType());
                obj.put("timestamp", o.getTimestamp());
                array.put(obj);
            } catch (Exception ignored) {}
        }
        prefs.edit().putString(KEY_ORDERS, array.toString()).apply();

        if (order.getCostRupees() == 0) {
            int currentSavings = prefs.getInt(KEY_TOTAL_SAVINGS, 0);
            prefs.edit().putInt(KEY_TOTAL_SAVINGS, currentSavings + 49).apply();
        }
    }

    public List<MachineOrder> getOrders() {
        List<MachineOrder> result = new ArrayList<>();
        String raw = prefs.getString(KEY_ORDERS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                result.add(new MachineOrder(
                    obj.optString("orderId"),
                    obj.optString("machineId"),
                    obj.optString("programName"),
                    obj.optInt("durationMinutes"),
                    obj.optInt("costRupees"),
                    obj.optString("paymentType"),
                    obj.optLong("timestamp")
                ));
            }
        } catch (Exception ignored) {}
        return result;
    }

    public int getTotalSavings() {
        return prefs.getInt(KEY_TOTAL_SAVINGS, 49 * 14); // default demo value
    }

    public int getTotalWashes() {
        List<MachineOrder> orders = getOrders();
        return Math.max(14, orders.size());
    }

    public void clearHistory() {
        prefs.edit().remove(KEY_ORDERS).apply();
    }
}
