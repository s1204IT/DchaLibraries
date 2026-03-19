package mf.javax.xml.transform.stream;

import java.io.OutputStream;
import java.io.Writer;
import mf.javax.xml.transform.Result;

public class StreamResult implements Result {
    private OutputStream outputStream;
    private String systemId;
    private Writer writer;

    public OutputStream getOutputStream() {
        return this.outputStream;
    }

    public Writer getWriter() {
        return this.writer;
    }

    public String getSystemId() {
        return this.systemId;
    }
}
