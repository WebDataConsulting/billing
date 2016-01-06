package com.sapienter.jbilling.common;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * To defer invocation of toString, StringBuilder and message concatenation, a
 * fairly costly step from memory perspective.
 * 
 * @author Vikas Bodani
 * 
 */
public final class FormatLogger {

    private final Logger log;

    public FormatLogger(Logger logger) {
        this.log= logger;
    }

    public FormatLogger(Class clazz) {
    	this.log= Logger.getLogger(clazz);
    }

    public FormatLogger(Class c, Level level) {
        this.log = Logger.getLogger(c);
        this.log.setLevel(level);
    }

    /* debug method overloaded */
    public void debug(String s) {
        log.debug(s);
    }

    public void debug(Throwable e) {
        debug("Error: ", e);
    }

    public void debug(String s, Throwable e) {
        if (log.isEnabledFor(Level.DEBUG)) {
            log.debug(s, e);
        }
    }

    public void debug(Object o) {
        if (log.isEnabledFor(Level.DEBUG)) {
            log.debug(o);
        }
    }

    /* error method overloaded */
    public void error(Throwable e) {
        error("Error: ", e);
    }
    
    public void error(String s) {
    	if (log.isEnabledFor(Level.ERROR)) {
            log.error(s);
        }
    }

    public void error(String s, Throwable e) {
        if (log.isEnabledFor(Level.ERROR)) {
            log.error(s, e);
        }
    }
    
    public void warn(String s, Throwable e) {
        if (log.isEnabledFor(Level.WARN)) {
            log.error(s, e);
        }
    }
    
    public void fatal(String s, Throwable e) {
        if (log.isEnabledFor(Level.FATAL)) {
            log.error(s, e);
        }
    }

    public void setLogLevel(Level level) {
        log.setLevel(level);
    }

    public Level getLogLevel(){
        return log.getLevel();
    }
    
    /* formatter methods */
    
    public void debug(String formatter, Object... args) {
        log(Level.DEBUG, formatter, args);
    }

    public void info(String formatter, Object... args) {
        log(Level.INFO, formatter, args);
    }

    public void warn(String formatter, Object... args) {
        log(Level.WARN, formatter, args);
    }

    public void error(String formatter, Object... args) {
        log(Level.ERROR, formatter, args);
    }
    
    public void fatal(String formatter, Object... args) {
        log(Level.FATAL, formatter, args);
    }
    
    private void log(Level level, String formatter, Object... args) {
        if (log.isEnabledFor(level)) {
            /*
             * Only now is the message constructed, and each "arg" evaluated by
             * having its toString() method invoked.
             */
            log.log(level, String.format(formatter, args));
        }
    }

}
