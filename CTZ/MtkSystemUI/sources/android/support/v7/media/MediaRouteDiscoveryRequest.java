package android.support.v7.media;

import android.os.Bundle;

public final class MediaRouteDiscoveryRequest {
    private final Bundle mBundle;
    private MediaRouteSelector mSelector;

    public MediaRouteDiscoveryRequest(MediaRouteSelector selector, boolean activeScan) {
        if (selector == null) {
            throw new IllegalArgumentException("selector must not be null");
        }
        this.mBundle = new Bundle();
        this.mSelector = selector;
        this.mBundle.putBundle("selector", selector.asBundle());
        this.mBundle.putBoolean("activeScan", activeScan);
    }

    public MediaRouteSelector getSelector() {
        ensureSelector();
        return this.mSelector;
    }

    private void ensureSelector() {
        if (this.mSelector == null) {
            this.mSelector = MediaRouteSelector.fromBundle(this.mBundle.getBundle("selector"));
            if (this.mSelector == null) {
                this.mSelector = MediaRouteSelector.EMPTY;
            }
        }
    }

    public boolean isActiveScan() {
        return this.mBundle.getBoolean("activeScan");
    }

    public boolean isValid() {
        ensureSelector();
        return this.mSelector.isValid();
    }

    public boolean equals(Object o) {
        if (!(o instanceof MediaRouteDiscoveryRequest)) {
            return false;
        }
        MediaRouteDiscoveryRequest other = (MediaRouteDiscoveryRequest) o;
        return getSelector().equals(other.getSelector()) && isActiveScan() == other.isActiveScan();
    }

    public int hashCode() {
        return getSelector().hashCode() ^ isActiveScan();
    }

    public String toString() {
        return "DiscoveryRequest{ selector=" + getSelector() + ", activeScan=" + isActiveScan() + ", isValid=" + isValid() + " }";
    }

    public Bundle asBundle() {
        return this.mBundle;
    }
}
