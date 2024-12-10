package com.example.moneynow;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class RefundRequestsAdapter extends ArrayAdapter<RefundRequest> {

    private List<RefundRequest> selectedRefundRequests = new ArrayList<>();

    public RefundRequestsAdapter(@NonNull Context context, @NonNull List<RefundRequest> objects) {
        super(context, 0, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_refund_request, parent, false);
        }

        RefundRequest refundRequest = getItem(position);
        if (refundRequest != null) {
            TextView staffIdTextView = convertView.findViewById(R.id.staffIdTextView);
            TextView vendorIdTextView = convertView.findViewById(R.id.vendorIdTextView);
            TextView transactionIdTextView = convertView.findViewById(R.id.transactionIdTextView);
            TextView dateTextView = convertView.findViewById(R.id.dateTextView);
            TextView amountTextView = convertView.findViewById(R.id.amountTextView);
            TextView statusTextView = convertView.findViewById(R.id.statusTextView);
            CheckBox selectCheckBox = convertView.findViewById(R.id.selectCheckBox);

            staffIdTextView.setText("Staff ID: " + refundRequest.getStaffId());
            vendorIdTextView.setText("Vendor ID: " + refundRequest.getVendorId()); // Use getVendorId() here
            transactionIdTextView.setText("Transaction ID: " + refundRequest.getTransactionId());
            dateTextView.setText("Date: " + refundRequest.getDate());
            amountTextView.setText("Amount: " + refundRequest.getAmount());
            statusTextView.setText("Status: " + refundRequest.getStatus());

            selectCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedRefundRequests.add(refundRequest);
                } else {
                    selectedRefundRequests.remove(refundRequest);
                }
            });

            selectCheckBox.setChecked(selectedRefundRequests.contains(refundRequest));
        }

        return convertView;
    }

    public List<RefundRequest> getSelectedRefundRequests() {
        return selectedRefundRequests;
    }
}
