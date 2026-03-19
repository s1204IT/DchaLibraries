package com.coremedia.iso.boxes.apple;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.Utf8;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;
import com.googlecode.mp4parser.AbstractBox;
import com.googlecode.mp4parser.util.ByteBufferByteChannel;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public abstract class AbstractAppleMetaDataBox extends AbstractBox implements ContainerBox {
    static final boolean $assertionsDisabled = false;
    private static Logger LOG = Logger.getLogger(AbstractAppleMetaDataBox.class.getName());
    AppleDataBox appleDataBox;

    @Override
    public List<Box> getBoxes() {
        return Collections.singletonList(this.appleDataBox);
    }

    @Override
    public <T extends Box> List<T> getBoxes(Class<T> cls) {
        return getBoxes(cls, false);
    }

    @Override
    public <T extends Box> List<T> getBoxes(Class<T> cls, boolean z) {
        if (cls.isAssignableFrom(this.appleDataBox.getClass())) {
            return Collections.singletonList(this.appleDataBox);
        }
        return null;
    }

    public AbstractAppleMetaDataBox(String str) {
        super(str);
        this.appleDataBox = new AppleDataBox();
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        IsoTypeReader.readUInt32(byteBuffer);
        IsoTypeReader.read4cc(byteBuffer);
        this.appleDataBox = new AppleDataBox();
        try {
            this.appleDataBox.parse(new ByteBufferByteChannel(byteBuffer), null, byteBuffer.remaining(), null);
            this.appleDataBox.setParent(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected long getContentSize() {
        return this.appleDataBox.getSize();
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        try {
            this.appleDataBox.getBox(new ByteBufferByteChannel(byteBuffer));
        } catch (IOException e) {
            throw new RuntimeException("The Channel is based on a ByteBuffer and therefore it shouldn't throw any exception");
        }
    }

    public String toString() {
        return getClass().getSimpleName() + "{appleDataBox=" + getValue() + '}';
    }

    static long toLong(byte b) {
        int i = b;
        if (b < 0) {
            i = b + 256;
        }
        return i;
    }

    public String getValue() {
        int i = 1;
        if (this.appleDataBox.getFlags() == 1) {
            return Utf8.convert(this.appleDataBox.getData());
        }
        int i2 = 0;
        if (this.appleDataBox.getFlags() == 21) {
            byte[] data = this.appleDataBox.getData();
            long j = 0;
            int length = data.length;
            int length2 = data.length;
            while (i2 < length2) {
                j += toLong(data[i2]) << (8 * (length - i));
                i2++;
                i++;
            }
            return "" + j;
        }
        if (this.appleDataBox.getFlags() == 0) {
            return String.format("%x", new BigInteger(this.appleDataBox.getData()));
        }
        return "unknown";
    }
}
