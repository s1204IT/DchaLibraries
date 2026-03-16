package java.io;

public interface ObjectOutput extends DataOutput, AutoCloseable {
    @Override
    void close() throws IOException;

    void flush() throws IOException;

    @Override
    void write(int i) throws IOException;

    @Override
    void write(byte[] bArr) throws IOException;

    @Override
    void write(byte[] bArr, int i, int i2) throws IOException;

    void writeObject(Object obj) throws IOException;
}
