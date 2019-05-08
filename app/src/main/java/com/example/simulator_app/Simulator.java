package com.example.simulator_app;

import android.app.Activity;
import android.widget.TextView;

import com.example.triage.Victim;

import java.util.Random;

public class Simulator extends Thread{

    Victim victim;
    MainActivity activity;
    volatile boolean alive = false;
    private boolean paused = false;
    private final Object pauseLock = new Object();

    public Simulator(Victim victim, MainActivity activity) {
        this.victim = victim;
        this.activity = activity;
    }

    @Override
    public void run() {
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
                sleep(1000);
                Random rand = new Random();
                victim = new Victim(rand.nextInt(5)); //placeholder for testing
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.actualize(victim);
                }
            });
        }
    }

    @Override
    public void start(){
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

}
