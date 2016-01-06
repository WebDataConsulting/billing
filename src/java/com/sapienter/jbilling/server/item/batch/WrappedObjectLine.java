package com.sapienter.jbilling.server.item.batch;

/**
 * The object is used when importing flat files. It contains the original line and line number
 * as well as the mapped object.
 *
 * @see WrappingLineMapper
 *
 * @author Gerhard
 * @since 03/05/13
 */
public class WrappedObjectLine<T> {

    /** line number in import file */
    int lineNr;
    /** original line */
    String line;
    /** mapped object */
    T object;

    public WrappedObjectLine(int lineNr, String line, T object) {
        this.lineNr = lineNr;
        this.line = line;
        this.object = object;
    }

    public int getLineNr() {
        return lineNr;
    }

    public void setLineNr(int lineNr) {
        this.lineNr = lineNr;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public T getObject() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }
}
