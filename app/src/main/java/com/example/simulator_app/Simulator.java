package com.example.simulator_app;

import android.os.Environment;
import android.widget.Toast;

import com.triage.model.Victim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class Simulator extends Thread{

    public enum Lifeline { GOOD, STABLE, HURT, DYING, DEAD}

    private final int timeStep = 1000;

    private Victim victim;
    private File lifelineSource;
    private MainActivity activity;
    volatile boolean alive = false;
    private boolean paused = false;
    private final Object pauseLock = new Object();

    public Simulator(Victim victim, MainActivity activity) {
        this.victim = victim;
        this.activity = activity;
    }

    public Simulator(MainActivity activity) {
        boolean breathing = true;
        float respiratoryRate = 20;
        float capillaryRefillTime = 1.5f;
        boolean walking = true;
        Victim.AVPU consciousness = Victim.AVPU.AWAKE;
        this.victim = new Victim(breathing, respiratoryRate, capillaryRefillTime, walking, consciousness);
        this.activity = activity;
    }



    @Override
    public void run() {
        BufferedReader lifeline = openLifeline();
        while (alive) {
            synchronized (pauseLock) {
                if (!alive) { // may have changed while waiting to
                    // synchronize on pauseLock
                    break;
                }
                if (paused) {
                    try {
                        synchronized (pauseLock) {
                            pauseLock.wait(); // will cause this Thread to block until
                            // another thread calls pauseLock.notifyAll()
                            // Note that calling wait() will
                            // relinquish the synchronized lock that this
                            // thread holds on pauseLock so another thread
                            // can acquire the lock to call notifyAll()
                            // (link with explanation below this code)
                        }
                    } catch (InterruptedException ex) {
                        break;
                    }
                    if (!alive) { // running might have changed since we paused
                        break;
                    }
                }
            }
            try { //this is where the simulation happens
                sleep(timeStep);
                try{
                    String line;
                    if((line = lifeline.readLine()) != null){
                        String[] state = line.split(";");
                        victim.setVictim(state);
                    }else{//open file again
                        lifeline.close();
                        lifeline = openLifeline();
                    }
                }catch (Exception e){
                    // TODO: error message
                }
                //advanceState();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            activity.transferPayload(victim);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.actualize(victim);
                }
            });
        }
    }

    void setLifelineSource(String filename){
        lifelineSource = new File(Environment.getExternalStorageDirectory(), "/life_lines/"+filename);

    }

    private BufferedReader openLifeline(){
        try {
            return new BufferedReader(new FileReader(lifelineSource));
        }
        catch(Exception e) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity.getApplicationContext(), "Błąd odczytu pliku. Plik mógł zostać usunięty. Przerwanie symulacji.", Toast.LENGTH_SHORT ).show();
                }
            });
            alive = false;
        }
        return null;
    }

    @Override
    public void start(){
        if(alive)
            return;
        alive = true;

        super.start();
    }

    public void kill(){
        alive = false;
    }

    public void pause(){
        paused = true;
    }

    public void unpause(){
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll(); // Unblocks thread
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public Victim getVictim() {
        return victim;
    }

}
