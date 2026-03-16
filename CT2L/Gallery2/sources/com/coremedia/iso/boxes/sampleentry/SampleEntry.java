package com.coremedia.iso.boxes.sampleentry;

import com.coremedia.iso.BoxParser;
import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;
import com.googlecode.mp4parser.AbstractBox;
import com.googlecode.mp4parser.util.ByteBufferByteChannel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class SampleEntry extends AbstractBox implements ContainerBox {
    private BoxParser boxParser;
    protected List<Box> boxes;
    private int dataReferenceIndex;

    protected SampleEntry(String type) {
        super(type);
        this.dataReferenceIndex = 1;
        this.boxes = new LinkedList();
    }

    public int getDataReferenceIndex() {
        return this.dataReferenceIndex;
    }

    @Override
    public List<Box> getBoxes() {
        return this.boxes;
    }

    @Override
    public <T extends Box> List<T> getBoxes(Class<T> clazz, boolean recursive) {
        ArrayList arrayList = new ArrayList(2);
        for (Box boxe : this.boxes) {
            if (clazz == boxe.getClass()) {
                arrayList.add(boxe);
            }
            if (recursive && (boxe instanceof ContainerBox)) {
                arrayList.addAll(((ContainerBox) boxe).getBoxes(clazz, recursive));
            }
        }
        return arrayList;
    }

    @Override
    public <T extends Box> List<T> getBoxes(Class<T> clazz) {
        return getBoxes(clazz, false);
    }

    @Override
    public void parse(ReadableByteChannel readableByteChannel, ByteBuffer header, long contentSize, BoxParser boxParser) throws IOException {
        this.boxParser = boxParser;
        super.parse(readableByteChannel, header, contentSize, boxParser);
    }

    public void _parseReservedAndDataReferenceIndex(ByteBuffer content) {
        content.get(new byte[6]);
        this.dataReferenceIndex = IsoTypeReader.readUInt16(content);
    }

    public void _parseChildBoxes(ByteBuffer content) {
        while (content.remaining() > 8) {
            try {
                this.boxes.add(this.boxParser.parseBox(new ByteBufferByteChannel(content), this));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        setDeadBytes(content.slice());
    }

    public void _writeReservedAndDataReferenceIndex(ByteBuffer bb) {
        bb.put(new byte[6]);
        IsoTypeWriter.writeUInt16(bb, this.dataReferenceIndex);
    }

    public void _writeChildBoxes(ByteBuffer bb) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WritableByteChannel wbc = Channels.newChannel(baos);
        try {
            for (Box box : this.boxes) {
                box.getBox(wbc);
            }
            wbc.close();
            bb.put(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Cannot happen. Everything should be in memory and therefore no exceptions.");
        }
    }
}
