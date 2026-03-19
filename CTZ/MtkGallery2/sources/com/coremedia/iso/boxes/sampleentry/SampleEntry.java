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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class SampleEntry extends AbstractBox implements ContainerBox {
    private BoxParser boxParser;
    protected List<Box> boxes;
    private int dataReferenceIndex;

    protected SampleEntry(String str) {
        super(str);
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
    public <T extends Box> List<T> getBoxes(Class<T> cls) {
        return getBoxes(cls, false);
    }

    @Override
    public void parse(ReadableByteChannel readableByteChannel, ByteBuffer byteBuffer, long j, BoxParser boxParser) throws IOException {
        this.boxParser = boxParser;
        super.parse(readableByteChannel, byteBuffer, j, boxParser);
    }

    public void _parseReservedAndDataReferenceIndex(ByteBuffer byteBuffer) {
        byteBuffer.get(new byte[6]);
        this.dataReferenceIndex = IsoTypeReader.readUInt16(byteBuffer);
    }

    public void _parseChildBoxes(ByteBuffer byteBuffer) {
        while (byteBuffer.remaining() > 8) {
            try {
                this.boxes.add(this.boxParser.parseBox(new ByteBufferByteChannel(byteBuffer), this));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        setDeadBytes(byteBuffer.slice());
    }

    public void _writeReservedAndDataReferenceIndex(ByteBuffer byteBuffer) {
        byteBuffer.put(new byte[6]);
        IsoTypeWriter.writeUInt16(byteBuffer, this.dataReferenceIndex);
    }

    public void _writeChildBoxes(ByteBuffer byteBuffer) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        WritableByteChannel writableByteChannelNewChannel = Channels.newChannel(byteArrayOutputStream);
        try {
            Iterator<Box> it = this.boxes.iterator();
            while (it.hasNext()) {
                it.next().getBox(writableByteChannelNewChannel);
            }
            writableByteChannelNewChannel.close();
            byteBuffer.put(byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Cannot happen. Everything should be in memory and therefore no exceptions.");
        }
    }
}
