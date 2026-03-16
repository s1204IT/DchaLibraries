package android.net.http;

public class LoggingEventHandler implements EventHandler {
    public void requestSent() {
        HttpLog.v("LoggingEventHandler:requestSent()");
    }

    @Override
    public void status(int major_version, int minor_version, int code, String reason_phrase) {
    }

    @Override
    public void headers(Headers headers) {
    }

    public void locationChanged(String newLocation, boolean permanent) {
    }

    @Override
    public void data(byte[] data, int len) {
    }

    @Override
    public void endData() {
    }

    @Override
    public void certificate(SslCertificate certificate) {
    }

    @Override
    public void error(int id, String description) {
    }

    @Override
    public boolean handleSslErrorRequest(SslError error) {
        return false;
    }
}
