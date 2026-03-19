package android.telephony.ims.compat.feature;

import com.android.ims.internal.IImsRcsFeature;

public class RcsFeature extends ImsFeature {
    private final IImsRcsFeature mImsRcsBinder = new IImsRcsFeature.Stub() {
    };

    @Override
    public void onFeatureReady() {
    }

    @Override
    public void onFeatureRemoved() {
    }

    @Override
    public final IImsRcsFeature getBinder() {
        return this.mImsRcsBinder;
    }
}
