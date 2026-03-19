package com.coremedia.iso.boxes.mdat;

import com.coremedia.iso.BoxParser;
import com.coremedia.iso.ChannelHelper;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;
import com.googlecode.mp4parser.AbstractBox;
import com.googlecode.mp4parser.util.CastUtils;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class MediaDataBox implements Box {
    static final boolean $assertionsDisabled = false;
    private static Logger LOG = Logger.getLogger(MediaDataBox.class.getName());
    private Map<Long, Reference<ByteBuffer>> cache = new HashMap();
    private ByteBuffer content;
    private long contentSize;
    private FileChannel fileChannel;
    ByteBuffer header;
    ContainerBox parent;
    private long startPosition;

    @Override
    public ContainerBox getParent() {
        return this.parent;
    }

    @Override
    public void setParent(ContainerBox containerBox) {
        this.parent = containerBox;
    }

    @Override
    public String getType() {
        return "mdat";
    }

    private static void transfer(FileChannel fileChannel, long j, long j2, WritableByteChannel writableByteChannel) throws IOException {
        long jTransferTo = 0;
        while (jTransferTo < j2) {
            jTransferTo += fileChannel.transferTo(j + jTransferTo, Math.min(67076096L, j2 - jTransferTo), writableByteChannel);
        }
    }

    @Override
    public void getBox(WritableByteChannel writableByteChannel) throws IOException {
        if (this.fileChannel != null) {
            transfer(this.fileChannel, this.startPosition - ((long) this.header.limit()), this.contentSize + ((long) this.header.limit()), writableByteChannel);
            return;
        }
        this.header.rewind();
        writableByteChannel.write(this.header);
        writableByteChannel.write(this.content);
    }

    private boolean checkStillOk() {
        try {
            this.fileChannel.position(this.startPosition - ((long) this.header.limit()));
            ByteBuffer byteBufferAllocate = ByteBuffer.allocate(this.header.limit());
            this.fileChannel.read(byteBufferAllocate);
            this.header.rewind();
            byteBufferAllocate.rewind();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public long getSize() {
        return ((long) this.header.limit()) + this.contentSize;
    }

    @Override
    public void parse(ReadableByteChannel readableByteChannel, ByteBuffer byteBuffer, long j, BoxParser boxParser) throws IOException {
        this.header = byteBuffer;
        this.contentSize = j;
        if ((readableByteChannel instanceof FileChannel) && j > AbstractBox.MEM_MAP_THRESHOLD) {
            this.fileChannel = readableByteChannel;
            this.startPosition = readableByteChannel.position();
            readableByteChannel.position(readableByteChannel.position() + j);
        } else {
            this.content = ChannelHelper.readFully(readableByteChannel, CastUtils.l2i(j));
            this.cache.put(0L, new SoftReference(this.content));
        }
    }

    public synchronized ByteBuffer getContent(long j, int i) {
        ByteBuffer byteBuffer;
        for (Long l : this.cache.keySet()) {
            if (l.longValue() <= j && j <= l.longValue() + 10485760 && (byteBuffer = this.cache.get(l).get()) != null && l.longValue() + ((long) byteBuffer.limit()) >= ((long) i) + j) {
                byteBuffer.position((int) (j - l.longValue()));
                ByteBuffer byteBufferSlice = byteBuffer.slice();
                byteBufferSlice.limit(i);
                return byteBufferSlice;
            }
        }
        try {
            MappedByteBuffer map = this.fileChannel.map(FileChannel.MapMode.READ_ONLY, this.startPosition + j, Math.min(10485760L, this.contentSize - j));
            this.cache.put(Long.valueOf(j), new SoftReference(map));
            map.position(0);
            ByteBuffer byteBufferSlice2 = map.slice();
            byteBufferSlice2.limit(i);
            return byteBufferSlice2;
        } catch (IOException e) {
            LOG.fine("Even mapping just 10MB of the source file into the memory failed. " + e);
            throw new RuntimeException("Delayed reading of mdat content failed. Make sure not to close the FileChannel that has been used to create the IsoFile!", e);
        }
    }

    public ByteBuffer getHeader() {
        return this.header;
    }
}
