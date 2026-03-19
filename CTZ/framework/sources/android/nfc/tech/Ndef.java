package android.nfc.tech;

import android.nfc.FormatException;
import android.nfc.INfcTag;
import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import java.io.IOException;

public final class Ndef extends BasicTagTechnology {
    public static final String EXTRA_NDEF_CARDSTATE = "ndefcardstate";
    public static final String EXTRA_NDEF_MAXLENGTH = "ndefmaxlength";
    public static final String EXTRA_NDEF_MSG = "ndefmsg";
    public static final String EXTRA_NDEF_TYPE = "ndeftype";
    public static final String ICODE_SLI = "com.nxp.ndef.icodesli";
    public static final String MIFARE_CLASSIC = "com.nxp.ndef.mifareclassic";
    public static final int NDEF_MODE_READ_ONLY = 1;
    public static final int NDEF_MODE_READ_WRITE = 2;
    public static final int NDEF_MODE_UNKNOWN = 3;
    public static final String NFC_FORUM_TYPE_1 = "org.nfcforum.ndef.type1";
    public static final String NFC_FORUM_TYPE_2 = "org.nfcforum.ndef.type2";
    public static final String NFC_FORUM_TYPE_3 = "org.nfcforum.ndef.type3";
    public static final String NFC_FORUM_TYPE_4 = "org.nfcforum.ndef.type4";
    private static final String TAG = "NFC";
    public static final int TYPE_1 = 1;
    public static final int TYPE_2 = 2;
    public static final int TYPE_3 = 3;
    public static final int TYPE_4 = 4;
    public static final int TYPE_ICODE_SLI = 102;
    public static final int TYPE_MIFARE_CLASSIC = 101;
    public static final int TYPE_OTHER = -1;
    public static final String UNKNOWN = "android.ndef.unknown";
    private final int mCardState;
    private final int mMaxNdefSize;
    private final NdefMessage mNdefMsg;
    private final int mNdefType;

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

    public static Ndef get(Tag tag) {
        if (!tag.hasTech(6)) {
            return null;
        }
        try {
            return new Ndef(tag);
        } catch (RemoteException e) {
            return null;
        }
    }

    public Ndef(Tag tag) throws RemoteException {
        super(tag, 6);
        Bundle techExtras = tag.getTechExtras(6);
        if (techExtras != null) {
            this.mMaxNdefSize = techExtras.getInt(EXTRA_NDEF_MAXLENGTH);
            this.mCardState = techExtras.getInt(EXTRA_NDEF_CARDSTATE);
            this.mNdefMsg = (NdefMessage) techExtras.getParcelable(EXTRA_NDEF_MSG);
            this.mNdefType = techExtras.getInt(EXTRA_NDEF_TYPE);
            return;
        }
        throw new NullPointerException("NDEF tech extras are null.");
    }

    public NdefMessage getCachedNdefMessage() {
        return this.mNdefMsg;
    }

    public String getType() {
        int i = this.mNdefType;
        switch (i) {
            case 1:
                return NFC_FORUM_TYPE_1;
            case 2:
                return NFC_FORUM_TYPE_2;
            case 3:
                return NFC_FORUM_TYPE_3;
            case 4:
                return NFC_FORUM_TYPE_4;
            default:
                switch (i) {
                    case 101:
                        return MIFARE_CLASSIC;
                    case 102:
                        return ICODE_SLI;
                    default:
                        return UNKNOWN;
                }
        }
    }

    public int getMaxSize() {
        return this.mMaxNdefSize;
    }

    public boolean isWritable() {
        return this.mCardState == 2;
    }

    public NdefMessage getNdefMessage() throws IOException, FormatException {
        checkConnected();
        try {
            INfcTag tagService = this.mTag.getTagService();
            if (tagService == null) {
                throw new IOException("Mock tags don't support this operation.");
            }
            int serviceHandle = this.mTag.getServiceHandle();
            if (tagService.isNdef(serviceHandle)) {
                NdefMessage ndefMessageNdefRead = tagService.ndefRead(serviceHandle);
                if (ndefMessageNdefRead == null && !tagService.isPresent(serviceHandle)) {
                    throw new TagLostException();
                }
                return ndefMessageNdefRead;
            }
            if (tagService.isPresent(serviceHandle)) {
                return null;
            }
            throw new TagLostException();
        } catch (RemoteException e) {
            Log.e(TAG, "NFC service dead", e);
            return null;
        }
    }

    public void writeNdefMessage(NdefMessage ndefMessage) throws IOException, FormatException {
        checkConnected();
        try {
            INfcTag tagService = this.mTag.getTagService();
            if (tagService == null) {
                throw new IOException("Mock tags don't support this operation.");
            }
            int serviceHandle = this.mTag.getServiceHandle();
            if (tagService.isNdef(serviceHandle)) {
                int iNdefWrite = tagService.ndefWrite(serviceHandle, ndefMessage);
                if (iNdefWrite != -8) {
                    switch (iNdefWrite) {
                        case -1:
                            throw new IOException();
                        case 0:
                            return;
                        default:
                            throw new IOException();
                    }
                }
                throw new FormatException();
            }
            throw new IOException("Tag is not ndef");
        } catch (RemoteException e) {
            Log.e(TAG, "NFC service dead", e);
        }
    }

    public boolean canMakeReadOnly() {
        INfcTag tagService = this.mTag.getTagService();
        if (tagService == null) {
            return false;
        }
        try {
            return tagService.canMakeReadOnly(this.mNdefType);
        } catch (RemoteException e) {
            Log.e(TAG, "NFC service dead", e);
            return false;
        }
    }

    public boolean makeReadOnly() throws IOException {
        checkConnected();
        try {
            INfcTag tagService = this.mTag.getTagService();
            if (tagService == null) {
                return false;
            }
            if (tagService.isNdef(this.mTag.getServiceHandle())) {
                int iNdefMakeReadOnly = tagService.ndefMakeReadOnly(this.mTag.getServiceHandle());
                if (iNdefMakeReadOnly == -8) {
                    return false;
                }
                switch (iNdefMakeReadOnly) {
                    case -1:
                        throw new IOException();
                    case 0:
                        return true;
                    default:
                        throw new IOException();
                }
            }
            throw new IOException("Tag is not ndef");
        } catch (RemoteException e) {
            Log.e(TAG, "NFC service dead", e);
            return false;
        }
    }
}
