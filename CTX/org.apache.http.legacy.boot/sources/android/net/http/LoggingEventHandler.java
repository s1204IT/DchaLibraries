package android.net.http;

public class LoggingEventHandler implements EventHandler {
    public void requestSent() {
        HttpLog.v("LoggingEventHandler:requestSent()");
    }

    @Override
    public void status(int i, int i2, int i3, String str) {
    }

    @Override
    public void headers(Headers headers) {
    }

    public void locationChanged(String str, boolean z) {
    }

    @Override
    public void data(byte[] bArr, int i) {
    }

    @Override
    public void endData() {
    }

    @Override
    public void certificate(SslCertificate sslCertificate) {
    }

    @Override
    public void error(int i, String str) {
    }

    @Override
    public boolean handleSslErrorRequest(SslError sslError) {
        return false;
    }
}
