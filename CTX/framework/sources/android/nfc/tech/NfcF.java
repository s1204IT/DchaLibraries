package android.nfc.tech;

import android.nfc.Tag;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import java.io.IOException;

public final class NfcF extends BasicTagTechnology {
    public static final String EXTRA_PMM = "pmm";
    public static final String EXTRA_SC = "systemcode";
    private static final String TAG = "NFC";
    private byte[] mManufacturer;
    private byte[] mSystemCode;

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

    public static NfcF get(Tag tag) {
        if (!tag.hasTech(4)) {
            return null;
        }
        try {
            return new NfcF(tag);
        } catch (RemoteException e) {
            return null;
        }
    }

    public NfcF(Tag tag) throws RemoteException {
        super(tag, 4);
        this.mSystemCode = null;
        this.mManufacturer = null;
        Bundle techExtras = tag.getTechExtras(4);
        if (techExtras != null) {
            this.mSystemCode = techExtras.getByteArray(EXTRA_SC);
            this.mManufacturer = techExtras.getByteArray(EXTRA_PMM);
        }
    }

    public byte[] getSystemCode() {
        return this.mSystemCode;
    }

    public byte[] getManufacturer() {
        return this.mManufacturer;
    }

    public byte[] transceive(byte[] bArr) throws IOException {
        return transceive(bArr, true);
    }

    public int getMaxTransceiveLength() {
        return getMaxTransceiveLengthInternal();
    }

    public void setTimeout(int i) {
        try {
            if (this.mTag.getTagService().setTimeout(4, i) != 0) {
                throw new IllegalArgumentException("The supplied timeout is not valid");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "NFC service dead", e);
        }
    }

    public int getTimeout() {
        try {
            return this.mTag.getTagService().getTimeout(4);
        } catch (RemoteException e) {
            Log.e(TAG, "NFC service dead", e);
            return 0;
        }
    }
}
