package android.security.keystore;

import android.net.wifi.WifiEnterpriseConfig;
import android.text.TextUtils;
import java.math.BigInteger;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Date;
import javax.security.auth.x500.X500Principal;

public final class KeyGenParameterSpec implements AlgorithmParameterSpec, UserAuthArgs {
    private final byte[] mAttestationChallenge;
    private final String[] mBlockModes;
    private final Date mCertificateNotAfter;
    private final Date mCertificateNotBefore;
    private final BigInteger mCertificateSerialNumber;
    private final X500Principal mCertificateSubject;
    private final String[] mDigests;
    private final String[] mEncryptionPaddings;
    private final boolean mInvalidatedByBiometricEnrollment;
    private final boolean mIsStrongBoxBacked;
    private final int mKeySize;
    private final Date mKeyValidityForConsumptionEnd;
    private final Date mKeyValidityForOriginationEnd;
    private final Date mKeyValidityStart;
    private final String mKeystoreAlias;
    private final int mPurposes;
    private final boolean mRandomizedEncryptionRequired;
    private final String[] mSignaturePaddings;
    private final AlgorithmParameterSpec mSpec;
    private final int mUid;
    private final boolean mUniqueIdIncluded;
    private final boolean mUnlockedDeviceRequired;
    private final boolean mUserAuthenticationRequired;
    private final boolean mUserAuthenticationValidWhileOnBody;
    private final int mUserAuthenticationValidityDurationSeconds;
    private final boolean mUserConfirmationRequired;
    private final boolean mUserPresenceRequired;
    private static final X500Principal DEFAULT_CERT_SUBJECT = new X500Principal("CN=fake");
    private static final BigInteger DEFAULT_CERT_SERIAL_NUMBER = new BigInteger(WifiEnterpriseConfig.ENGINE_ENABLE);
    private static final Date DEFAULT_CERT_NOT_BEFORE = new Date(0);
    private static final Date DEFAULT_CERT_NOT_AFTER = new Date(2461449600000L);

