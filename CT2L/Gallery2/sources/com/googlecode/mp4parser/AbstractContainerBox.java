package com.googlecode.mp4parser;

import com.coremedia.iso.BoxParser;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;
import com.googlecode.mp4parser.util.ByteBufferByteChannel;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public abstract class AbstractContainerBox extends AbstractBox implements ContainerBox {
    private static Logger LOG = Logger.getLogger(AbstractContainerBox.class.getName());
    protected BoxParser boxParser;
    protected List<Box> boxes;

    @Override
    protected long getContentSize() {
        long contentSize = 0;
        for (Box boxe : this.boxes) {
            contentSize += boxe.getSize();
        }
        return contentSize;
    }

    public AbstractContainerBox(String type) {
        super(type);
        this.boxes = new LinkedList();
    }

    @Override
    public List<Box> getBoxes() {
        return Collections.unmodifiableList(this.boxes);
    }

    @Override
    public <T extends Box> List<T> getBoxes(Class<T> clazz) {
        return getBoxes(clazz, false);
    }

    @Override
    public <T extends Box> List<T> getBoxes(Class<T> clazz, boolean recursive) {
        ArrayList arrayList = new ArrayList(2);
        for (Box boxe : this.boxes) {
            if (clazz.isInstance(boxe)) {
                arrayList.add(boxe);
            }
            if (recursive && (boxe instanceof ContainerBox)) {
                arrayList.addAll(((ContainerBox) boxe).getBoxes(clazz, recursive));
            }
        }
        return arrayList;
    }

    public void addBox(Box b) {
        b.setParent(this);
        this.boxes.add(b);
    }

    @Override
    public void parse(ReadableByteChannel readableByteChannel, ByteBuffer header, long contentSize, BoxParser boxParser) throws IOException {
        this.boxParser = boxParser;
        super.parse(readableByteChannel, header, contentSize, boxParser);
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseChildBoxes(content);
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(getClass().getSimpleName()).append("[");
        for (int i = 0; i < this.boxes.size(); i++) {
            if (i > 0) {
                buffer.append(";");
            }
            buffer.append(this.boxes.get(i).toString());
        }
        buffer.append("]");
        return buffer.toString();
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeChildBoxes(byteBuffer);
    }

    protected final void parseChildBoxes(ByteBuffer content) {
        while (content.remaining() >= 8) {
            try {
                this.boxes.add(this.boxParser.parseBox(new ByteBufferByteChannel(content), this));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (content.remaining() != 0) {
            setDeadBytes(content.slice());
            LOG.warning("Something's wrong with the sizes. There are dead bytes in a container box.");
        }
    }

    protected final void writeChildBoxes(ByteBuffer bb) {
        WritableByteChannel wbc = new ByteBufferByteChannel(bb);
        for (Box box : this.boxes) {
            try {
                box.getBox(wbc);
            } catch (IOException e) {
                throw new RuntimeException("Cannot happen to me", e);
            }
        }
    }
}
