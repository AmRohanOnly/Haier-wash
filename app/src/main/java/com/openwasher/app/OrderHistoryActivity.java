package com.openwasher.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.openwasher.app.data.OrderStorage;
import com.openwasher.app.model.MachineOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderHistoryActivity extends Activity {

    private OrderStorage storage;
    private List<MachineOrder> orderList;
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        storage = new OrderStorage(this);
        orderList = storage.getOrders();

        // If list is empty, inject a couple of sample records for demonstration
        if (orderList.isEmpty()) {
            storage.saveOrder(new MachineOrder("ORD-A109", "WASHER-01", "Standard Cotton (35 min)", 35, 0, "FREE_COMMUNITY", System.currentTimeMillis() - 86400000));
            storage.saveOrder(new MachineOrder("ORD-B214", "DRYER-02", "Hot Air Dry (40 min)", 40, 0, "FREE_COMMUNITY", System.currentTimeMillis() - 172800000));
            orderList = storage.getOrders();
        }

        Button btnBack = findViewById(R.id.btnBackHistory);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ListView lvHistory = findViewById(R.id.lvHistory);
        adapter = new HistoryAdapter();
        lvHistory.setAdapter(adapter);

        Button btnClear = findViewById(R.id.btnClearHistory);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storage.clearHistory();
                orderList.clear();
                adapter.notifyDataSetChanged();
            }
        });
    }

    private class HistoryAdapter extends BaseAdapter {
        private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

        @Override
        public int getCount() {
            return orderList.size();
        }

        @Override
        public Object getItem(int position) {
            return orderList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(OrderHistoryActivity.this).inflate(R.layout.item_history, parent, false);
            }

            MachineOrder order = orderList.get(position);

            TextView tvProgram = convertView.findViewById(R.id.tvHistoryProgram);
            TextView tvCost = convertView.findViewById(R.id.tvHistoryCost);
            TextView tvMachine = convertView.findViewById(R.id.tvHistoryMachine);
            TextView tvDate = convertView.findViewById(R.id.tvHistoryDate);

            tvProgram.setText(order.getProgramName());
            if (order.getCostRupees() == 0) {
                tvCost.setText("FREE (₹ 0)");
                tvCost.setTextColor(getResources().getColor(R.color.success));
            } else {
                tvCost.setText("Paid: ₹ " + order.getCostRupees());
                tvCost.setTextColor(getResources().getColor(R.color.accent));
            }

            tvMachine.setText("Unit: " + order.getMachineId());
            tvDate.setText(sdf.format(new Date(order.getTimestamp())));

            return convertView;
        }
    }
}
