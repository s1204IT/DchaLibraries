package android.net;

public abstract class NetworkSpecifier {
    public abstract boolean satisfiedBy(NetworkSpecifier networkSpecifier);

    public void assertValidFromUid(int i) {
    }

    public NetworkSpecifier redact() {
        return this;
    }
}
