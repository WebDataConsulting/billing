package com.sapienter.jbilling.server.process.db;

public enum ProratingType {
    PRORATING_AUTO_ON("PRORATING_AUTO_ON"),
    PRORATING_AUTO_OFF("PRORATING_AUTO_OFF"),
    PRORATING_MANUAL("PRORATING_MANUAL");
    
    private final String proratingType;
    
    ProratingType(String proratingType) {
    	this.proratingType = proratingType; 
	}
    
    public String getProratingType() {
    	return this.proratingType;
    }
    
    public String getOptionText() {
    	return this.proratingType;
    }
    
    public boolean isProratingAutoOn() {
    	return this.proratingType.equals(PRORATING_AUTO_ON.getOptionText());
    }
    
    public boolean isProratingAutoOff() {
    	return this.proratingType.equals(PRORATING_AUTO_OFF.getOptionText());
    }
    
    public boolean isProratingManual() {
    	return this.proratingType.equals(PRORATING_MANUAL.getOptionText());
    }
    
    public static ProratingType getProratingTypeByOptionText(String proratingType) {
    	for (ProratingType type : values()) {
    		if (null != proratingType && proratingType.equals(type.getOptionText())) {
    			return type;
    		}
    	}
    	
    	return null;
    }
}
