package com.coremedia.iso;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;
import com.googlecode.mp4parser.util.CastUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.Logger;

public abstract class AbstractBoxParser implements BoxParser {
    static final boolean $assertionsDisabled;
    private static Logger LOG;

    public abstract Box createBox(String str, byte[] bArr, String str2);

    static {
        $assertionsDisabled = !AbstractBoxParser.class.desiredAssertionStatus();
        LOG = Logger.getLogger(AbstractBoxParser.class.getName());
    }

    @Override
    public Box parseBox(ReadableByteChannel byteChannel, ContainerBox parent) throws IOException {
        long contentSize;
        ByteBuffer header = ChannelHelper.readFully(byteChannel, 8L);
        long size = IsoTypeReader.readUInt32(header);
        if (size < 8 && size > 1) {
            LOG.severe("Plausibility check failed: size < 8 (size = " + size + "). Stop parsing!");
            return null;
        }
        String type = IsoTypeReader.read4cc(header);
        byte[] usertype = null;
        if (size == 1) {
            ByteBuffer bb = ByteBuffer.allocate(8);
            byteChannel.read(bb);
            bb.rewind();
            size = IsoTypeReader.readUInt64(bb);
            contentSize = size - 16;
        } else if (size == 0) {
            if (byteChannel instanceof FileChannel) {
                size = (((FileChannel) byteChannel).size() - ((FileChannel) byteChannel).position()) - 8;
                contentSize = size - 8;
            } else {
                throw new RuntimeException("Only FileChannel inputs may use size == 0 (box reaches to the end of file)");
            }
        } else {
            contentSize = size - 8;
        }
        if ("uuid".equals(type)) {
            ByteBuffer bb2 = ByteBuffer.allocate(16);
            byteChannel.read(bb2);
            bb2.rewind();
            usertype = bb2.array();
            contentSize -= 16;
        }
        Box box = createBox(type, usertype, parent.getType());
        box.setParent(parent);
        LOG.finest("Parsing " + box.getType());
        if (CastUtils.l2i(size - contentSize) == 8) {
            header.rewind();
        } else if (CastUtils.l2i(size - contentSize) == 16) {
            header = ByteBuffer.allocate(16);
            IsoTypeWriter.writeUInt32(header, 1L);
            header.put(IsoFile.fourCCtoBytes(type));
            IsoTypeWriter.writeUInt64(header, size);
        } else if (CastUtils.l2i(size - contentSize) == 24) {
            header = ByteBuffer.allocate(24);
            IsoTypeWriter.writeUInt32(header, size);
            header.put(IsoFile.fourCCtoBytes(type));
            header.put(usertype);
        } else if (CastUtils.l2i(size - contentSize) == 32) {
            header = ByteBuffer.allocate(32);
            IsoTypeWriter.writeUInt32(header, size);
            header.put(IsoFile.fourCCtoBytes(type));
            IsoTypeWriter.writeUInt64(header, size);
            header.put(usertype);
        } else {
            throw new RuntimeException("I didn't expect that");
        }
        box.parse(byteChannel, header, contentSize, this);
        if ($assertionsDisabled || size == box.getSize()) {
            return box;
        }
        throw new AssertionError("Reconstructed Size is not x to the number of parsed bytes! (" + box.getType() + ") Actual Box size: " + size + " Calculated size: " + box.getSize());
    }
}
