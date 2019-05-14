package com.triage.model;

import java.io.Serializable;

//klasa reprezentująca poszkodowanego rozszerzona o
//interfejs pozwalający na przesyłanie obiektu między aktywnościami
public class Victim implements Serializable {
    private static final long serialVersionUID = 186362213453111235L;

    private static int totalID = 0;

    private boolean changingState;
    private long id;
    private boolean breathing;
    private int respiratoryRate;
    private float capillaryRefillTime;
    private boolean walking;
    private TriageColor color;
    private AVPU consciousness;

    public Victim(boolean breathing, int respiratoryRate, float capillaryRefillTime, boolean walking, AVPU consciousness) {
        this.id = totalID; totalID++;
        this.breathing = breathing;
        this.respiratoryRate = respiratoryRate;
        this.capillaryRefillTime = capillaryRefillTime;
        this.walking = walking;
        this.consciousness = consciousness;
        calculateColor();
    }

    public Victim() {
        this.id = totalID; totalID++;
        this.breathing = true;
        this.respiratoryRate = 20;
        this.capillaryRefillTime = 1.5f;
        this.walking = true;
        this.consciousness = AVPU.AWAKE;
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
