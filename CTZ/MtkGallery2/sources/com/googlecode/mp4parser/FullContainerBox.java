package com.googlecode.mp4parser;

import com.coremedia.iso.BoxParser;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;
import com.googlecode.mp4parser.util.ByteBufferByteChannel;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public abstract class FullContainerBox extends AbstractFullBox implements ContainerBox {
    private static Logger LOG = Logger.getLogger(FullContainerBox.class.getName());
    BoxParser boxParser;
    protected List<Box> boxes;

    @Override
    public <T extends Box> List<T> getBoxes(Class<T> cls) {
        return getBoxes(cls, false);
    }

    @Override
    public <T extends Box> List<T> getBoxes(Class<T> cls, boolean z) {
        ArrayList arrayList = new ArrayList(2);
        for (Box box : this.boxes) {
            if (cls == box.getClass()) {
                arrayList.add(box);
            }
            if (z && (box instanceof ContainerBox)) {
                arrayList.addAll(((ContainerBox) box).getBoxes(cls, z));
            }
        }
        return arrayList;
    }

    @Override
    protected long getContentSize() {
        Iterator<Box> it = this.boxes.iterator();
        long size = 4;
        while (it.hasNext()) {
            size += it.next().getSize();
        }
        return size;
    }

    public void addBox(Box box) {
        box.setParent(this);
        this.boxes.add(box);
    }

    public FullContainerBox(String str) {
        super(str);
        this.boxes = new LinkedList();
    }

    @Override
    public List<Box> getBoxes() {
        return this.boxes;
    }

    @Override
    public void parse(ReadableByteChannel readableByteChannel, ByteBuffer byteBuffer, long j, BoxParser boxParser) throws IOException {
        this.boxParser = boxParser;
        super.parse(readableByteChannel, byteBuffer, j, boxParser);
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        parseChildBoxes(byteBuffer);
    }

    protected final void parseChildBoxes(ByteBuffer byteBuffer) {
        while (byteBuffer.remaining() >= 8) {
            try {
                this.boxes.add(this.boxParser.parseBox(new ByteBufferByteChannel(byteBuffer), this));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (byteBuffer.remaining() != 0) {
            setDeadBytes(byteBuffer.slice());
            LOG.severe("Some sizes are wrong");
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("[");
        for (int i = 0; i < this.boxes.size(); i++) {
            if (i > 0) {
                sb.append(";");
            }
            sb.append(this.boxes.get(i).toString());
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        writeChildBoxes(byteBuffer);
    }

    protected final void writeChildBoxes(ByteBuffer byteBuffer) {
        ByteBufferByteChannel byteBufferByteChannel = new ByteBufferByteChannel(byteBuffer);
        Iterator<Box> it = this.boxes.iterator();
        while (it.hasNext()) {
            try {
                it.next().getBox(byteBufferByteChannel);
            } catch (IOException e) {
                throw new RuntimeException("Cannot happen.", e);
            }
        }
    }
}
