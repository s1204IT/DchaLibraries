package com.android.ims;

import android.net.Uri;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import java.util.Arrays;

public class ImsConnectionStateListener extends ImsRegistrationImplBase.Callback {
    public final void onRegistered(int i) {
        onImsConnected(i);
    }

    public final void onRegistering(int i) {
        onImsProgressing(i);
    }

    public final void onDeregistered(ImsReasonInfo imsReasonInfo) {
        onImsDisconnected(imsReasonInfo);
    }

    public final void onTechnologyChangeFailed(int i, ImsReasonInfo imsReasonInfo) {
        onRegistrationChangeFailed(i, imsReasonInfo);
    }

    public void onSubscriberAssociatedUriChanged(Uri[] uriArr) {
        registrationAssociatedUriChanged(uriArr);
    }

    public void onFeatureCapabilityChangedAdapter(int i, ImsFeature.Capabilities capabilities) {
        int[] iArr = new int[6];
        Arrays.fill(iArr, -1);
        int[] iArr2 = new int[6];
        Arrays.fill(iArr2, -1);
        switch (i) {
            case 0:
                if (capabilities.isCapable(1)) {
                    iArr[0] = 0;
                }
                if (capabilities.isCapable(2)) {
                    iArr[1] = 1;
                }
                if (capabilities.isCapable(4)) {
                    iArr[4] = 4;
                }
                break;
            case 1:
                if (capabilities.isCapable(1)) {
                    iArr[2] = 2;
                }
                if (capabilities.isCapable(2)) {
                    iArr[3] = 3;
                }
                if (capabilities.isCapable(4)) {
                    iArr[5] = 5;
                }
                break;
        }
        for (int i2 = 0; i2 < iArr.length; i2++) {
            if (iArr[i2] != i2) {
                iArr2[i2] = i2;
            }
        }
        onFeatureCapabilityChanged(1, iArr, iArr2);
    }

    public void onImsConnected(int i) {
    }

    public void onImsProgressing(int i) {
    }

    public void onImsDisconnected(ImsReasonInfo imsReasonInfo) {
    }

    public void onImsResumed() {
    }

    public void onImsSuspended() {
    }

    public void onFeatureCapabilityChanged(int i, int[] iArr, int[] iArr2) {
    }

    public void onVoiceMessageCountChanged(int i) {
    }

    public void registrationAssociatedUriChanged(Uri[] uriArr) {
    }

    public void onRegistrationChangeFailed(int i, ImsReasonInfo imsReasonInfo) {
    }
}
