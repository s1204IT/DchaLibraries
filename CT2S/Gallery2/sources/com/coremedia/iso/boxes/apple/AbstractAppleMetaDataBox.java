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
    static final boolean $assertionsDisabled;
    private static Logger LOG;
    AppleDataBox appleDataBox;

    static {
        $assertionsDisabled = !AbstractAppleMetaDataBox.class.desiredAssertionStatus();
        LOG = Logger.getLogger(AbstractAppleMetaDataBox.class.getName());
    }

    @Override
    public List<Box> getBoxes() {
        return Collections.singletonList(this.appleDataBox);
    }

    @Override
    public <T extends Box> List<T> getBoxes(Class<T> clazz) {
        return getBoxes(clazz, false);
    }

    @Override
    public <T extends Box> List<T> getBoxes(Class<T> clazz, boolean recursive) {
        if (clazz.isAssignableFrom(this.appleDataBox.getClass())) {
            return Collections.singletonList(this.appleDataBox);
        }
        return null;
    }

    public AbstractAppleMetaDataBox(String type) {
        super(type);
        this.appleDataBox = new AppleDataBox();
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        IsoTypeReader.readUInt32(content);
        String thisShouldBeData = IsoTypeReader.read4cc(content);
        if (!$assertionsDisabled && !"data".equals(thisShouldBeData)) {
            throw new AssertionError();
        }
        this.appleDataBox = new AppleDataBox();
        try {
            this.appleDataBox.parse(new ByteBufferByteChannel(content), null, content.remaining(), null);
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
        return b < 0 ? b + 256 : b;
    }

    public String getValue() {
        if (this.appleDataBox.getFlags() == 1) {
            return Utf8.convert(this.appleDataBox.getData());
        }
        if (this.appleDataBox.getFlags() == 21) {
            byte[] content = this.appleDataBox.getData();
            long l = 0;
            int length = content.length;
            int len$ = content.length;
            int i$ = 0;
            int current = 1;
            while (i$ < len$) {
                byte b = content[i$];
                l += toLong(b) << ((length - current) * 8);
                i$++;
                current++;
            }
            return "" + l;
        }
        if (this.appleDataBox.getFlags() == 0) {
            return String.format("%x", new BigInteger(this.appleDataBox.getData()));
        }
        return "unknown";
    }
}
