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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private enum TransmitterStatus {IDLE, DISCOVERING, ADVERTISING, CONNECTED};

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
    private String rescuerID = "";
    private String IMEI;
    private TransmitterStatus transmitterStatus = TransmitterStatus.IDLE;
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    private Simulator simulator;
    private Rescuer rescuer;
    private Spinner spinner;
    private CSVFile csvFile;

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
                    transmitterStatus = TransmitterStatus.CONNECTED;
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
            if(s.equals(coordinatorID)) {
                coordinatorID = "";
                transmitterStatus = TransmitterStatus.IDLE;
            }
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
                    rescuerID = s;
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
            Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
            rescuerID = "";
            Nearby.getConnectionsClient(getApplicationContext()).stopAdvertising();
            startDiscovery();
        }
    };
    PayloadCallback payloadReceiver = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String s, Payload payload) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(payload.asBytes());
                ObjectInputStream is = new ObjectInputStream(bis);
                rescuer = (Rescuer) is.readObject();
                Nearby.getConnectionsClient(getApplicationContext()).disconnectFromEndpoint(s);
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
            t.setText(String.format("%.0f", victim.getRespiratoryRate()) + "odd./min");

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
        Simulator.Lifeline l = Simulator.Lifeline.values()[spinner.getSelectedItemPosition()];
        simulator = new Simulator(l, this);

        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(this);
        Button stopButton = findViewById(R.id.stopButton);
        stopButton.setOnClickListener(this);
        Button pauseButton = findViewById(R.id.pauseButton);
        pauseButton.setOnClickListener(this);

        TextView t = findViewById(R.id.reset_transmitter);
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectionsClient adapter = Nearby.getConnectionsClient(getApplicationContext());
                switch (transmitterStatus){
                    case CONNECTED:
                        break;
                    case ADVERTISING:
                        adapter.stopAdvertising();
                        startAdvertising();
                        break;
                    case DISCOVERING:
                        adapter.stopDiscovery();
                        startDiscovery();
                        break;
                    default:
                        startAdvertising();
                }
                updateSettings();
            }
        });




        checkingDirectory();
        loadCSVFile();
    }

    public void updateSettings(){
        TextView t = findViewById(R.id.transmitter_status_label);
        switch (transmitterStatus){
            case ADVERTISING:
                t.setText("czeka na ZRM");
                break;
            case DISCOVERING:
                t.setText("łączy się z KAM");
                break;
            case CONNECTED:
                t.setText("połączony z KAM");
                break;
            default:
                t.setText("bezczynny");
        }
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
        ViewFlipper vf = findViewById(R.id.layout_manager);
        switch(id){
            case R.id.action_settings:
                updateSettings();
                vf.setDisplayedChild(1);
                return true;
            default:
                vf.setDisplayedChild(0);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.startButton:
                Toast.makeText(MainActivity.this, "Start", Toast.LENGTH_SHORT).show();
                try {
                    checkingDirectory();
                    loadCSVFile();
                    simulator.setStatesList(csvFile.read());
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
                if(simulator.alive) {
                    if (simulator.isPaused()) {
                        simulator.unpause();
                        ((Button) findViewById(R.id.pauseButton)).setText("PAUZA");
                    } else {
                        simulator.pause();
                        ((Button) findViewById(R.id.pauseButton)).setText("KONT.");
                    }
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
                            transmitterStatus = TransmitterStatus.ADVERTISING;
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
                            transmitterStatus = TransmitterStatus.DISCOVERING;
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
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




    private void loadCSVFile(){
        try {
            File file = new File("/storage/emulated/0/life_lines/" + spinner.getItemAtPosition(spinner.getSelectedItemPosition()).toString());
            InputStream inputStream = new FileInputStream(file);
            OutputStream outputStream = new FileOutputStream(file);
            csvFile.setCSVFile(inputStream, outputStream);
        } catch(IOException e){
            Toast.makeText(getApplicationContext(), "Problem z wczytaniem pliku", Toast.LENGTH_SHORT ).show();
            Log.e("TAG", e.getMessage());
        }
    }


    // check content of directory "/storage/emulated/0/life_lines"
    // if it not contains any file, function will create file with life line
    private void checkingDirectory()
    {
        File directory = new File("/storage/emulated/0/life_lines");
        if (!directory.exists()) {
            if (directory.mkdir()) {
                System.out.println("Directory is created!");
            } else {
                System.out.println("Failed to create directory!");
            }
        }
        String[] filesList = directory.list();
        if(filesList.length == 0)
        {
            // TODO:
            // Tworzenie przykladowego pliku z linia zycia
            String csvFilePath = directory.getPath()+"linia0.csv";     // ZAPISZ NAZWE PLIKU GDZIES POZA FUNKCJA, NAPISZ FUNKCJE USTALAJACA SCIEZKE
            try {
                FileWriter writer = new FileWriter(csvFilePath);
                ArrayList<String> victimStatesExample = new ArrayList<String>();
                //breathing, respiratoryRate, capillaryRefillTime, walking, consciousness
                        // AWAKE, VERBAL, PAIN, UNRESPONSIVE
                victimStatesExample.add("true,45,5,true,VERBAL");
                victimStatesExample.add("true,35,4,false,PAIN");
                victimStatesExample.add("true,30,3,false,AWAKE");
                victimStatesExample.add("true,25,2,false,UNRESPONSIVE");
                victimStatesExample.add("false,15,1,false,UNRESPONSIVE");


                try {
                    File file = new File(csvFilePath);
                    InputStream inputStream = new FileInputStream(file);
                    OutputStream outputStream = new FileOutputStream(file);
                    csvFile.setCSVFile(inputStream, outputStream);
                    csvFile.write(victimStatesExample);
                } catch(IOException e){
                    Toast.makeText(getApplicationContext(), "Problem z wczytaniem pliku", Toast.LENGTH_SHORT ).show();
                    Log.e("TAG", e.getMessage());
                }

                filesList = directory.list();
            }catch(Exception e)
            {
                Toast.makeText(getApplicationContext(), "Nie udalo sie stworzyc pliku z przykladowymi danymi", Toast.LENGTH_SHORT ).show();
                Log.e("TAG", e.getMessage());
            }
        }

        // save list of files in spinner
        Spinner spin = (Spinner) findViewById(R.id.spin_lifeline);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, filesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(adapter);
    }

}
