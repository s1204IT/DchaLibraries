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
    static final boolean $assertionsDisabled;
    private static Logger LOG;
    public static int MEM_MAP_THRESHOLD;
    private ByteBuffer content;
    private ByteBuffer deadBytes = null;
    private ContainerBox parent;
    protected String type;
    private byte[] userType;

    protected abstract void _parseDetails(ByteBuffer byteBuffer);

    protected abstract void getContent(ByteBuffer byteBuffer);

    protected abstract long getContentSize();

    static {
        $assertionsDisabled = !AbstractBox.class.desiredAssertionStatus();
        MEM_MAP_THRESHOLD = 102400;
        LOG = Logger.getLogger(AbstractBox.class.getName());
    }

    protected AbstractBox(String type) {
        this.type = type;
    }

    protected AbstractBox(String type, byte[] userType) {
        this.type = type;
        this.userType = userType;
    }

    @Override
    public void parse(ReadableByteChannel readableByteChannel, ByteBuffer header, long contentSize, BoxParser boxParser) throws IOException {
        if ((readableByteChannel instanceof FileChannel) && contentSize > MEM_MAP_THRESHOLD) {
            this.content = ((FileChannel) readableByteChannel).map(FileChannel.MapMode.READ_ONLY, ((FileChannel) readableByteChannel).position(), contentSize);
            ((FileChannel) readableByteChannel).position(((FileChannel) readableByteChannel).position() + contentSize);
        } else {
            if (!$assertionsDisabled && contentSize >= 2147483647L) {
                throw new AssertionError();
            }
            this.content = ChannelHelper.readFully(readableByteChannel, contentSize);
        }
        if (!isParsed()) {
            parseDetails();
        }
    }

    @Override
    public void getBox(WritableByteChannel os) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(CastUtils.l2i(getSize()));
        getHeader(bb);
        if (this.content == null) {
            getContent(bb);
            if (this.deadBytes != null) {
                this.deadBytes.rewind();
                while (this.deadBytes.remaining() > 0) {
                    bb.put(this.deadBytes);
                }
            }
        } else {
            this.content.rewind();
            bb.put(this.content);
        }
        bb.rewind();
        os.write(bb);
    }

    final synchronized void parseDetails() {
        if (this.content != null) {
            ByteBuffer content = this.content;
            this.content = null;
            content.rewind();
            _parseDetails(content);
            if (content.remaining() > 0) {
                this.deadBytes = content.slice();
            }
            if (!$assertionsDisabled && !verify(content)) {
                throw new AssertionError();
            }
        }
    }

    protected void setDeadBytes(ByteBuffer newDeadBytes) {
        this.deadBytes = newDeadBytes;
    }

    @Override
    public long getSize() {
        long size = this.content == null ? getContentSize() : this.content.limit();
        return size + ((long) (("uuid".equals(getType()) ? 16 : 0) + (size >= 4294967288L ? 8 : 0) + 8)) + ((long) (this.deadBytes != null ? this.deadBytes.limit() : 0));
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
    public void setParent(ContainerBox parent) {
        this.parent = parent;
    }

    public IsoFile getIsoFile() {
        return this.parent.getIsoFile();
    }

    public boolean isParsed() {
        return this.content == null;
    }

    private boolean verify(ByteBuffer content) {
        ByteBuffer bb = ByteBuffer.allocate(CastUtils.l2i(((long) (this.deadBytes != null ? this.deadBytes.limit() : 0)) + getContentSize()));
        getContent(bb);
        if (this.deadBytes != null) {
            this.deadBytes.rewind();
            while (this.deadBytes.remaining() > 0) {
                bb.put(this.deadBytes);
            }
        }
        content.rewind();
        bb.rewind();
        if (content.remaining() != bb.remaining()) {
            LOG.severe(getType() + ": remaining differs " + content.remaining() + " vs. " + bb.remaining());
            return false;
        }
        int p = content.position();
        int i = content.limit() - 1;
        int j = bb.limit() - 1;
        while (i >= p) {
            byte v1 = content.get(i);
            byte v2 = bb.get(j);
            if (v1 == v2) {
                i--;
                j--;
            } else {
                LOG.severe(String.format("%s: buffers differ at %d: %2X/%2X", getType(), Integer.valueOf(i), Byte.valueOf(v1), Byte.valueOf(v2)));
                byte[] b1 = new byte[content.remaining()];
                byte[] b2 = new byte[bb.remaining()];
                content.get(b1);
                bb.get(b2);
                System.err.println("original      : " + Hex.encodeHex(b1, 4));
                System.err.println("reconstructed : " + Hex.encodeHex(b2, 4));
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
