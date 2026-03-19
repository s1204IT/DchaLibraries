package org.apache.xml.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

class WriterToASCI extends Writer implements WriterChain {
    private final OutputStream m_os;

    public WriterToASCI(OutputStream outputStream) {
        this.m_os = outputStream;
    }

    @Override
    public void write(char[] cArr, int i, int i2) throws IOException {
        int i3 = i2 + i;
        while (i < i3) {
            this.m_os.write(cArr[i]);
            i++;
        }
    }

    @Override
    public void write(int i) throws IOException {
        this.m_os.write(i);
    }

    @Override
    public void write(String str) throws IOException {
        int length = str.length();
        for (int i = 0; i < length; i++) {
            this.m_os.write(str.charAt(i));
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
