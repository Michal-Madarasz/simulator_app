package com.triage.model;

import java.io.Serializable;

public class Rescuer implements Serializable {
    private static final long serialVersionUID = 18636221345311L;

    private String id;
    private String name;

    public Rescuer() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
