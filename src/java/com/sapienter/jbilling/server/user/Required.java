package com.sapienter.jbilling.server.user;

public enum Required {
    YES("Yes", true), NO("No", false);

    final String name;
    final Boolean id;

    Required(String name, Boolean id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public Boolean getId() {
        return this.id;
    }

    }