package android.net.wifi.aware;

import java.util.List;

public class DiscoverySessionCallback {
    public void onPublishStarted(PublishDiscoverySession publishDiscoverySession) {
    }

    public void onSubscribeStarted(SubscribeDiscoverySession subscribeDiscoverySession) {
    }

    public void onSessionConfigUpdated() {
    }

    public void onSessionConfigFailed() {
    }

    public void onSessionTerminated() {
    }

    public void onServiceDiscovered(PeerHandle peerHandle, byte[] bArr, List<byte[]> list) {
    }

    public void onServiceDiscoveredWithinRange(PeerHandle peerHandle, byte[] bArr, List<byte[]> list, int i) {
    }

    public void onMessageSendSucceeded(int i) {
    }

    public void onMessageSendFailed(int i) {
    }

    public void onMessageReceived(PeerHandle peerHandle, byte[] bArr) {
    }
}
