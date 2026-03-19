package android.nfc.tech;

import android.nfc.Tag;
import android.os.Bundle;
import android.os.RemoteException;
import java.io.IOException;

public final class NfcV extends BasicTagTechnology {
    public static final String EXTRA_DSFID = "dsfid";
    public static final String EXTRA_RESP_FLAGS = "respflags";
    private byte mDsfId;
    private byte mRespFlags;

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

    public static NfcV get(Tag tag) {
        if (!tag.hasTech(5)) {
            return null;
        }
        try {
            return new NfcV(tag);
        } catch (RemoteException e) {
            return null;
        }
    }

    public NfcV(Tag tag) throws RemoteException {
        super(tag, 5);
        Bundle techExtras = tag.getTechExtras(5);
        this.mRespFlags = techExtras.getByte(EXTRA_RESP_FLAGS);
        this.mDsfId = techExtras.getByte(EXTRA_DSFID);
    }

    public byte getResponseFlags() {
        return this.mRespFlags;
    }

    public byte getDsfId() {
        return this.mDsfId;
    }

    public byte[] transceive(byte[] bArr) throws IOException {
        return transceive(bArr, true);
    }

    public int getMaxTransceiveLength() {
        return getMaxTransceiveLengthInternal();
    }
}
