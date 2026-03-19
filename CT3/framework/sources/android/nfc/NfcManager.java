package android.nfc;

import android.content.Context;

public final class NfcManager {
    private final NfcAdapter mAdapter;

    public NfcManager(Context context) {
        NfcAdapter nfcAdapter;
        Context context2 = context.getApplicationContext();
        if (context2 == null) {
            throw new IllegalArgumentException("context not associated with any application (using a mock context?)");
        }
        try {
            nfcAdapter = NfcAdapter.getNfcAdapter(context2);
        } catch (UnsupportedOperationException e) {
            nfcAdapter = null;
        }
        this.mAdapter = nfcAdapter;
    }

    public NfcAdapter getDefaultAdapter() {
        return this.mAdapter;
    }
}
