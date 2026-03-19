package android.nfc.tech;

import android.nfc.Tag;
import android.nfc.TagLostException;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.midi.MidiConstants;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class MifareClassic extends BasicTagTechnology {
    public static final int BLOCK_SIZE = 16;
    public static final byte[] KEY_DEFAULT = {-1, -1, -1, -1, -1, -1};
    public static final byte[] KEY_MIFARE_APPLICATION_DIRECTORY = {MidiConstants.STATUS_POLYPHONIC_AFTERTOUCH, -95, -94, -93, -92, -91};
    public static final byte[] KEY_NFC_FORUM = {-45, -9, -45, -9, -45, -9};
    private static final int MAX_BLOCK_COUNT = 256;
    private static final int MAX_SECTOR_COUNT = 40;
    public static final int SIZE_1K = 1024;
    public static final int SIZE_2K = 2048;
    public static final int SIZE_4K = 4096;
    public static final int SIZE_MINI = 320;
    private static final String TAG = "NFC";
    public static final int TYPE_CLASSIC = 0;
    public static final int TYPE_PLUS = 1;
    public static final int TYPE_PRO = 2;
    public static final int TYPE_UNKNOWN = -1;
    private boolean mIsEmulated;
    private int mSize;
    private int mType;

    @Override
    public void close() throws IOException {
        super.close();
    }

    @Override
    public void connect() throws IOException {
        super.connect();
    }

    @Override
    public Tag getTag() {
        return super.getTag();
    }

    @Override
    public boolean isConnected() {
        return super.isConnected();
    }

    @Override
    public void reconnect() throws IOException {
        super.reconnect();
    }

    public static MifareClassic get(Tag tag) {
        if (!tag.hasTech(8)) {
            return null;
        }
        try {
            return new MifareClassic(tag);
        } catch (RemoteException e) {
            return null;
        }
    }

    public MifareClassic(Tag tag) throws RemoteException {
        super(tag, 8);
        NfcA nfcA = NfcA.get(tag);
        this.mIsEmulated = false;
        switch (nfcA.getSak()) {
            case 1:
            case 8:
                this.mType = 0;
                this.mSize = 1024;
                return;
            case 9:
                this.mType = 0;
                this.mSize = 320;
                return;
            case 16:
                this.mType = 1;
                this.mSize = 2048;
                return;
            case 17:
                this.mType = 1;
                this.mSize = 4096;
                return;
            case 24:
                this.mType = 0;
                this.mSize = 4096;
                return;
            case 40:
                this.mType = 0;
                this.mSize = 1024;
                this.mIsEmulated = true;
                return;
            case 56:
                this.mType = 0;
                this.mSize = 4096;
                this.mIsEmulated = true;
                return;
            case 136:
                this.mType = 0;
                this.mSize = 1024;
                return;
            case 152:
            case 184:
                this.mType = 2;
                this.mSize = 4096;
                return;
            default:
                throw new RuntimeException("Tag incorrectly enumerated as MIFARE Classic, SAK = " + ((int) nfcA.getSak()));
        }
    }

    public int getType() {
        return this.mType;
    }

    public int getSize() {
        return this.mSize;
    }

    public boolean isEmulated() {
        return this.mIsEmulated;
    }

    public int getSectorCount() {
        int i = this.mSize;
        if (i == 320) {
            return 5;
        }
        if (i == 1024) {
            return 16;
        }
        if (i == 2048) {
            return 32;
        }
        if (i == 4096) {
            return 40;
        }
        return 0;
    }

    public int getBlockCount() {
        return this.mSize / 16;
    }

    public int getBlockCountInSector(int i) {
        validateSector(i);
        if (i < 32) {
            return 4;
        }
        return 16;
    }

    public int blockToSector(int i) {
        validateBlock(i);
        if (i < 128) {
            return i / 4;
        }
        return 32 + ((i - 128) / 16);
    }

    public int sectorToBlock(int i) {
        if (i < 32) {
            return i * 4;
        }
        return 128 + ((i - 32) * 16);
    }

    public boolean authenticateSectorWithKeyA(int i, byte[] bArr) throws IOException {
        return authenticate(i, bArr, true);
    }

    public boolean authenticateSectorWithKeyB(int i, byte[] bArr) throws IOException {
        return authenticate(i, bArr, false);
    }

    private boolean authenticate(int i, byte[] bArr, boolean z) throws IOException {
        validateSector(i);
        checkConnected();
        byte[] bArr2 = new byte[12];
        if (z) {
            bArr2[0] = 96;
        } else {
            bArr2[0] = 97;
        }
        bArr2[1] = (byte) sectorToBlock(i);
        byte[] id = getTag().getId();
        System.arraycopy(id, id.length - 4, bArr2, 2, 4);
        System.arraycopy(bArr, 0, bArr2, 6, 6);
        try {
        } catch (TagLostException e) {
            throw e;
        } catch (IOException e2) {
        }
        return transceive(bArr2, false) != null;
    }

    public byte[] readBlock(int i) throws IOException {
        validateBlock(i);
        checkConnected();
        return transceive(new byte[]{48, (byte) i}, false);
    }

    public void writeBlock(int i, byte[] bArr) throws IOException {
        validateBlock(i);
        checkConnected();
        if (bArr.length != 16) {
            throw new IllegalArgumentException("must write 16-bytes");
        }
        byte[] bArr2 = new byte[bArr.length + 2];
        bArr2[0] = MidiConstants.STATUS_POLYPHONIC_AFTERTOUCH;
        bArr2[1] = (byte) i;
        System.arraycopy(bArr, 0, bArr2, 2, bArr.length);
        transceive(bArr2, false);
    }

    public void increment(int i, int i2) throws IOException {
        validateBlock(i);
        validateValueOperand(i2);
        checkConnected();
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(6);
        byteBufferAllocate.order(ByteOrder.LITTLE_ENDIAN);
        byteBufferAllocate.put((byte) -63);
        byteBufferAllocate.put((byte) i);
        byteBufferAllocate.putInt(i2);
        transceive(byteBufferAllocate.array(), false);
    }

    public void decrement(int i, int i2) throws IOException {
        validateBlock(i);
        validateValueOperand(i2);
        checkConnected();
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(6);
        byteBufferAllocate.order(ByteOrder.LITTLE_ENDIAN);
        byteBufferAllocate.put((byte) -64);
        byteBufferAllocate.put((byte) i);
        byteBufferAllocate.putInt(i2);
        transceive(byteBufferAllocate.array(), false);
    }

    public void transfer(int i) throws IOException {
        validateBlock(i);
        checkConnected();
        transceive(new byte[]{MidiConstants.STATUS_CONTROL_CHANGE, (byte) i}, false);
    }

    public void restore(int i) throws IOException {
        validateBlock(i);
        checkConnected();
        transceive(new byte[]{-62, (byte) i}, false);
    }

    public byte[] transceive(byte[] bArr) throws IOException {
        return transceive(bArr, true);
    }

    public int getMaxTransceiveLength() {
        return getMaxTransceiveLengthInternal();
    }

    public void setTimeout(int i) {
        try {
            if (this.mTag.getTagService().setTimeout(8, i) != 0) {
                throw new IllegalArgumentException("The supplied timeout is not valid");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "NFC service dead", e);
        }
    }

    public int getTimeout() {
        try {
            return this.mTag.getTagService().getTimeout(8);
        } catch (RemoteException e) {
            Log.e(TAG, "NFC service dead", e);
            return 0;
        }
    }

    private static void validateSector(int i) {
        if (i < 0 || i >= 40) {
            throw new IndexOutOfBoundsException("sector out of bounds: " + i);
        }
    }

    private static void validateBlock(int i) {
        if (i < 0 || i >= 256) {
            throw new IndexOutOfBoundsException("block out of bounds: " + i);
        }
    }

    private static void validateValueOperand(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("value operand negative");
        }
    }
}
