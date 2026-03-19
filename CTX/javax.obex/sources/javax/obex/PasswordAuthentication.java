package javax.obex;

public final class PasswordAuthentication {
    private final byte[] mPassword;
    private byte[] mUserName;

    public PasswordAuthentication(byte[] bArr, byte[] bArr2) {
        if (bArr != null) {
            this.mUserName = new byte[bArr.length];
            System.arraycopy(bArr, 0, this.mUserName, 0, bArr.length);
        }
        this.mPassword = new byte[bArr2.length];
        System.arraycopy(bArr2, 0, this.mPassword, 0, bArr2.length);
    }

    public byte[] getUserName() {
        return this.mUserName;
    }

    public byte[] getPassword() {
        return this.mPassword;
    }
}
