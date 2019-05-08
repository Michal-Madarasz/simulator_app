package com.example.triage;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.widget.Spinner;

import com.example.simulator_app.MainActivity;
import com.example.simulator_app.R;

import java.io.Serializable;

//klasa reprezentująca poszkodowanego rozszerzona o
//interfejs pozwalający na przesyłanie obiektu między aktywnościami
public class Victim implements Serializable {

    private boolean changingState;
    private int lifeline;
    private long transmitterIMEI;
    private boolean breathing;
    private int respiratoryRate;
    private float capillaryRefillTime;
    private boolean walking;
    private TriageColor color;
    private AVPU consciousness;

    public Victim(long transmitterIMEI, boolean breathing, int respiratoryRate, float capillaryRefillTime, boolean walking, AVPU consciousness) {
        this.transmitterIMEI = transmitterIMEI;
        this.breathing = breathing;
        this.respiratoryRate = respiratoryRate;
        this.capillaryRefillTime = capillaryRefillTime;
        this.walking = walking;
        this.consciousness = consciousness;
        calculateColor();
    }

    public Victim( int choice ) {

        lifeline = choice;
        setParameters();
    }

    public Victim() {
        lifeline = 0;
        setParameters();
    }

    public void setParameters() {

        switch (lifeline) {
            case 0:
                setYellow();
                break;
            case 1:
                setGreen();
                break;
            case 2:
                setRed();
                break;
            default:
                setYellow();
                break;
        }

    }

    public void setGreen() {
        walking=true;
        breathing=true;
        respiratoryRate=25;
        capillaryRefillTime=1;
        consciousness=AVPU.VERBAL;
        calculateColor();
    }

    public void setYellow() {
        breathing=true;
        respiratoryRate=25;
        capillaryRefillTime=1;
        walking=false;
        consciousness=AVPU.AWAKE;
        calculateColor();
    }

    public void setRed() {
        walking=false;
        breathing=true;
        respiratoryRate=35;
        capillaryRefillTime=1;
        consciousness=AVPU.VERBAL;
        calculateColor();
    }

    public void calculateColor() {
        if(walking){
            color = TriageColor.GREEN;
            return;
        } else {
            if(!breathing){
                color = TriageColor.BLACK;
                return;
            } else {
                if(respiratoryRate>30){
                    color = TriageColor.RED;
                    return;
                } else {
                    if(capillaryRefillTime>2){
                        color = TriageColor.RED;
                        return;
                    } else {
                        if(consciousness== AVPU.PAIN || consciousness== AVPU.UNRESPONSIVE){
                            color = TriageColor.RED;
                            return;
                        } else {
                            color = TriageColor.YELLOW;
                        }
                    }
                }
            }
        }
    }

    public long getTransmitterIMEI() {
        return transmitterIMEI;
    }

    public void setTransmitterIMEI(long transmitterIMEI) {
        this.transmitterIMEI = transmitterIMEI;
    }

    public boolean isBreathing() {
        return breathing;
    }

    public void setBreathing(boolean breathing) {
        this.breathing = breathing;
    }

    public int getRespiratoryRate() {
        return respiratoryRate;
    }

    public void setRespiratoryRate(int respiratoryRate) {
        this.respiratoryRate = respiratoryRate;
    }

    public boolean isWalking() {
        return walking;
    }

    public void setWalking(boolean walking) {
        this.walking = walking;
    }

    public TriageColor getColor() {
        return color;
    }

    public void setColor(TriageColor color) {
        this.color = color;
    }

    public AVPU getConsciousness() {
        return consciousness;
    }

    public void setConsciousness(AVPU consciousness) {
        this.consciousness = consciousness;
    }

    public float getCapillaryRefillTime() {
        return capillaryRefillTime;
    }

    public void setCapillaryRefillTime(float capillaryRefillTime) {
        this.capillaryRefillTime = capillaryRefillTime;
    }

    public enum TriageColor {BLACK, RED, YELLOW, GREEN}

    public enum AVPU {AWAKE, VERBAL, PAIN, UNRESPONSIVE}

    public void stopChangingState() {
        changingState = false;
    }

    public boolean getChangingState() {
        return changingState;
    }
}
