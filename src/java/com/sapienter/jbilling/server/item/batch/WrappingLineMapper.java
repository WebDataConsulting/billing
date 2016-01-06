package com.sapienter.jbilling.server.item.batch;

import org.springframework.batch.item.file.LineMapper;

/**
 * Asks a delegate to map the line into an object and wraps the response in a
 * WrappedObjectLine object which contains the original line and line number.
 *
 * @see WrappedObjectLine
 *
 * @author Gerhard
 * @since 03/05/13
 */
public class WrappingLineMapper<T> implements LineMapper<WrappedObjectLine<T>> {

    /** Line Mapper which will do the transformation */
    private LineMapper<T> delegate;

    @Override
    public WrappedObjectLine mapLine(String s, int i) throws Exception {
        return new WrappedObjectLine(i, s, delegate.mapLine(s, i));
    }

    public LineMapper<T> getDelegate() {
        return delegate;
    }

    public void setDelegate(LineMapper<T> delegate) {
        this.delegate = delegate;
    }
}
