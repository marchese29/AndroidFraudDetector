package edu.ohio_state.cse.cardtracker;

import android.location.Location;

/**
 * Represents a single transaction.
 */
public class Transaction {

    private int timeStamp;
    private String vendor;
    private Location location;
    private int amount;
    private boolean usedChip;

    public int getTimeStamp() {
        return timeStamp;
    }

    public String getVendor() {
        return vendor;
    }

    public int getAmount() {
        return amount;
    }

    public Location getLocation() {
        return location;
    }

    public boolean usedChip() {
        return usedChip;
    }

    /**
     * Calculates the distance in meters between this transaction and another one.
     *
     * @param trx The transaction to calculate distance against.
     * @return The distance between this transaction and the other one.
     */
    public float distanceInMeters(Transaction trx) {
        return this.location.distanceTo(trx.getLocation());
    }

    public Transaction(String content) {
        String[] items = content.split("\\?");
        this.timeStamp = Integer.parseInt(items[0]);
        this.vendor = items[1];

        this.location = new Location("");
        this.location.setLatitude(Double.parseDouble(items[2]));
        this.location.setLongitude(Double.parseDouble(items[3]));

        this.amount = Integer.parseInt(items[4]);
        this.usedChip = items[5].equals("y");
    }
}
