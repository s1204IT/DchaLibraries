package org.apache.xml.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

class WriterToASCI extends Writer implements WriterChain {
    private final OutputStream m_os;

    public WriterToASCI(OutputStream os) {
        this.m_os = os;
    }

    @Override
    public void write(char[] chars, int start, int length) throws IOException {
        int n = length + start;
        for (int i = start; i < n; i++) {
            this.m_os.write(chars[i]);
        }
    }

    @Override
    public void write(int c) throws IOException {
        this.m_os.write(c);
    }

    @Override
    public void write(String s) throws IOException {
        int n = s.length();
        for (int i = 0; i < n; i++) {
            this.m_os.write(s.charAt(i));
        }
    }

    @Override
    public void flush() throws IOException {
        this.m_os.flush();
    }

    @Override
    public void close() throws IOException {
        this.m_os.close();
    }

    @Override
    public OutputStream getOutputStream() {
        return this.m_os;
    }

    @Override
    public Writer getWriter() {
        return null;
    }
}
