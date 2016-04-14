package edu.ohio_state.cse.cardtracker;

import android.app.NotificationManager;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int TWO_MINUTES = 1000 * 60 * 2;

    private Button connectButton;
    private Button disconnectButton;
    private EditText ipAddressBox;
    private EditText portBox;

    private Thread clientThread;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initiate state
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configure the widgets
        connectButton = (Button) findViewById(R.id.b_connect);
        disconnectButton = (Button) findViewById(R.id.b_disconnect);
        ipAddressBox = (EditText) findViewById(R.id.et_ip_address);
        portBox = (EditText) findViewById(R.id.et_port_number);

        // Set the click listeners
        connectButton.setOnClickListener(this);
        disconnectButton.setOnClickListener(this);

        // Our disconnect button should not be enabled unless we're connected.
        disconnectButton.setEnabled(false);

        // Configure location updates.
        this.configureLocation();
    }

    /**
     * Determines whether or not the provided location is better than the current best.
     *
     * @param location The most-recently acquired location from the current provider.
     * @param currentBestLocation The best current known location.
     * @return Whether or not the location is better now.
     */
    private static boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private static boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    /**
     * Configures location updates.
     */
    private void configureLocation() {
        this.locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        this.locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d(MainActivity.TAG,"Detected Location change");
                if (isBetterLocation(location, MainActivity.this.currentLocation)) {
                    Log.d(MainActivity.TAG,"About to set currentLocation");
                    MainActivity.this.currentLocation = location;
                    Log.d(MainActivity.TAG,"Set currentLocation");
                }

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this.locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this.locationListener);
        } catch (SecurityException e) {
            Log.d(MainActivity.TAG,"Security Exception");
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.b_connect:
                this.clientThread = new Thread(
                        new ClientThread()
                );
                this.clientThread.start();
                this.connectButton.setEnabled(false);
                this.disconnectButton.setEnabled(true);
                break;
            case R.id.b_disconnect:
                this.clientThread.interrupt();
                this.disconnectButton.setEnabled(false);
                this.connectButton.setEnabled(true);
                this.clientThread = null;
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            this.locationManager.removeUpdates(this.locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * Represents the thread with the socket listening to the "bank".
     */
    private class ClientThread implements Runnable {

        /*
         * References to the client and stream reader.
         */
        private Socket client;
        private BufferedReader reader;

        /*
         * The list of transactions.
         */
        private List<Transaction> transactions;

        public ClientThread() {
            this.transactions = new ArrayList<>();
        }

        @Override
        public void run() {

            try {
                this.client = new Socket(
                        ipAddressBox.getText().toString(),
                        Integer.parseInt(portBox.getText().toString()));

                this.reader = new BufferedReader(
                        new InputStreamReader(this.client.getInputStream()));

                while (!Thread.currentThread().isInterrupted()) {
                    // Don't do anything if we're not ready to read from the socket.
                    if (!reader.ready()) {
                        Thread.sleep(500);
                        continue;
                    }

                    // Getting to this point means there's something to be read.
                    final String line = reader.readLine();
                    this.onDataFromServer(line);
                }
            } catch (InterruptedException e) {
                // We received an interrupt while sleeping, no error.
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    this.reader.close();
                    this.client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Creates and shows a new fraud notification.
         *
         * @param trx The fraudulent transaction.
         * @param reason The reason that the transaction is fraudulent.
         */
        protected void createFraudNotification(Transaction trx, final String reason) {
            Log.d(MainActivity.TAG, "A fraud notification was requested.");

            NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Potential Fraud Detected: " + trx.getVendor())
                    .setContentText(reason);
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(0, builder.build());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, reason, Toast.LENGTH_LONG).show();
                }
            });
        }

        boolean requiredQuickTravel(int firstTime, int secondTime, float distance){
            int firstDate = firstTime/10000;
            int secondDate = secondTime/10000;
            boolean isSuspicious = false;
            //if the transactions happened on the same day
            if(firstDate==secondDate){
                int timeDifference = Math.abs(firstTime-secondTime);
                //within the last hour
                if((timeDifference)<100){
                    //more than 200 miles away
                    isSuspicious = distance > 321869;
                    //within last 5 hours
                }else if(timeDifference<500){
                    //more than 1000 miles away
                    isSuspicious = distance >  1609000;
                }
            }
            return isSuspicious;
        }

        /**
         * Called when the server sends us new data.  Use this opportunity to check the validity
         * of a transaction and send a fraud notification if necessary.
         *
         * @param data The string data from the server.
         */
        protected void onDataFromServer(final String data) {
            // For now, show the eventual text.
            Log.d(MainActivity.TAG, "Received data: " + data);
            // Create the transaction.
            Transaction trx = new Transaction(data);



            if(currentLocation!=null) {
                if (trx.getAmount() > 500) {
                    createFraudNotification(trx, "Transaction amount greater than $500.");
                } else if (!trx.usedChip() && trx.getAmount() > 25) {
                    createFraudNotification(trx, "Transaction without chip greater than $25.");
                }else if (trx.getLocation().distanceTo(currentLocation) > 500) {
                    createFraudNotification(trx, "Transaction further than 500 meters away.");
                }else if (trx.getVendor().toLowerCase().contains("vend") && trx.getAmount() > 10) {
                    createFraudNotification(trx, "Vending machine made transaction greater than $10.");
                } else {

                        boolean firstTime = true;
                        Transaction mostRecent;

                        // TODO: Check previous transactions and check for fraud.
                        for (Transaction t : this.transactions) {
                             if (requiredQuickTravel(t.getTimeStamp(), trx.getTimeStamp(), trx.getLocation().distanceTo(t.getLocation()))) {
                                createFraudNotification(trx, "Two transactions occurred far apart over a short amount of time.");
                             }
                        }
                }

            }else{
                createFraudNotification(trx, "phone location is null");
            }

            // Add the transaction to the running list
            this.transactions.add(trx);
        }
    }
}
