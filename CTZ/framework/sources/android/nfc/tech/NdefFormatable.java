package android.nfc.tech;

import android.nfc.FormatException;
import android.nfc.INfcTag;
import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.os.RemoteException;
import android.util.Log;
import java.io.IOException;

public final class NdefFormatable extends BasicTagTechnology {
    private static final String TAG = "NFC";

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

    public static NdefFormatable get(Tag tag) {
        if (!tag.hasTech(7)) {
            return null;
        }
        try {
            return new NdefFormatable(tag);
        } catch (RemoteException e) {
            return null;
        }
    }

    public NdefFormatable(Tag tag) throws RemoteException {
        super(tag, 7);
    }

    public void format(NdefMessage ndefMessage) throws IOException, FormatException {
        format(ndefMessage, false);
    }

    public void formatReadOnly(NdefMessage ndefMessage) throws IOException, FormatException {
        format(ndefMessage, true);
    }

    void format(NdefMessage ndefMessage, boolean z) throws IOException, FormatException {
        checkConnected();
        try {
            int serviceHandle = this.mTag.getServiceHandle();
            INfcTag tagService = this.mTag.getTagService();
            int ndef = tagService.formatNdef(serviceHandle, MifareClassic.KEY_DEFAULT);
            if (ndef != -8) {
                switch (ndef) {
                    case -1:
                        throw new IOException();
                    case 0:
                        if (!tagService.isNdef(serviceHandle)) {
                            throw new IOException();
                        }
                        if (ndefMessage != null) {
                            int iNdefWrite = tagService.ndefWrite(serviceHandle, ndefMessage);
                            if (iNdefWrite != -8) {
                                switch (iNdefWrite) {
                                    case -1:
                                        throw new IOException();
                                    case 0:
                                        break;
                                    default:
                                        throw new IOException();
                                }
                            } else {
                                throw new FormatException();
                            }
                        }
                        if (z) {
                            int iNdefMakeReadOnly = tagService.ndefMakeReadOnly(serviceHandle);
                            if (iNdefMakeReadOnly != -8) {
                                switch (iNdefMakeReadOnly) {
                                    case -1:
                                        throw new IOException();
                                    case 0:
                                        break;
                                    default:
                                        throw new IOException();
                                }
                            } else {
                                throw new IOException();
                            }
                        }
                        return;
                    default:
                        throw new IOException();
                }
            }
            throw new FormatException();
        } catch (RemoteException e) {
            Log.e(TAG, "NFC service dead", e);
        }
    }
}
