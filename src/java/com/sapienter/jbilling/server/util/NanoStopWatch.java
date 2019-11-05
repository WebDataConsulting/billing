/**
 * 
 */
package com.sapienter.jbilling.server.util;

/*
 *  Copyright (c) 2011, Carlos Quintanilla
 *  Special thanks to Corey Goldberg (for his StopWatch.java | Java Timer Class)
 *  here http://www.goldb.org/stopwatchjava.html
 * 
 *  Stopwatch.java is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 */

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Calendar;

/**
 * @author Carlos Quintanilla, Vikas Bodani
 */

public class NanoStopWatch {
    
    // constants
    private final long nsPerTick = 100;
    private final long nsPerMs = 1000000;
    private final long nsPerSs = 1000000000;
    private final long nsPerMm = 60000000000L;
    private final long nsPerHh = 3600000000000L;

    private long startTime = 0;
    private long stopTime = 0;
    private boolean running = false;
    
    private String name=null;
    
    public NanoStopWatch(String name) {
        this.name= name;
    }
    
    public NanoStopWatch() {
    }

    
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Starts measuring elapsed time
     * for an interval.
     */
    public void start() {
        this.startTime = System.nanoTime();       
        this.running = true;
    }
    
    /**
     * Stops measuring elapsed time
     * for an interval.
     */
    public void stop() {
        this.stopTime = System.nanoTime();
        this.running = false;
    }
    
    /**
     * Stops time interval measurement 
     * and resets the elapsed time to zero.
     */ 
    public void reset() {
        this.startTime = 0;
        this.stopTime = 0;
        this.running = false;
    }
    
    /**
     * Gets the total elapsed time measured 
     * by the current instance, in nanoseconds.
     * 1 Tick = 100 nanoseconds 
     */
    public long getElapsedTicks() {
        long elapsed;
        if (running) {
             elapsed = (System.nanoTime() - startTime);
        }
        else {
            elapsed = (stopTime - startTime);
        }
        return elapsed / nsPerTick;
    }
    
    /**
     * Gets the total elapsed time measured 
     * by the current instance, in milliseconds.
     * 10000 Ticks = 1 millisecond (1000000 nanoseconds)
     */
    public long getElapsedMilliseconds() {
        long elapsed;
        if (running) {
             elapsed = (System.nanoTime() - startTime);
        }
        else {
            elapsed = (stopTime - startTime);
        }
        return elapsed / nsPerMs;
    }
    
    /**
     * Gets the total elapsed time measured 
     * by the current instance, in seconds.
     * 10000000 Ticks = 1 second (1000 milliseconds)
     */
    public long getElapsedSeconds() {
        long elapsed;
        if (running) {
             elapsed = (System.nanoTime() - startTime);
        }
        else {
            elapsed = (stopTime - startTime);
        }        
        return elapsed / nsPerSs;
    }
    
    /**
     * Gets the total elapsed time measured 
     * by the current instance, in minutes.
     * 600000000 Ticks = 1 minute (60 seconds)
     */
    public long getElapsedMinutes() {
        long elapsed;
        if (running) {
             elapsed = (System.nanoTime() - startTime);
        }
        else {
            elapsed = (stopTime - startTime);
        }        
        return elapsed / nsPerMm;
    }
    
    /**
     * Gets the total elapsed time measured 
     * by the current instance, in hours.
     * 36000000000 Ticks = 1 hour (60 minutes)
     */
    public long getElapsedHours() {
        long elapsed;
        if (running) {
             elapsed = (System.nanoTime() - startTime);
        }
        else {
            elapsed = (stopTime - startTime);
        }        
        return elapsed / nsPerHh;
    }
    
    /**
     * Gets the total elapsed time with format 
     * 00:00:00.0000000 = 00:mm:ss.SSS + 9999 Ticks
     */ 
    public String getElapsed() {
        String timeFormatted = "";
        timeFormatted = this.formatTime(this.getElapsedTicks());        
        return name + " execution time: " + timeFormatted;
    }
    
    /**
     * Gets the total elapsed time with format 
     * 00:00:00.0000000 = 00:mm:ss.SSS + #### Ticks
     * @param elapsedTicks elapsed ticks between start and stop nano time
     */ 
    private String formatTime(final long elapsedTicks) {        
        String formattedTime = "";
        // should be hh:mm:ss.SSS, but 00 starts with 01 
        DateTimeFormatter formatter = DateTimeFormat.forPattern("00:mm:ss.SSS");
        Calendar calendar = Calendar.getInstance();        
        
        if (elapsedTicks <= 9999) {
            calendar.setTimeInMillis(0);
            formattedTime = formatter.print(calendar.getTime().getTime())
                    + String.valueOf(String.format("%04d", elapsedTicks));
        }
        else {
            calendar.setTimeInMillis(elapsedTicks * nsPerTick / nsPerMs);            
            String formattedTicks = String.format("%07d", elapsedTicks);
            formattedTicks = formattedTicks.substring(formattedTicks.length() - 4);
            formattedTime = formatter.print(calendar.getTime().getTime()) + formattedTicks;
        }
        return formattedTime;
    }
}
