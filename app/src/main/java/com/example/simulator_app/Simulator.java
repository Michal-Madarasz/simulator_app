package com.example.simulator_app;

import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.triage.model.Victim;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class Simulator extends Thread{

    public enum Lifeline { GOOD, STABLE, HURT, DYING, DEAD}

    private final int timeStep = 3000;

    private Victim victim;
    private Lifeline lifeline;
    private MainActivity activity;
    volatile boolean alive = false;
    private boolean paused = false;
    private final Object pauseLock = new Object();

    ArrayList<String[]> statesList = new ArrayList<String[]>();

    public Simulator(Victim victim, MainActivity activity) {
        this.victim = victim;
        this.activity = activity;
        lifeline = Lifeline.GOOD;
    }

    public Simulator(Lifeline lifeline, MainActivity activity) {
        this.lifeline = lifeline;
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
        while (alive) {
            for(String[] state : statesList) {

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
                        victim.setVictim(state);
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
    }

    private void advanceState(){
        Random r = new Random();
        switch(lifeline){
            case GOOD:
                break;
            case STABLE:
                break;
            case HURT:
                break;
            case DYING:
                break;
            default:
        }
        float noise = (float)r.nextGaussian()/10;
        victim.setRespiratoryRate(victim.getRespiratoryRate()+(victim.getRespiratoryRate()*noise));
        victim.setCapillaryRefillTime(r.nextFloat()*3);
        victim.setWalking(r.nextFloat()>0.8);
        if(victim.getRespiratoryRate()<=0){
            victim.setBreathing(false);
            victim.setWalking(false);
            victim.setRespiratoryRate(0);
            victim.setConsciousness(Victim.AVPU.UNRESPONSIVE);
        }
        victim.calculateColor();
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

    void setStatesList(ArrayList<String[]> statesList)
    {
        this.statesList=statesList;
    }

}
