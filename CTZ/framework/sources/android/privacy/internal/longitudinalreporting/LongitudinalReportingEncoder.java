package android.privacy.internal.longitudinalreporting;

import android.privacy.DifferentialPrivacyEncoder;
import android.privacy.internal.rappor.RapporConfig;
import android.privacy.internal.rappor.RapporEncoder;
import com.android.internal.annotations.VisibleForTesting;

public class LongitudinalReportingEncoder implements DifferentialPrivacyEncoder {
    private static final boolean DEBUG = false;
    private static final String PRR1_ENCODER_ID = "prr1_encoder_id";
    private static final String PRR2_ENCODER_ID = "prr2_encoder_id";
    private static final String TAG = "LongitudinalEncoder";
    private final LongitudinalReportingConfig mConfig;
    private final Boolean mFakeValue;
    private final RapporEncoder mIRREncoder;
    private final boolean mIsSecure;

    public static LongitudinalReportingEncoder createEncoder(LongitudinalReportingConfig longitudinalReportingConfig, byte[] bArr) {
        return new LongitudinalReportingEncoder(longitudinalReportingConfig, true, bArr);
    }

    @VisibleForTesting
    public static LongitudinalReportingEncoder createInsecureEncoderForTest(LongitudinalReportingConfig longitudinalReportingConfig) {
        return new LongitudinalReportingEncoder(longitudinalReportingConfig, false, null);
    }

    private LongitudinalReportingEncoder(LongitudinalReportingConfig longitudinalReportingConfig, boolean z, byte[] bArr) {
        RapporEncoder rapporEncoderCreateInsecureEncoderForTest;
        this.mConfig = longitudinalReportingConfig;
        this.mIsSecure = z;
        if (getLongTermRandomizedResult(longitudinalReportingConfig.getProbabilityP(), z, bArr, longitudinalReportingConfig.getEncoderId() + PRR1_ENCODER_ID)) {
            this.mFakeValue = Boolean.valueOf(getLongTermRandomizedResult(longitudinalReportingConfig.getProbabilityQ(), z, bArr, longitudinalReportingConfig.getEncoderId() + PRR2_ENCODER_ID));
        } else {
            this.mFakeValue = null;
        }
        RapporConfig iRRConfig = longitudinalReportingConfig.getIRRConfig();
        if (z) {
            rapporEncoderCreateInsecureEncoderForTest = RapporEncoder.createEncoder(iRRConfig, bArr);
        } else {
            rapporEncoderCreateInsecureEncoderForTest = RapporEncoder.createInsecureEncoderForTest(iRRConfig);
        }
        this.mIRREncoder = rapporEncoderCreateInsecureEncoderForTest;
    }

    @Override
    public byte[] encodeString(String str) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] encodeBoolean(boolean z) {
        if (this.mFakeValue != null) {
            z = this.mFakeValue.booleanValue();
        }
        return this.mIRREncoder.encodeBoolean(z);
    }

    @Override
    public byte[] encodeBits(byte[] bArr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LongitudinalReportingConfig getConfig() {
        return this.mConfig;
    }

    @Override
    public boolean isInsecureEncoderForTest() {
        return !this.mIsSecure;
    }

    @VisibleForTesting
    public static boolean getLongTermRandomizedResult(double d, boolean z, byte[] bArr, String str) {
        RapporEncoder rapporEncoderCreateInsecureEncoderForTest;
        double d2 = d < 0.5d ? d * 2.0d : (1.0d - d) * 2.0d;
        boolean z2 = d >= 0.5d;
        RapporConfig rapporConfig = new RapporConfig(str, 1, d2, 0.0d, 1.0d, 1, 1);
        if (z) {
            rapporEncoderCreateInsecureEncoderForTest = RapporEncoder.createEncoder(rapporConfig, bArr);
        } else {
            rapporEncoderCreateInsecureEncoderForTest = RapporEncoder.createInsecureEncoderForTest(rapporConfig);
        }
        return rapporEncoderCreateInsecureEncoderForTest.encodeBoolean(z2)[0] > 0;
    }
}
