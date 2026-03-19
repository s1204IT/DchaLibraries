package java.util;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class InvalidPropertiesFormatException extends IOException {
    private static final long serialVersionUID = 7763056076009360219L;

    public InvalidPropertiesFormatException(Throwable th) {
        super(th == null ? null : th.toString());
        initCause(th);
    }

    public InvalidPropertiesFormatException(String str) {
        super(str);
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws NotSerializableException {
        throw new NotSerializableException("Not serializable.");
    }

    private void readObject(ObjectInputStream objectInputStream) throws NotSerializableException {
        throw new NotSerializableException("Not serializable.");
    }
}
