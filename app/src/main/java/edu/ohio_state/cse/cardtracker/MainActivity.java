package edu.ohio_state.cse.cardtracker;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private Button connectButton;
    private Button disconnectButton;
    private EditText ipAddressBox;
    private EditText portBox;

    private Thread clientThread;

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
        protected void createFraudNotification(Transaction trx, String reason) {
            Log.i(MainActivity.TAG, "A fraud notification was requested.");

            NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Potential Fraud Detected: " + trx.getVendor())
                    .setContentText(reason);
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(0, builder.build());
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

            // TODO: Check previous transactions and check for fraud.
            createFraudNotification(trx, data);
            for (Transaction t : this.transactions) {
                // See if we have any issues with this particular transaction and our new one.
            }

            // Add the transaction to the running list.
            this.transactions.add(trx);
        }
    }
}
