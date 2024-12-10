package com.example.moneynow;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.util.List;
import java.util.Map;

public class TransactionAdapter extends ArrayAdapter<Map<String, Object>> {

    public TransactionAdapter(@NonNull Context context, List<Map<String, Object>> transactions) {
        super(context, 0, transactions);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_transaction, parent, false);
        }

        Map<String, Object> transaction = getItem(position);
        if (transaction != null) {
            TextView textViewDate = convertView.findViewById(R.id.textViewTransactionDate);
            TextView textViewAmount = convertView.findViewById(R.id.textViewTransactionAmount);
            TextView textViewVendorUserID = convertView.findViewById(R.id.textViewTransactionVendorUserID);

            textViewDate.setText((String) transaction.get("date"));
            textViewAmount.setText((String) transaction.get("amount"));
            textViewVendorUserID.setText((String) transaction.get("vendorUserID"));
        }

        return convertView;
    }
}
