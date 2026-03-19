package org.apache.commons.logging.impl;

import java.io.Serializable;
import org.apache.commons.logging.Log;

@Deprecated
public class NoOpLog implements Log, Serializable {
    public NoOpLog() {
    }

    public NoOpLog(String str) {
    }

    @Override
    public void trace(Object obj) {
    }

    @Override
    public void trace(Object obj, Throwable th) {
    }

    @Override
    public void debug(Object obj) {
    }

    @Override
    public void debug(Object obj, Throwable th) {
    }

    @Override
    public void info(Object obj) {
    }

    @Override
    public void info(Object obj, Throwable th) {
    }

    @Override
    public void warn(Object obj) {
    }

    @Override
    public void warn(Object obj, Throwable th) {
    }

    @Override
    public void error(Object obj) {
    }

    @Override
    public void error(Object obj, Throwable th) {
    }

    @Override
    public void fatal(Object obj) {
    }

    @Override
    public void fatal(Object obj, Throwable th) {
    }

    @Override
    public final boolean isDebugEnabled() {
        return false;
    }

    @Override
    public final boolean isErrorEnabled() {
        return false;
    }

    @Override
    public final boolean isFatalEnabled() {
        return false;
    }

    @Override
    public final boolean isInfoEnabled() {
        return false;
    }

    @Override
    public final boolean isTraceEnabled() {
        return false;
    }

    @Override
    public final boolean isWarnEnabled() {
        return false;
    }
}
