package android.nfc.tech;

import android.nfc.Tag;
import android.nfc.TransceiveResult;
import android.os.RemoteException;
import android.util.Log;
import java.io.IOException;

abstract class BasicTagTechnology implements TagTechnology {
    private static final String TAG = "NFC";
    boolean mIsConnected;
    int mSelectedTechnology;
    final Tag mTag;

    BasicTagTechnology(Tag tag, int i) throws RemoteException {
        this.mTag = tag;
        this.mSelectedTechnology = i;
    }

    @Override
    public Tag getTag() {
        return this.mTag;
    }

    void checkConnected() {
        if (this.mTag.getConnectedTechnology() != this.mSelectedTechnology || this.mTag.getConnectedTechnology() == -1) {
            throw new IllegalStateException("Call connect() first!");
        }
    }

    @Override
    public boolean isConnected() {
        if (!this.mIsConnected) {
            return false;
        }
        try {
            return this.mTag.getTagService().isPresent(this.mTag.getServiceHandle());
        } catch (RemoteException e) {
            Log.e(TAG, "NFC service dead", e);
            return false;
        }
    }

    @Override
    public void connect() throws IOException {
        try {
            int iConnect = this.mTag.getTagService().connect(this.mTag.getServiceHandle(), this.mSelectedTechnology);
            if (iConnect == 0) {
                this.mTag.setConnectedTechnology(this.mSelectedTechnology);
                this.mIsConnected = true;
            } else {
                if (iConnect == -21) {
                    throw new UnsupportedOperationException("Connecting to this technology is not supported by the NFC adapter.");
                }
                throw new IOException();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "NFC service dead", e);
            throw new IOException("NFC service died");
        }
    }

    @Override
    public void reconnect() throws IOException {
        if (!this.mIsConnected) {
            throw new IllegalStateException("Technology not connected yet");
        }
        try {
            if (this.mTag.getTagService().reconnect(this.mTag.getServiceHandle()) != 0) {
                this.mIsConnected = false;
                this.mTag.setTechnologyDisconnected();
                throw new IOException();
            }
        } catch (RemoteException e) {
            this.mIsConnected = false;
            this.mTag.setTechnologyDisconnected();
            Log.e(TAG, "NFC service dead", e);
            throw new IOException("NFC service died");
        }
    }

    @Override
    public void close() throws IOException {
        try {
            try {
                this.mTag.getTagService().resetTimeouts();
                this.mTag.getTagService().reconnect(this.mTag.getServiceHandle());
            } catch (RemoteException e) {
                Log.e(TAG, "NFC service dead", e);
            }
        } finally {
            this.mIsConnected = false;
            this.mTag.setTechnologyDisconnected();
        }
    }

    int getMaxTransceiveLengthInternal() {
        try {
            return this.mTag.getTagService().getMaxTransceiveLength(this.mSelectedTechnology);
        } catch (RemoteException e) {
            Log.e(TAG, "NFC service dead", e);
            return 0;
        }
    }

    byte[] transceive(byte[] bArr, boolean z) throws IOException {
        checkConnected();
        try {
            TransceiveResult transceiveResultTransceive = this.mTag.getTagService().transceive(this.mTag.getServiceHandle(), bArr, z);
            if (transceiveResultTransceive == null) {
                throw new IOException("transceive failed");
            }
            return transceiveResultTransceive.getResponseOrThrow();
        } catch (RemoteException e) {
            Log.e(TAG, "NFC service dead", e);
            throw new IOException("NFC service died");
        }
    }
}
