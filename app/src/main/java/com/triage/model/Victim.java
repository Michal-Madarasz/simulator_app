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
    private float respiratoryRate;
    private float capillaryRefillTime;
    private boolean walking;
    private TriageColor color;
    private AVPU consciousness;

    public Victim(boolean breathing, float respiratoryRate, float capillaryRefillTime, boolean walking, AVPU consciousness){
        this.id = totalID; totalID++;
        this.breathing = breathing;
        this.respiratoryRate = respiratoryRate;
        this.capillaryRefillTime = capillaryRefillTime;
        this.walking = walking;
        this.consciousness = consciousness;
        this.color = null;
    }

    public Victim() {
        this.id = totalID; totalID++;
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

    public float getRespiratoryRate() {
        return respiratoryRate;
    }

    public void setRespiratoryRate(float respiratoryRate) {
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

    // order: breathing, respiratoryRate, capillaryRefillTime, walking, consciousness
    public void setVictim(String[] data) throws Exception {
        this.id = totalID; totalID++;

        if(data.length!=5)
        {
            throw new Exception();
        }

        if(data[0].equals("true")) breathing = true;
        else if(data[0].equals("false")) breathing = false;
        else throw new Exception();

        respiratoryRate = Float.parseFloat(data[1]);

        capillaryRefillTime = Float.parseFloat(data[2]);


        if(data[3].equals("true")) walking = true;
        else if(data[3].equals("false")) walking = false;
        else throw new Exception();

        if(data[4].equals("AWAKE")) consciousness = AVPU.AWAKE;
        else if(data[4].equals("PAIN")) consciousness = AVPU.PAIN;
        else if(data[4].equals("VERBAL")) consciousness = AVPU.VERBAL;
        else if(data[4].equals("UNRESPONSIVE")) consciousness = AVPU.UNRESPONSIVE;
        else throw new Exception();

    }
}
