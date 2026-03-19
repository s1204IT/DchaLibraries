package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.midi.MidiConstants;
import com.android.internal.util.ArrayUtils;
import java.io.ByteArrayInputStream;
import java.lang.ref.SoftReference;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public class Signature implements Parcelable {
    public static final Parcelable.Creator<Signature> CREATOR = new Parcelable.Creator<Signature>() {
        @Override
        public Signature createFromParcel(Parcel parcel) {
            return new Signature(parcel);
        }

        @Override
        public Signature[] newArray(int i) {
            return new Signature[i];
        }
    };
    private Certificate[] mCertificateChain;
    private int mHashCode;
    private boolean mHaveHashCode;
    private final byte[] mSignature;
    private SoftReference<String> mStringRef;

    public Signature(byte[] bArr) {
        this.mSignature = (byte[]) bArr.clone();
        this.mCertificateChain = null;
    }

    public Signature(Certificate[] certificateArr) throws CertificateEncodingException {
        this.mSignature = certificateArr[0].getEncoded();
        if (certificateArr.length > 1) {
            this.mCertificateChain = (Certificate[]) Arrays.copyOfRange(certificateArr, 1, certificateArr.length);
        }
    }

    private static final int parseHexDigit(int i) {
        if (48 <= i && i <= 57) {
            return i - 48;
        }
        if (97 <= i && i <= 102) {
            return (i - 97) + 10;
        }
        if (65 <= i && i <= 70) {
            return (i - 65) + 10;
        }
        throw new IllegalArgumentException("Invalid character " + i + " in hex string");
    }

    public Signature(String str) {
        byte[] bytes = str.getBytes();
        int length = bytes.length;
        if (length % 2 != 0) {
            throw new IllegalArgumentException("text size " + length + " is not even");
        }
        byte[] bArr = new byte[length / 2];
        int i = 0;
        int i2 = 0;
        while (i < length) {
            int i3 = i + 1;
            bArr[i2] = (byte) ((parseHexDigit(bytes[i]) << 4) | parseHexDigit(bytes[i3]));
            i = i3 + 1;
            i2++;
        }
        this.mSignature = bArr;
    }

    public char[] toChars() {
        return toChars(null, null);
    }

    public char[] toChars(char[] cArr, int[] iArr) {
        byte[] bArr = this.mSignature;
        int length = bArr.length;
        int i = length * 2;
        if (cArr == null || i > cArr.length) {
            cArr = new char[i];
        }
        for (int i2 = 0; i2 < length; i2++) {
            byte b = bArr[i2];
            int i3 = (b >> 4) & 15;
            int i4 = i2 * 2;
            cArr[i4] = (char) (i3 >= 10 ? (i3 + 97) - 10 : i3 + 48);
            int i5 = b & MidiConstants.STATUS_CHANNEL_MASK;
            cArr[i4 + 1] = (char) (i5 >= 10 ? (97 + i5) - 10 : 48 + i5);
        }
        if (iArr != null) {
            iArr[0] = length;
        }
        return cArr;
    }

    public String toCharsString() {
        String str = this.mStringRef == null ? null : this.mStringRef.get();
        if (str != null) {
            return str;
        }
        String str2 = new String(toChars());
        this.mStringRef = new SoftReference<>(str2);
        return str2;
    }

    public byte[] toByteArray() {
        byte[] bArr = new byte[this.mSignature.length];
        System.arraycopy(this.mSignature, 0, bArr, 0, this.mSignature.length);
        return bArr;
    }

    public PublicKey getPublicKey() throws CertificateException {
        return CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(this.mSignature)).getPublicKey();
    }

    public Signature[] getChainSignatures() throws CertificateEncodingException {
        int i = 0;
        int i2 = 1;
        if (this.mCertificateChain == null) {
            return new Signature[]{this};
        }
        Signature[] signatureArr = new Signature[this.mCertificateChain.length + 1];
        signatureArr[0] = this;
        Certificate[] certificateArr = this.mCertificateChain;
        int length = certificateArr.length;
        while (i < length) {
            signatureArr[i2] = new Signature(certificateArr[i].getEncoded());
            i++;
            i2++;
        }
        return signatureArr;
    }

    public boolean equals(Object obj) {
        if (obj != null) {
            try {
                Signature signature = (Signature) obj;
                if (this != signature) {
                    if (!Arrays.equals(this.mSignature, signature.mSignature)) {
                        return false;
                    }
                }
                return true;
            } catch (ClassCastException e) {
            }
        }
        return false;
    }

    public int hashCode() {
        if (this.mHaveHashCode) {
            return this.mHashCode;
        }
        this.mHashCode = Arrays.hashCode(this.mSignature);
        this.mHaveHashCode = true;
        return this.mHashCode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByteArray(this.mSignature);
    }

    private Signature(Parcel parcel) {
        this.mSignature = parcel.createByteArray();
    }

    public static boolean areExactMatch(Signature[] signatureArr, Signature[] signatureArr2) {
        return signatureArr.length == signatureArr2.length && ArrayUtils.containsAll(signatureArr, signatureArr2) && ArrayUtils.containsAll(signatureArr2, signatureArr);
    }

    public static boolean areEffectiveMatch(Signature[] signatureArr, Signature[] signatureArr2) throws CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Signature[] signatureArr3 = new Signature[signatureArr.length];
        for (int i = 0; i < signatureArr.length; i++) {
            signatureArr3[i] = bounce(certificateFactory, signatureArr[i]);
        }
        Signature[] signatureArr4 = new Signature[signatureArr2.length];
        for (int i2 = 0; i2 < signatureArr2.length; i2++) {
            signatureArr4[i2] = bounce(certificateFactory, signatureArr2[i2]);
        }
        return areExactMatch(signatureArr3, signatureArr4);
    }

    public static boolean areEffectiveMatch(Signature signature, Signature signature2) throws CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        return bounce(certificateFactory, signature).equals(bounce(certificateFactory, signature2));
    }

    public static Signature bounce(CertificateFactory certificateFactory, Signature signature) throws CertificateException {
        Signature signature2 = new Signature(((X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(signature.mSignature))).getEncoded());
        if (Math.abs(signature2.mSignature.length - signature.mSignature.length) > 2) {
            throw new CertificateException("Bounced cert length looks fishy; before " + signature.mSignature.length + ", after " + signature2.mSignature.length);
        }
        return signature2;
    }
}
