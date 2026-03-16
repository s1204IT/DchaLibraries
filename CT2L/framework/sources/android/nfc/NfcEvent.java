package android.nfc;

public final class NfcEvent {
    public final NfcAdapter nfcAdapter;

    NfcEvent(NfcAdapter nfcAdapter) {
        this.nfcAdapter = nfcAdapter;
    }
}
