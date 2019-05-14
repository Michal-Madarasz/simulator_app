package com.example.simulator_app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.triage.model.Rescuer;
import com.triage.model.Victim;

import org.javatuples.Triplet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE
            };

    final private String RESCUER_SERVICE_ID = "triage.rescuer-simulator";
    final private String COORDINATOR_SERVICE_ID = "triage.simulator-rescuer";
    private String coordinatorID = "";
    private String IMEI;
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    private Simulator simulator;
    private Rescuer rescuer;
    private Spinner spinner;

    EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(String s, DiscoveredEndpointInfo discoveredEndpointInfo) {
            Nearby.getConnectionsClient(getApplicationContext()).requestConnection(discoveredEndpointInfo.getEndpointName(), s, communicationCallbacksCoordinator);
        }

        @Override
        public void onEndpointLost(String s) {

        }
    };
    ConnectionLifecycleCallback communicationCallbacksCoordinator = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(final String endID, ConnectionInfo connectionInfo) {
            Nearby.getConnectionsClient(getApplicationContext()).acceptConnection(endID, payloadReceiver);
        }

        @Override
        public void onConnectionResult(String s, ConnectionResolution connectionResolution) {
            switch (connectionResolution.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    // We're connected! Can now start sending and receiving data.
                    coordinatorID = s;
                    break;
                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    // The connection was rejected by one or both sides.
                    break;
                case ConnectionsStatusCodes.STATUS_ERROR:
                    // The connection broke before it was able to be accepted.
                    break;
                default:
                    // Unknown status code
            }
        }

        @Override
        public void onDisconnected(String s) {
            if(s.equals(coordinatorID))
                coordinatorID = "";
        }
    };


    ConnectionLifecycleCallback communicationCallbacksRescuers = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(final String endID, ConnectionInfo connectionInfo) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Autoryzacja")
                    .setMessage("Wykryto próbę połączenia.\nCzy drugie urządzenie wyświetla kod: " + connectionInfo.getAuthenticationToken())
                    .setPositiveButton("tak", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Nearby.getConnectionsClient(getApplicationContext()).acceptConnection(endID, payloadReceiver);
                        }
                    })
                    .setNegativeButton("nie", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Nearby.getConnectionsClient(getApplicationContext()).rejectConnection(endID);
                        }
                    })
                    .show();
        }

        @Override
        public void onConnectionResult(String s, ConnectionResolution connectionResolution) {
            switch (connectionResolution.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    // We're connected! Can now start sending and receiving data.
                    break;
                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    // The connection was rejected by one or both sides.
                    break;
                case ConnectionsStatusCodes.STATUS_ERROR:
                    // The connection broke before it was able to be accepted.
                    break;
                default:
                    // Unknown status code
            }
        }

        @Override
        public void onDisconnected(String s) {
            //Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
        }
    };
    PayloadCallback payloadReceiver = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String s, Payload payload) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(payload.asBytes());
                ObjectInputStream is = new ObjectInputStream(bis);
                rescuer = (Rescuer) is.readObject();
                Nearby.getConnectionsClient(getApplicationContext()).stopAdvertising();
                startDiscovery();
            } catch (Exception e){
                Toast.makeText(getApplicationContext(), "Błąd odbioru informacji o ratowniku", Toast.LENGTH_SHORT ).show();
                Log.e("TAG", e.getMessage());
            }
        }

        @Override
        public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {

        }
    };

    public static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void transferPayload(Victim victim){
        if(coordinatorID.equals("")) return;

        Triplet<String, Rescuer, Victim> data = new Triplet<>(IMEI, rescuer, victim);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(data);
            oos.flush();
            byte[] bytes = bos.toByteArray();
            Payload bytesPayload = Payload.fromBytes(bytes);
            Nearby.getConnectionsClient(getApplicationContext()).sendPayload(coordinatorID, bytesPayload);
            //Toast.makeText(getApplicationContext(), "Wysłano", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void actualize(Victim victim) {
        try {
            TextView t;

            t = findViewById(R.id.IMEI_val);
            t.setText(String.format("%d", victim.getId()));

            t = findViewById(R.id.breath_val);
            if (victim.isBreathing())
                t.setText("tak");
            else
                t.setText("nie");

            t = findViewById(R.id.refill_val);
            t.setText(victim.getRespiratoryRate() + "odd./min");

            t = findViewById(R.id.pulse_val);
            t.setText(victim.getCapillaryRefillTime() + "s");

            t = findViewById(R.id.walking_val);
            if (victim.isWalking())
                t.setText("tak");
            else
                t.setText("nie");

            t = findViewById(R.id.conscious_val);
            switch (victim.getConsciousness()) {
                case AWAKE:
                    t.setText("przytomny");
                    break;
                case VERBAL:
                    t.setText("reag. na głos");
                    break;
                case PAIN:
                    t.setText("reag. na ból");
                    break;
                case UNRESPONSIVE:
                    t.setText("nieprzytomny");
                    break;
            }

            ImageView img = findViewById(R.id.color_val);
            switch (victim.getColor()) {
                case BLACK:
                    img.setImageResource(R.color.colorTriageBlack);
                    break;
                case RED:
                    img.setImageResource(R.color.colorTriageRed);
                    break;
                case YELLOW:
                    img.setImageResource(R.color.colorTriageYellow);
                    break;
                case GREEN:
                    img.setImageResource(R.color.colorTriageGreen);
                    break;
            }
        }catch(NullPointerException e){
            e.printStackTrace();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        spinner = findViewById(R.id.spin_lifeline);
        Victim victim = new Victim();
        simulator = new Simulator(victim, this);

        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(this);
        Button stopButton = findViewById(R.id.stopButton);
        stopButton.setOnClickListener(this);
        Button pauseButton = findViewById(R.id.pauseButton);
        pauseButton.setOnClickListener(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.startButton:
                Toast.makeText(MainActivity.this, "Start", Toast.LENGTH_SHORT).show();
                try {
                    simulator.start();
                } catch (IllegalThreadStateException e) { //something went wrong
                    e.printStackTrace();
                    simulator.start();
                }
                break;
            case R.id.stopButton:
                Toast.makeText(MainActivity.this, "Stop", Toast.LENGTH_SHORT).show();
                simulator.kill();
                break;
            case R.id.pauseButton:
                if(simulator.isPaused()){
                    simulator.unpause();
                    ((Button)findViewById(R.id.pauseButton)).setText("PAUZA");
                }
                else {
                    simulator.pause();
                    ((Button)findViewById(R.id.pauseButton)).setText("KONT.");
                }
                break;
            default:
        }
    }

    private void startAdvertising() {
        //Toast.makeText(getApplicationContext(), "Startujemy", Toast.LENGTH_SHORT).show();
        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();
        Nearby.getConnectionsClient(getApplicationContext()).startAdvertising(
                "Symulator", RESCUER_SERVICE_ID, communicationCallbacksRescuers, advertisingOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            Toast.makeText(getApplicationContext(), "Startujemy nadawanie", Toast.LENGTH_SHORT).show();
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            Toast.makeText(getApplicationContext(), "Nie wystartowano nadawania", Toast.LENGTH_SHORT).show();
                            Log.e("TAG", e.getMessage());
                        });
    }

    private void startDiscovery() {
        DiscoveryOptions discoveryOptions =
                new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build();
        Nearby.getConnectionsClient(getApplicationContext()).startDiscovery(
                COORDINATOR_SERVICE_ID,
                endpointDiscoveryCallback,
                discoveryOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            Toast.makeText(getApplicationContext(), "Startujemy odkrywanie", Toast.LENGTH_SHORT).show();
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            Toast.makeText(getApplicationContext(), "Nie wystartowano odkrywania", Toast.LENGTH_SHORT).show();
                            Log.e("TAG", e.getMessage());
                        });
    }



    /** Called when our Activity has been made visible to the user. */
    @Override
    protected void onStart() {
        super.onStart();
        if (!hasPermissions(this, getRequiredPermissions())) {
            if (!hasPermissions(this, getRequiredPermissions())) {
                if (Build.VERSION.SDK_INT < 23) {
                    ActivityCompat.requestPermissions(
                            this, getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS);
                } else {
                    requestPermissions(getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS);
                }
            }
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT < 23) {
                ActivityCompat.requestPermissions(
                        this, getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS);
            } else {
                requestPermissions(getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS);
            }
            return;
        }
        TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        IMEI = tm.getDeviceId();
        startAdvertising();
    }

    protected String[] getRequiredPermissions() {
        return REQUIRED_PERMISSIONS;
    }
}
