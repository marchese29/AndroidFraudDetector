package edu.ohio_state.cse.cardtracker;

/**
 * Represents a single transaction.
 */
public class Transaction {

    private int timeStamp;
    private String vendor;
    private int amount;
//    private boolean usedChip;

    public int getTimeStamp() {
        return timeStamp;
    }

    public String getVendor() {
        return vendor;
    }

    public int getAmount() {
        return amount;
    }

//    public boolean usedChip() {
//        return usedChip;
//    }

    public Transaction(String content) {
        String[] items = content.split("\\?");
        this.timeStamp = Integer.parseInt(items[0]);
        this.vendor = items[1];
        this.amount = Integer.parseInt(items[2]);
//        this.usedChip = Boolean.parseBoolean(items[3]);
    }
}
