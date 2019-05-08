package com.example.simulator_app;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.triage.Victim;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //private Victim victim;
    private Simulator simulator;
    private Spinner spinner;


    public void actualize(Victim victim) {
        TextView t;
        t = findViewById(R.id.breath_val);
        if(victim.isBreathing())
            t.setText("tak");
        else
            t.setText("nie");

        t = findViewById(R.id.refill_val);
        t.setText(victim.getRespiratoryRate()+"odd./min");

        t = findViewById(R.id.pulse_val);
        t.setText(victim.getCapillaryRefillTime()+"s");

        t = findViewById(R.id.walking_val);
        if(victim.isWalking())
            t.setText("tak");
        else
            t.setText("nie");

        t = findViewById(R.id.conscious_val);
        switch(victim.getConsciousness()){
            case AWAKE: t.setText("przytomny"); break;
            case VERBAL: t.setText("reag. na głos"); break;
            case PAIN: t.setText("reag. na ból"); break;
            case UNRESPONSIVE: t.setText("nieprzytomny"); break;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        spinner = findViewById(R.id.spin_lifeline);
        Victim victim = new Victim(spinner.getSelectedItemPosition());
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
                    Victim victim = new Victim(spinner.getSelectedItemPosition());
                    simulator = new Simulator(victim, this);
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
}
