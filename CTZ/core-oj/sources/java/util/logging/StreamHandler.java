package java.util.logging;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

public class StreamHandler extends Handler {
    private boolean doneHeader;
    private OutputStream output;
    private volatile Writer writer;

    private void configure() {
        LogManager logManager = LogManager.getLogManager();
        String name = getClass().getName();
        setLevel(logManager.getLevelProperty(name + ".level", Level.INFO));
        setFilter(logManager.getFilterProperty(name + ".filter", null));
        setFormatter(logManager.getFormatterProperty(name + ".formatter", new SimpleFormatter()));
        try {
            setEncoding(logManager.getStringProperty(name + ".encoding", null));
        } catch (Exception e) {
            try {
                setEncoding(null);
            } catch (Exception e2) {
            }
        }
    }

    public StreamHandler() {
        this.sealed = false;
        configure();
        this.sealed = true;
    }

    public StreamHandler(OutputStream outputStream, Formatter formatter) {
        this.sealed = false;
        configure();
        setFormatter(formatter);
        setOutputStream(outputStream);
        this.sealed = true;
    }

    protected synchronized void setOutputStream(OutputStream outputStream) throws SecurityException {
        if (outputStream == null) {
            throw new NullPointerException();
        }
        flushAndClose();
        this.output = outputStream;
        this.doneHeader = false;
        String encoding = getEncoding();
        if (encoding == null) {
            this.writer = new OutputStreamWriter(this.output);
        } else {
            try {
                this.writer = new OutputStreamWriter(this.output, encoding);
            } catch (UnsupportedEncodingException e) {
                throw new Error("Unexpected exception " + ((Object) e));
            }
        }
    }

    @Override
    public synchronized void setEncoding(String str) throws SecurityException, UnsupportedEncodingException {
        super.setEncoding(str);
        if (this.output == null) {
            return;
        }
        flush();
        if (str == null) {
            this.writer = new OutputStreamWriter(this.output);
        } else {
            this.writer = new OutputStreamWriter(this.output, str);
        }
    }

    @Override
    public synchronized void publish(LogRecord logRecord) {
        if (isLoggable(logRecord)) {
            try {
                String str = getFormatter().format(logRecord);
                try {
                    if (!this.doneHeader) {
                        this.writer.write(getFormatter().getHead(this));
                        this.doneHeader = true;
                    }
                    this.writer.write(str);
                } catch (Exception e) {
                    reportError(null, e, 1);
                }
            } catch (Exception e2) {
                reportError(null, e2, 5);
            }
        }
    }

    @Override
    public boolean isLoggable(LogRecord logRecord) {
        if (this.writer == null || logRecord == null) {
            return false;
        }
        return super.isLoggable(logRecord);
    }

    @Override
    public synchronized void flush() {
        if (this.writer != null) {
            try {
                this.writer.flush();
            } catch (Exception e) {
                reportError(null, e, 2);
            }
        }
    }

    private synchronized void flushAndClose() throws SecurityException {
        checkPermission();
        if (this.writer != null) {
            try {
                if (!this.doneHeader) {
                    this.writer.write(getFormatter().getHead(this));
                    this.doneHeader = true;
                }
                this.writer.write(getFormatter().getTail(this));
                this.writer.flush();
                this.writer.close();
            } catch (Exception e) {
                reportError(null, e, 3);
            }
            this.writer = null;
            this.output = null;
        }
    }

    @Override
    public synchronized void close() throws SecurityException {
        flushAndClose();
    }
}
