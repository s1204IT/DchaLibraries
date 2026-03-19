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
    static final boolean $assertionsDisabled = false;
    private static Logger LOG = Logger.getLogger(AbstractBoxParser.class.getName());

    public abstract Box createBox(String str, byte[] bArr, String str2);

    @Override
    public Box parseBox(ReadableByteChannel readableByteChannel, ContainerBox containerBox) throws IOException {
        long j;
        ByteBuffer fully = ChannelHelper.readFully(readableByteChannel, 8L);
        long uInt32 = IsoTypeReader.readUInt32(fully);
        byte[] bArrArray = null;
        if (uInt32 < 8 && uInt32 > 1) {
            LOG.severe("Plausibility check failed: size < 8 (size = " + uInt32 + "). Stop parsing!");
            return null;
        }
        String str = IsoTypeReader.read4cc(fully);
        if (uInt32 == 1) {
            ByteBuffer byteBufferAllocate = ByteBuffer.allocate(8);
            readableByteChannel.read(byteBufferAllocate);
            byteBufferAllocate.rewind();
            uInt32 = IsoTypeReader.readUInt64(byteBufferAllocate);
            j = uInt32 - 16;
        } else if (uInt32 == 0) {
            if (readableByteChannel instanceof FileChannel) {
                uInt32 = (readableByteChannel.size() - readableByteChannel.position()) - 8;
                j = uInt32 - 8;
            } else {
                throw new RuntimeException("Only FileChannel inputs may use size == 0 (box reaches to the end of file)");
            }
        } else {
            j = uInt32 - 8;
        }
        if ("uuid".equals(str)) {
            ByteBuffer byteBufferAllocate2 = ByteBuffer.allocate(16);
            readableByteChannel.read(byteBufferAllocate2);
            byteBufferAllocate2.rewind();
            bArrArray = byteBufferAllocate2.array();
            j -= 16;
        }
        long j2 = j;
        Box boxCreateBox = createBox(str, bArrArray, containerBox.getType());
        boxCreateBox.setParent(containerBox);
        LOG.finest("Parsing " + boxCreateBox.getType());
        long j3 = uInt32 - j2;
        if (CastUtils.l2i(j3) == 8) {
            fully.rewind();
        } else if (CastUtils.l2i(j3) == 16) {
            fully = ByteBuffer.allocate(16);
            IsoTypeWriter.writeUInt32(fully, 1L);
            fully.put(IsoFile.fourCCtoBytes(str));
            IsoTypeWriter.writeUInt64(fully, uInt32);
        } else if (CastUtils.l2i(j3) == 24) {
            fully = ByteBuffer.allocate(24);
            IsoTypeWriter.writeUInt32(fully, uInt32);
            fully.put(IsoFile.fourCCtoBytes(str));
            fully.put(bArrArray);
        } else if (CastUtils.l2i(j3) == 32) {
            fully = ByteBuffer.allocate(32);
            IsoTypeWriter.writeUInt32(fully, uInt32);
            fully.put(IsoFile.fourCCtoBytes(str));
            IsoTypeWriter.writeUInt64(fully, uInt32);
            fully.put(bArrArray);
        } else {
            throw new RuntimeException("I didn't expect that");
        }
        boxCreateBox.parse(readableByteChannel, fully, j2, this);
        return boxCreateBox;
    }
}
