package android.util.apk;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

abstract class ApkVerityBuilder {
    private static final int CHUNK_SIZE_BYTES = 4096;
    private static final byte[] DEFAULT_SALT = new byte[8];
    private static final int DIGEST_SIZE_BYTES = 32;
    private static final int FSVERITY_HEADER_SIZE_BYTES = 64;
    private static final String JCA_DIGEST_ALGORITHM = "SHA-256";
    private static final int MMAP_REGION_SIZE_BYTES = 1048576;
    private static final int ZIP_EOCD_CENTRAL_DIR_OFFSET_FIELD_OFFSET = 16;
    private static final int ZIP_EOCD_CENTRAL_DIR_OFFSET_FIELD_SIZE = 4;

    private ApkVerityBuilder() {
    }

    static class ApkVerityResult {
        public final ByteBuffer fsverityData;
        public final byte[] rootHash;

        ApkVerityResult(ByteBuffer byteBuffer, byte[] bArr) {
            this.fsverityData = byteBuffer;
            this.rootHash = bArr;
        }
    }

    static ApkVerityResult generateApkVerity(RandomAccessFile randomAccessFile, SignatureInfo signatureInfo, ByteBufferFactory byteBufferFactory) throws NoSuchAlgorithmException, DigestException, IOException, SecurityException {
        int i = calculateVerityLevelOffset(randomAccessFile.length() - (signatureInfo.centralDirOffset - signatureInfo.apkSigningBlockOffset))[r0.length - 1];
        int i2 = i + 4096;
        ByteBuffer byteBufferCreate = byteBufferFactory.create(i2);
        byteBufferCreate.order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer byteBufferSlice = slice(byteBufferCreate, 0, i);
        int i3 = i + 64;
        ByteBuffer byteBufferSlice2 = slice(byteBufferCreate, i, i3);
        ByteBuffer byteBufferSlice3 = slice(byteBufferCreate, i3, i2);
        byte[] bArr = new byte[32];
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr);
        byteBufferWrap.order(ByteOrder.LITTLE_ENDIAN);
        calculateFsveritySignatureInternal(randomAccessFile, signatureInfo, byteBufferSlice, byteBufferWrap, byteBufferSlice2, byteBufferSlice3);
        byteBufferCreate.position(i3 + byteBufferSlice3.limit());
        byteBufferCreate.putInt(64 + byteBufferSlice3.limit() + 4);
        byteBufferCreate.flip();
        return new ApkVerityResult(byteBufferCreate, bArr);
    }

    static byte[] generateFsverityRootHash(RandomAccessFile randomAccessFile, ByteBuffer byteBuffer, SignatureInfo signatureInfo) throws NoSuchAlgorithmException, DigestException, IOException {
        ByteBuffer byteBufferOrder = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer byteBufferSlice = slice(byteBufferOrder, 0, 64);
        ByteBuffer byteBufferSlice2 = slice(byteBufferOrder, 64, 4032);
        calculateFsveritySignatureInternal(randomAccessFile, signatureInfo, null, null, byteBufferSlice, byteBufferSlice2);
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(byteBufferSlice);
        messageDigest.update(byteBufferSlice2);
        messageDigest.update(byteBuffer);
        return messageDigest.digest();
    }

    private static void calculateFsveritySignatureInternal(RandomAccessFile randomAccessFile, SignatureInfo signatureInfo, ByteBuffer byteBuffer, ByteBuffer byteBuffer2, ByteBuffer byteBuffer3, ByteBuffer byteBuffer4) throws NoSuchAlgorithmException, DigestException, IOException {
        assertSigningBlockAlignedAndHasFullPages(signatureInfo);
        long j = signatureInfo.centralDirOffset - signatureInfo.apkSigningBlockOffset;
        int[] iArrCalculateVerityLevelOffset = calculateVerityLevelOffset(randomAccessFile.length() - j);
        if (byteBuffer != null) {
            byte[] bArrGenerateApkVerityTree = generateApkVerityTree(randomAccessFile, signatureInfo, DEFAULT_SALT, iArrCalculateVerityLevelOffset, byteBuffer);
            if (byteBuffer2 != null) {
                byteBuffer2.put(bArrGenerateApkVerityTree);
                byteBuffer2.flip();
            }
        }
        if (byteBuffer3 != null) {
            byteBuffer3.order(ByteOrder.LITTLE_ENDIAN);
            generateFsverityHeader(byteBuffer3, randomAccessFile.length(), iArrCalculateVerityLevelOffset.length - 1, DEFAULT_SALT);
        }
        if (byteBuffer4 != null) {
            byteBuffer4.order(ByteOrder.LITTLE_ENDIAN);
            generateFsverityExtensions(byteBuffer4, signatureInfo.apkSigningBlockOffset, j, signatureInfo.eocdOffset);
        }
    }

    private static class BufferedDigester implements DataDigester {
        private static final int BUFFER_SIZE = 4096;
        private int mBytesDigestedSinceReset;
        private final byte[] mDigestBuffer;
        private final MessageDigest mMd;
        private final ByteBuffer mOutput;
        private final byte[] mSalt;

        private BufferedDigester(byte[] bArr, ByteBuffer byteBuffer) throws NoSuchAlgorithmException {
            this.mDigestBuffer = new byte[32];
            this.mSalt = bArr;
            this.mOutput = byteBuffer.slice();
            this.mMd = MessageDigest.getInstance("SHA-256");
            this.mMd.update(this.mSalt);
            this.mBytesDigestedSinceReset = 0;
        }

        @Override
        public void consume(ByteBuffer byteBuffer) throws DigestException {
            byteBuffer.position();
            int iRemaining = byteBuffer.remaining();
            while (iRemaining > 0) {
                int iMin = Math.min(iRemaining, 4096 - this.mBytesDigestedSinceReset);
                byteBuffer.limit(byteBuffer.position() + iMin);
                this.mMd.update(byteBuffer);
                iRemaining -= iMin;
                this.mBytesDigestedSinceReset += iMin;
                if (this.mBytesDigestedSinceReset == 4096) {
                    this.mMd.digest(this.mDigestBuffer, 0, this.mDigestBuffer.length);
                    this.mOutput.put(this.mDigestBuffer);
                    this.mMd.update(this.mSalt);
                    this.mBytesDigestedSinceReset = 0;
                }
            }
        }

        public void assertEmptyBuffer() throws DigestException {
            if (this.mBytesDigestedSinceReset != 0) {
                throw new IllegalStateException("Buffer is not empty: " + this.mBytesDigestedSinceReset);
            }
        }

        private void fillUpLastOutputChunk() {
            int iPosition = this.mOutput.position() % 4096;
            if (iPosition == 0) {
                return;
            }
            this.mOutput.put(ByteBuffer.allocate(4096 - iPosition));
        }
    }

    private static void consumeByChunk(DataDigester dataDigester, DataSource dataSource, int i) throws DigestException, IOException {
        long size = dataSource.size();
        long j = 0;
        while (size > 0) {
            int iMin = (int) Math.min(size, i);
            dataSource.feedIntoDataDigester(dataDigester, j, iMin);
            long j2 = iMin;
            j += j2;
            size -= j2;
        }
    }

    private static void generateApkVerityDigestAtLeafLevel(RandomAccessFile randomAccessFile, SignatureInfo signatureInfo, byte[] bArr, ByteBuffer byteBuffer) throws NoSuchAlgorithmException, DigestException, IOException {
        BufferedDigester bufferedDigester = new BufferedDigester(bArr, byteBuffer);
        consumeByChunk(bufferedDigester, new MemoryMappedFileDataSource(randomAccessFile.getFD(), 0L, signatureInfo.apkSigningBlockOffset), 1048576);
        long j = signatureInfo.eocdOffset + 16;
        consumeByChunk(bufferedDigester, new MemoryMappedFileDataSource(randomAccessFile.getFD(), signatureInfo.centralDirOffset, j - signatureInfo.centralDirOffset), 1048576);
        ByteBuffer byteBufferOrder = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        byteBufferOrder.putInt(Math.toIntExact(signatureInfo.apkSigningBlockOffset));
        byteBufferOrder.flip();
        bufferedDigester.consume(byteBufferOrder);
        long j2 = j + 4;
        consumeByChunk(bufferedDigester, new MemoryMappedFileDataSource(randomAccessFile.getFD(), j2, randomAccessFile.length() - j2), 1048576);
        int length = (int) (randomAccessFile.length() % 4096);
        if (length != 0) {
            bufferedDigester.consume(ByteBuffer.allocate(4096 - length));
        }
        bufferedDigester.assertEmptyBuffer();
        bufferedDigester.fillUpLastOutputChunk();
    }

    private static byte[] generateApkVerityTree(RandomAccessFile randomAccessFile, SignatureInfo signatureInfo, byte[] bArr, int[] iArr, ByteBuffer byteBuffer) throws NoSuchAlgorithmException, DigestException, IOException {
        generateApkVerityDigestAtLeafLevel(randomAccessFile, signatureInfo, bArr, slice(byteBuffer, iArr[iArr.length - 2], iArr[iArr.length - 1]));
        int length = iArr.length - 3;
        while (true) {
            if (length >= 0) {
                int i = length + 1;
                ByteBuffer byteBufferSlice = slice(byteBuffer, iArr[i], iArr[length + 2]);
                ByteBuffer byteBufferSlice2 = slice(byteBuffer, iArr[length], iArr[i]);
                ByteBufferDataSource byteBufferDataSource = new ByteBufferDataSource(byteBufferSlice);
                BufferedDigester bufferedDigester = new BufferedDigester(bArr, byteBufferSlice2);
                consumeByChunk(bufferedDigester, byteBufferDataSource, 4096);
                bufferedDigester.assertEmptyBuffer();
                bufferedDigester.fillUpLastOutputChunk();
                length--;
            } else {
                byte[] bArr2 = new byte[32];
                BufferedDigester bufferedDigester2 = new BufferedDigester(bArr, ByteBuffer.wrap(bArr2));
                bufferedDigester2.consume(slice(byteBuffer, 0, 4096));
                bufferedDigester2.assertEmptyBuffer();
                return bArr2;
            }
        }
    }

    private static ByteBuffer generateFsverityHeader(ByteBuffer byteBuffer, long j, int i, byte[] bArr) {
        if (bArr.length != 8) {
            throw new IllegalArgumentException("salt is not 8 bytes long");
        }
        byteBuffer.put("TrueBrew".getBytes());
        byteBuffer.put((byte) 1);
        byteBuffer.put((byte) 0);
        byteBuffer.put((byte) 12);
        byteBuffer.put((byte) 7);
        byteBuffer.putShort((short) 1);
        byteBuffer.putShort((short) 1);
        byteBuffer.putInt(0);
        byteBuffer.putInt(0);
        byteBuffer.putLong(j);
        byteBuffer.put((byte) 2);
        byteBuffer.put((byte) 0);
        byteBuffer.put(bArr);
        skip(byteBuffer, 22);
        byteBuffer.flip();
        return byteBuffer;
    }

    private static ByteBuffer generateFsverityExtensions(ByteBuffer byteBuffer, long j, long j2, long j3) {
        byteBuffer.putInt(24);
        byteBuffer.putShort((short) 1);
        skip(byteBuffer, 2);
        byteBuffer.putLong(j);
        byteBuffer.putLong(j2);
        byteBuffer.putInt(20);
        byteBuffer.putShort((short) 2);
        skip(byteBuffer, 2);
        byteBuffer.putLong(j3 + 16);
        byteBuffer.putInt(Math.toIntExact(j));
        skip(byteBuffer, 4);
        byteBuffer.flip();
        return byteBuffer;
    }

    private static int[] calculateVerityLevelOffset(long j) {
        ArrayList arrayList = new ArrayList();
        do {
            j = divideRoundup(j, 4096L) * 32;
            arrayList.add(Long.valueOf(divideRoundup(j, 4096L) * 4096));
        } while (j > 4096);
        int[] iArr = new int[arrayList.size() + 1];
        int i = 0;
        iArr[0] = 0;
        while (i < arrayList.size()) {
            int i2 = i + 1;
            iArr[i2] = iArr[i] + Math.toIntExact(((Long) arrayList.get((arrayList.size() - i) - 1)).longValue());
            i = i2;
        }
        return iArr;
    }

    private static void assertSigningBlockAlignedAndHasFullPages(SignatureInfo signatureInfo) {
        if (signatureInfo.apkSigningBlockOffset % 4096 != 0) {
            throw new IllegalArgumentException("APK Signing Block does not start at the page  boundary: " + signatureInfo.apkSigningBlockOffset);
        }
        if ((signatureInfo.centralDirOffset - signatureInfo.apkSigningBlockOffset) % 4096 != 0) {
            throw new IllegalArgumentException("Size of APK Signing Block is not a multiple of 4096: " + (signatureInfo.centralDirOffset - signatureInfo.apkSigningBlockOffset));
        }
    }

    private static ByteBuffer slice(ByteBuffer byteBuffer, int i, int i2) {
        ByteBuffer byteBufferDuplicate = byteBuffer.duplicate();
        byteBufferDuplicate.position(0);
        byteBufferDuplicate.limit(i2);
        byteBufferDuplicate.position(i);
        return byteBufferDuplicate.slice();
    }

    private static void skip(ByteBuffer byteBuffer, int i) {
        byteBuffer.position(byteBuffer.position() + i);
    }

    private static long divideRoundup(long j, long j2) {
        return ((j + j2) - 1) / j2;
    }
}
