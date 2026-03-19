package com.googlecode.mp4parser;

import com.coremedia.iso.BoxParser;
import com.coremedia.iso.ChannelHelper;
import com.coremedia.iso.Hex;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;
import com.googlecode.mp4parser.util.CastUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Logger;

public abstract class AbstractBox implements Box {
    static final boolean $assertionsDisabled = false;
    private ByteBuffer content;
    private ByteBuffer deadBytes = null;
    private ContainerBox parent;
    protected String type;
    private byte[] userType;
    public static int MEM_MAP_THRESHOLD = 102400;
    private static Logger LOG = Logger.getLogger(AbstractBox.class.getName());

    protected abstract void _parseDetails(ByteBuffer byteBuffer);

    protected abstract void getContent(ByteBuffer byteBuffer);

    protected abstract long getContentSize();

    protected AbstractBox(String str) {
        this.type = str;
    }

    protected AbstractBox(String str, byte[] bArr) {
        this.type = str;
        this.userType = bArr;
    }

    @Override
    public void parse(ReadableByteChannel readableByteChannel, ByteBuffer byteBuffer, long j, BoxParser boxParser) throws IOException {
        if ((readableByteChannel instanceof FileChannel) && j > MEM_MAP_THRESHOLD) {
            this.content = readableByteChannel.map(FileChannel.MapMode.READ_ONLY, readableByteChannel.position(), j);
            readableByteChannel.position(readableByteChannel.position() + j);
        } else {
            this.content = ChannelHelper.readFully(readableByteChannel, j);
        }
        if (!isParsed()) {
            parseDetails();
        }
    }

    @Override
    public void getBox(WritableByteChannel writableByteChannel) throws IOException {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(CastUtils.l2i(getSize()));
        getHeader(byteBufferAllocate);
        if (this.content == null) {
            getContent(byteBufferAllocate);
            if (this.deadBytes != null) {
                this.deadBytes.rewind();
                while (this.deadBytes.remaining() > 0) {
                    byteBufferAllocate.put(this.deadBytes);
                }
            }
        } else {
            this.content.rewind();
            byteBufferAllocate.put(this.content);
        }
        byteBufferAllocate.rewind();
        writableByteChannel.write(byteBufferAllocate);
    }

    final synchronized void parseDetails() {
        if (this.content != null) {
            ByteBuffer byteBuffer = this.content;
            this.content = null;
            byteBuffer.rewind();
            _parseDetails(byteBuffer);
            if (byteBuffer.remaining() > 0) {
                this.deadBytes = byteBuffer.slice();
            }
        }
    }

    protected void setDeadBytes(ByteBuffer byteBuffer) {
        this.deadBytes = byteBuffer;
    }

    @Override
    public long getSize() {
        long contentSize = this.content == null ? getContentSize() : this.content.limit();
        return contentSize + ((long) (8 + (contentSize >= 4294967288L ? 8 : 0) + ("uuid".equals(getType()) ? 16 : 0))) + ((long) (this.deadBytes != null ? this.deadBytes.limit() : 0));
    }

    @Override
    public String getType() {
        return this.type;
    }

    public byte[] getUserType() {
        return this.userType;
    }

    @Override
    public ContainerBox getParent() {
        return this.parent;
    }

    @Override
    public void setParent(ContainerBox containerBox) {
        this.parent = containerBox;
    }

    public IsoFile getIsoFile() {
        return this.parent.getIsoFile();
    }

    public boolean isParsed() {
        return this.content == null;
    }

    private boolean verify(ByteBuffer byteBuffer) {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(CastUtils.l2i(getContentSize() + ((long) (this.deadBytes != null ? this.deadBytes.limit() : 0))));
        getContent(byteBufferAllocate);
        if (this.deadBytes != null) {
            this.deadBytes.rewind();
            while (this.deadBytes.remaining() > 0) {
                byteBufferAllocate.put(this.deadBytes);
            }
        }
        byteBuffer.rewind();
        byteBufferAllocate.rewind();
        if (byteBuffer.remaining() != byteBufferAllocate.remaining()) {
            LOG.severe(getType() + ": remaining differs " + byteBuffer.remaining() + " vs. " + byteBufferAllocate.remaining());
            return false;
        }
        int iPosition = byteBuffer.position();
        int iLimit = byteBuffer.limit() - 1;
        int iLimit2 = byteBufferAllocate.limit() - 1;
        while (iLimit >= iPosition) {
            byte b = byteBuffer.get(iLimit);
            byte b2 = byteBufferAllocate.get(iLimit2);
            if (b == b2) {
                iLimit--;
                iLimit2--;
            } else {
                LOG.severe(String.format("%s: buffers differ at %d: %2X/%2X", getType(), Integer.valueOf(iLimit), Byte.valueOf(b), Byte.valueOf(b2)));
                byte[] bArr = new byte[byteBuffer.remaining()];
                byte[] bArr2 = new byte[byteBufferAllocate.remaining()];
                byteBuffer.get(bArr);
                byteBufferAllocate.get(bArr2);
                System.err.println("original      : " + Hex.encodeHex(bArr, 4));
                System.err.println("reconstructed : " + Hex.encodeHex(bArr2, 4));
                return false;
            }
        }
        return true;
    }

    private boolean isSmallBox() {
        long jLimit;
        if (this.content == null) {
            jLimit = getContentSize() + ((long) (this.deadBytes != null ? this.deadBytes.limit() : 0)) + 8;
        } else {
            jLimit = this.content.limit();
        }
        return jLimit < 4294967296L;
    }

    private void getHeader(ByteBuffer byteBuffer) {
        if (isSmallBox()) {
            IsoTypeWriter.writeUInt32(byteBuffer, getSize());
            byteBuffer.put(IsoFile.fourCCtoBytes(getType()));
        } else {
            IsoTypeWriter.writeUInt32(byteBuffer, 1L);
            byteBuffer.put(IsoFile.fourCCtoBytes(getType()));
            IsoTypeWriter.writeUInt64(byteBuffer, getSize());
        }
        if ("uuid".equals(getType())) {
            byteBuffer.put(getUserType());
        }
    }
}