    public KeyGenParameterSpec(String str, int i, int i2, AlgorithmParameterSpec algorithmParameterSpec, X500Principal x500Principal, BigInteger bigInteger, Date date, Date date2, Date date3, Date date4, Date date5, int i3, String[] strArr, String[] strArr2, String[] strArr3, String[] strArr4, boolean z, boolean z2, int i4, boolean z3, byte[] bArr, boolean z4, boolean z5, boolean z6, boolean z7, boolean z8, boolean z9) {
        X500Principal x500Principal2;
        Date date6;
        Date date7;
        BigInteger bigInteger2;
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("keyStoreAlias must not be empty");
        }
        if (x500Principal == null) {
            x500Principal2 = DEFAULT_CERT_SUBJECT;
        } else {
            x500Principal2 = x500Principal;
        }
        if (date == null) {
            date6 = DEFAULT_CERT_NOT_BEFORE;
        } else {
            date6 = date;
        }
        if (date2 == null) {
            date7 = DEFAULT_CERT_NOT_AFTER;
        } else {
            date7 = date2;
        }
        if (bigInteger == null) {
            bigInteger2 = DEFAULT_CERT_SERIAL_NUMBER;
        } else {
            bigInteger2 = bigInteger;
        }
        if (date7.before(date6)) {
            throw new IllegalArgumentException("certificateNotAfter < certificateNotBefore");
        }
        this.mKeystoreAlias = str;
        this.mUid = i;
        this.mKeySize = i2;
        this.mSpec = algorithmParameterSpec;
        this.mCertificateSubject = x500Principal2;
        this.mCertificateSerialNumber = bigInteger2;
        this.mCertificateNotBefore = Utils.cloneIfNotNull(date6);
        this.mCertificateNotAfter = Utils.cloneIfNotNull(date7);
        this.mKeyValidityStart = Utils.cloneIfNotNull(date3);
        this.mKeyValidityForOriginationEnd = Utils.cloneIfNotNull(date4);
        this.mKeyValidityForConsumptionEnd = Utils.cloneIfNotNull(date5);
        this.mPurposes = i3;
        this.mDigests = ArrayUtils.cloneIfNotEmpty(strArr);
        this.mEncryptionPaddings = ArrayUtils.cloneIfNotEmpty(ArrayUtils.nullToEmpty(strArr2));
        this.mSignaturePaddings = ArrayUtils.cloneIfNotEmpty(ArrayUtils.nullToEmpty(strArr3));
        this.mBlockModes = ArrayUtils.cloneIfNotEmpty(ArrayUtils.nullToEmpty(strArr4));
        this.mRandomizedEncryptionRequired = z;
        this.mUserAuthenticationRequired = z2;
        this.mUserPresenceRequired = z3;
        this.mUserAuthenticationValidityDurationSeconds = i4;
        this.mAttestationChallenge = Utils.cloneIfNotNull(bArr);
        this.mUniqueIdIncluded = z4;
        this.mUserAuthenticationValidWhileOnBody = z5;
        this.mInvalidatedByBiometricEnrollment = z6;
        this.mIsStrongBoxBacked = z7;
        this.mUserConfirmationRequired = z8;
        this.mUnlockedDeviceRequired = z9;
    }

    public String getKeystoreAlias() {
        return this.mKeystoreAlias;
    }

    public int getUid() {
        return this.mUid;
    }

    public int getKeySize() {
        return this.mKeySize;
    }

    public AlgorithmParameterSpec getAlgorithmParameterSpec() {
        return this.mSpec;
    }

    public X500Principal getCertificateSubject() {
        return this.mCertificateSubject;
    }

    public BigInteger getCertificateSerialNumber() {
        return this.mCertificateSerialNumber;
    }

    public Date getCertificateNotBefore() {
        return Utils.cloneIfNotNull(this.mCertificateNotBefore);
    }

    public Date getCertificateNotAfter() {
        return Utils.cloneIfNotNull(this.mCertificateNotAfter);
    }

    public Date getKeyValidityStart() {
        return Utils.cloneIfNotNull(this.mKeyValidityStart);
    }

    public Date getKeyValidityForConsumptionEnd() {
        return Utils.cloneIfNotNull(this.mKeyValidityForConsumptionEnd);
    }

    public Date getKeyValidityForOriginationEnd() {
        return Utils.cloneIfNotNull(this.mKeyValidityForOriginationEnd);
    }

    public int getPurposes() {
        return this.mPurposes;
    }

    public String[] getDigests() {
        if (this.mDigests == null) {
            throw new IllegalStateException("Digests not specified");
        }
        return ArrayUtils.cloneIfNotEmpty(this.mDigests);
    }

    public boolean isDigestsSpecified() {
        return this.mDigests != null;
    }

    public String[] getEncryptionPaddings() {
        return ArrayUtils.cloneIfNotEmpty(this.mEncryptionPaddings);
    }

    public String[] getSignaturePaddings() {
        return ArrayUtils.cloneIfNotEmpty(this.mSignaturePaddings);
    }

    public String[] getBlockModes() {
        return ArrayUtils.cloneIfNotEmpty(this.mBlockModes);
    }

    public boolean isRandomizedEncryptionRequired() {
        return this.mRandomizedEncryptionRequired;
    }

    @Override
    public boolean isUserAuthenticationRequired() {
        return this.mUserAuthenticationRequired;
    }

    @Override
    public boolean isUserConfirmationRequired() {
        return this.mUserConfirmationRequired;
    }

    @Override
    public int getUserAuthenticationValidityDurationSeconds() {
        return this.mUserAuthenticationValidityDurationSeconds;
    }

    @Override
    public boolean isUserPresenceRequired() {
        return this.mUserPresenceRequired;
    }

    public byte[] getAttestationChallenge() {
        return Utils.cloneIfNotNull(this.mAttestationChallenge);
    }

    public boolean isUniqueIdIncluded() {
        return this.mUniqueIdIncluded;
    }

    @Override
    public boolean isUserAuthenticationValidWhileOnBody() {
        return this.mUserAuthenticationValidWhileOnBody;
    }

    @Override
    public boolean isInvalidatedByBiometricEnrollment() {
        return this.mInvalidatedByBiometricEnrollment;
    }

    public boolean isStrongBoxBacked() {
        return this.mIsStrongBoxBacked;
    }

    @Override
    public boolean isUnlockedDeviceRequired() {
        return this.mUnlockedDeviceRequired;
    }

    @Override
    public long getBoundToSpecificSecureUserId() {
        return 0L;
    }

    public static final class Builder {
        private byte[] mAttestationChallenge;
        private String[] mBlockModes;
        private Date mCertificateNotAfter;
        private Date mCertificateNotBefore;
        private BigInteger mCertificateSerialNumber;
        private X500Principal mCertificateSubject;
        private String[] mDigests;
        private String[] mEncryptionPaddings;
        private boolean mInvalidatedByBiometricEnrollment;
        private boolean mIsStrongBoxBacked;
        private int mKeySize;
        private Date mKeyValidityForConsumptionEnd;
        private Date mKeyValidityForOriginationEnd;
        private Date mKeyValidityStart;
        private final String mKeystoreAlias;
        private int mPurposes;
        private boolean mRandomizedEncryptionRequired;
        private String[] mSignaturePaddings;
        private AlgorithmParameterSpec mSpec;
        private int mUid;
        private boolean mUniqueIdIncluded;
        private boolean mUnlockedDeviceRequired;
        private boolean mUserAuthenticationRequired;
        private boolean mUserAuthenticationValidWhileOnBody;
        private int mUserAuthenticationValidityDurationSeconds;
        private boolean mUserConfirmationRequired;
        private boolean mUserPresenceRequired;

        public Builder(String str, int i) {
            this.mUid = -1;
            this.mKeySize = -1;
            this.mRandomizedEncryptionRequired = true;
            this.mUserAuthenticationValidityDurationSeconds = -1;
            this.mUserPresenceRequired = false;
            this.mAttestationChallenge = null;
            this.mUniqueIdIncluded = false;
            this.mInvalidatedByBiometricEnrollment = true;
            this.mIsStrongBoxBacked = false;
            this.mUnlockedDeviceRequired = false;
            if (str == null) {
                throw new NullPointerException("keystoreAlias == null");
            }
            if (str.isEmpty()) {
                throw new IllegalArgumentException("keystoreAlias must not be empty");
            }
            this.mKeystoreAlias = str;
            this.mPurposes = i;
        }

        public Builder(KeyGenParameterSpec keyGenParameterSpec) {
            this(keyGenParameterSpec.getKeystoreAlias(), keyGenParameterSpec.getPurposes());
            this.mUid = keyGenParameterSpec.getUid();
            this.mKeySize = keyGenParameterSpec.getKeySize();
            this.mSpec = keyGenParameterSpec.getAlgorithmParameterSpec();
            this.mCertificateSubject = keyGenParameterSpec.getCertificateSubject();
            this.mCertificateSerialNumber = keyGenParameterSpec.getCertificateSerialNumber();
            this.mCertificateNotBefore = keyGenParameterSpec.getCertificateNotBefore();
            this.mCertificateNotAfter = keyGenParameterSpec.getCertificateNotAfter();
            this.mKeyValidityStart = keyGenParameterSpec.getKeyValidityStart();
            this.mKeyValidityForOriginationEnd = keyGenParameterSpec.getKeyValidityForOriginationEnd();
            this.mKeyValidityForConsumptionEnd = keyGenParameterSpec.getKeyValidityForConsumptionEnd();
            this.mPurposes = keyGenParameterSpec.getPurposes();
            if (keyGenParameterSpec.isDigestsSpecified()) {
                this.mDigests = keyGenParameterSpec.getDigests();
            }
            this.mEncryptionPaddings = keyGenParameterSpec.getEncryptionPaddings();
            this.mSignaturePaddings = keyGenParameterSpec.getSignaturePaddings();
            this.mBlockModes = keyGenParameterSpec.getBlockModes();
            this.mRandomizedEncryptionRequired = keyGenParameterSpec.isRandomizedEncryptionRequired();
            this.mUserAuthenticationRequired = keyGenParameterSpec.isUserAuthenticationRequired();
            this.mUserAuthenticationValidityDurationSeconds = keyGenParameterSpec.getUserAuthenticationValidityDurationSeconds();
            this.mUserPresenceRequired = keyGenParameterSpec.isUserPresenceRequired();
            this.mAttestationChallenge = keyGenParameterSpec.getAttestationChallenge();
            this.mUniqueIdIncluded = keyGenParameterSpec.isUniqueIdIncluded();
            this.mUserAuthenticationValidWhileOnBody = keyGenParameterSpec.isUserAuthenticationValidWhileOnBody();
            this.mInvalidatedByBiometricEnrollment = keyGenParameterSpec.isInvalidatedByBiometricEnrollment();
        }

        public Builder setUid(int i) {
            this.mUid = i;
            return this;
        }

        public Builder setKeySize(int i) {
            if (i < 0) {
                throw new IllegalArgumentException("keySize < 0");
            }
            this.mKeySize = i;
            return this;
        }

        public Builder setAlgorithmParameterSpec(AlgorithmParameterSpec algorithmParameterSpec) {
            if (algorithmParameterSpec == null) {
                throw new NullPointerException("spec == null");
            }
            this.mSpec = algorithmParameterSpec;
            return this;
        }

        public Builder setCertificateSubject(X500Principal x500Principal) {
            if (x500Principal == null) {
                throw new NullPointerException("subject == null");
            }
            this.mCertificateSubject = x500Principal;
            return this;
        }

        public Builder setCertificateSerialNumber(BigInteger bigInteger) {
            if (bigInteger == null) {
                throw new NullPointerException("serialNumber == null");
            }
            this.mCertificateSerialNumber = bigInteger;
            return this;
        }

        public Builder setCertificateNotBefore(Date date) {
            if (date == null) {
                throw new NullPointerException("date == null");
            }
            this.mCertificateNotBefore = Utils.cloneIfNotNull(date);
            return this;
        }

        public Builder setCertificateNotAfter(Date date) {
            if (date == null) {
                throw new NullPointerException("date == null");
            }
            this.mCertificateNotAfter = Utils.cloneIfNotNull(date);
            return this;
        }

        public Builder setKeyValidityStart(Date date) {
            this.mKeyValidityStart = Utils.cloneIfNotNull(date);
            return this;
        }

        public Builder setKeyValidityEnd(Date date) {
            setKeyValidityForOriginationEnd(date);
            setKeyValidityForConsumptionEnd(date);
            return this;
        }

        public Builder setKeyValidityForOriginationEnd(Date date) {
            this.mKeyValidityForOriginationEnd = Utils.cloneIfNotNull(date);
            return this;
        }

        public Builder setKeyValidityForConsumptionEnd(Date date) {
            this.mKeyValidityForConsumptionEnd = Utils.cloneIfNotNull(date);
            return this;
        }

        public Builder setDigests(String... strArr) {
            this.mDigests = ArrayUtils.cloneIfNotEmpty(strArr);
            return this;
        }

        public Builder setEncryptionPaddings(String... strArr) {
            this.mEncryptionPaddings = ArrayUtils.cloneIfNotEmpty(strArr);
            return this;
        }

        public Builder setSignaturePaddings(String... strArr) {
            this.mSignaturePaddings = ArrayUtils.cloneIfNotEmpty(strArr);
            return this;
        }

        public Builder setBlockModes(String... strArr) {
            this.mBlockModes = ArrayUtils.cloneIfNotEmpty(strArr);
            return this;
        }

        public Builder setRandomizedEncryptionRequired(boolean z) {
            this.mRandomizedEncryptionRequired = z;
            return this;
        }

        public Builder setUserAuthenticationRequired(boolean z) {
            this.mUserAuthenticationRequired = z;
            return this;
        }

        public Builder setUserConfirmationRequired(boolean z) {
            this.mUserConfirmationRequired = z;
            return this;
        }

        public Builder setUserAuthenticationValidityDurationSeconds(int i) {
            if (i < -1) {
                throw new IllegalArgumentException("seconds must be -1 or larger");
            }
            this.mUserAuthenticationValidityDurationSeconds = i;
            return this;
        }

        public Builder setUserPresenceRequired(boolean z) {
            this.mUserPresenceRequired = z;
            return this;
        }

        public Builder setAttestationChallenge(byte[] bArr) {
            this.mAttestationChallenge = bArr;
            return this;
        }

        public Builder setUniqueIdIncluded(boolean z) {
            this.mUniqueIdIncluded = z;
            return this;
        }

        public Builder setUserAuthenticationValidWhileOnBody(boolean z) {
            this.mUserAuthenticationValidWhileOnBody = z;
            return this;
        }

        public Builder setInvalidatedByBiometricEnrollment(boolean z) {
            this.mInvalidatedByBiometricEnrollment = z;
            return this;
        }

        public Builder setIsStrongBoxBacked(boolean z) {
            this.mIsStrongBoxBacked = z;
            return this;
        }

        public Builder setUnlockedDeviceRequired(boolean z) {
            this.mUnlockedDeviceRequired = z;
            return this;
        }

        public KeyGenParameterSpec build() {
            return new KeyGenParameterSpec(this.mKeystoreAlias, this.mUid, this.mKeySize, this.mSpec, this.mCertificateSubject, this.mCertificateSerialNumber, this.mCertificateNotBefore, this.mCertificateNotAfter, this.mKeyValidityStart, this.mKeyValidityForOriginationEnd, this.mKeyValidityForConsumptionEnd, this.mPurposes, this.mDigests, this.mEncryptionPaddings, this.mSignaturePaddings, this.mBlockModes, this.mRandomizedEncryptionRequired, this.mUserAuthenticationRequired, this.mUserAuthenticationValidityDurationSeconds, this.mUserPresenceRequired, this.mAttestationChallenge, this.mUniqueIdIncluded, this.mUserAuthenticationValidWhileOnBody, this.mInvalidatedByBiometricEnrollment, this.mIsStrongBoxBacked, this.mUserConfirmationRequired, this.mUnlockedDeviceRequired);
        }
    }
}
