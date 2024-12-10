package com.example.moneynow;

public class RefundRequest {
    private String staffId; // Staff user-friendly ID
    private String uid; // Staff UID for backend operations
    private String vendorId; // Vendor UID
    private String transactionId;
    private String date;
    private String amount;
    private String status;

    public RefundRequest(String staffId, String uid, String vendorId, String transactionId, String date, String amount, String status) {
        this.staffId = staffId;
        this.uid = uid;
        this.vendorId = vendorId; // Use vendor UID
        this.transactionId = transactionId;
        this.date = date;
        this.amount = amount;
        this.status = status;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getDate() {
        return date;
    }

    public String getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }
}
