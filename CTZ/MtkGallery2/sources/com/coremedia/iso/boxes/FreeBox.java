package com.coremedia.iso.boxes;

import com.coremedia.iso.BoxParser;
import com.coremedia.iso.ChannelHelper;
import com.coremedia.iso.IsoTypeWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class FreeBox implements Box {
    static final boolean $assertionsDisabled = false;
    ByteBuffer data;
    private ContainerBox parent;
    List<Box> replacers = new LinkedList();

    @Override
    public void getBox(WritableByteChannel writableByteChannel) throws IOException {
        Iterator<Box> it = this.replacers.iterator();
        while (it.hasNext()) {
            it.next().getBox(writableByteChannel);
        }
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(8);
        IsoTypeWriter.writeUInt32(byteBufferAllocate, 8 + this.data.limit());
        byteBufferAllocate.put("free".getBytes());
        byteBufferAllocate.rewind();
        writableByteChannel.write(byteBufferAllocate);
        this.data.rewind();
        writableByteChannel.write(this.data);
    }

    @Override
    public ContainerBox getParent() {
        return this.parent;
    }

    @Override
    public void setParent(ContainerBox containerBox) {
        this.parent = containerBox;
    }

    @Override
    public long getSize() {
        Iterator<Box> it = this.replacers.iterator();
        long size = 8;
        while (it.hasNext()) {
            size += it.next().getSize();
        }
        return size + ((long) this.data.limit());
    }

    @Override
    public String getType() {
        return "free";
    }

    @Override
    public void parse(ReadableByteChannel readableByteChannel, ByteBuffer byteBuffer, long j, BoxParser boxParser) throws IOException {
        if ((readableByteChannel instanceof FileChannel) && j > 1048576) {
            this.data = readableByteChannel.map(FileChannel.MapMode.READ_ONLY, readableByteChannel.position(), j);
            readableByteChannel.position(readableByteChannel.position() + j);
        } else {
            this.data = ChannelHelper.readFully(readableByteChannel, j);
        }
    }
}
