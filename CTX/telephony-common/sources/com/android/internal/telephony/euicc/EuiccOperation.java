package com.android.internal.telephony.euicc;

import android.app.PendingIntent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.euicc.DownloadableSubscription;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
public class EuiccOperation implements Parcelable {

    @VisibleForTesting
    static final int ACTION_DOWNLOAD_CONFIRMATION_CODE = 7;

    @VisibleForTesting
    static final int ACTION_DOWNLOAD_DEACTIVATE_SIM = 2;

    @VisibleForTesting
    static final int ACTION_DOWNLOAD_NO_PRIVILEGES = 3;

    @VisibleForTesting
    static final int ACTION_GET_DEFAULT_LIST_DEACTIVATE_SIM = 4;

    @VisibleForTesting
    static final int ACTION_GET_METADATA_DEACTIVATE_SIM = 1;

    @VisibleForTesting
    static final int ACTION_SWITCH_DEACTIVATE_SIM = 5;

    @VisibleForTesting
    static final int ACTION_SWITCH_NO_PRIVILEGES = 6;
    public static final Parcelable.Creator<EuiccOperation> CREATOR = new Parcelable.Creator<EuiccOperation>() {
        @Override
        public EuiccOperation createFromParcel(Parcel parcel) {
            return new EuiccOperation(parcel);
        }

        @Override
        public EuiccOperation[] newArray(int i) {
            return new EuiccOperation[i];
        }
    };
    private static final String TAG = "EuiccOperation";

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public final int mAction;
    private final String mCallingPackage;
    private final long mCallingToken;
    private final DownloadableSubscription mDownloadableSubscription;
    private final int mSubscriptionId;
    private final boolean mSwitchAfterDownload;

    @VisibleForTesting
    @Retention(RetentionPolicy.SOURCE)
    @interface Action {
    }

    public static EuiccOperation forGetMetadataDeactivateSim(long j, DownloadableSubscription downloadableSubscription, String str) {
        return new EuiccOperation(1, j, downloadableSubscription, 0, false, str);
    }

    public static EuiccOperation forDownloadDeactivateSim(long j, DownloadableSubscription downloadableSubscription, boolean z, String str) {
        return new EuiccOperation(2, j, downloadableSubscription, 0, z, str);
    }

    public static EuiccOperation forDownloadNoPrivileges(long j, DownloadableSubscription downloadableSubscription, boolean z, String str) {
        return new EuiccOperation(3, j, downloadableSubscription, 0, z, str);
    }

    public static EuiccOperation forDownloadConfirmationCode(long j, DownloadableSubscription downloadableSubscription, boolean z, String str) {
        return new EuiccOperation(7, j, downloadableSubscription, 0, z, str);
    }

    static EuiccOperation forGetDefaultListDeactivateSim(long j, String str) {
        return new EuiccOperation(4, j, null, 0, false, str);
    }

    static EuiccOperation forSwitchDeactivateSim(long j, int i, String str) {
        return new EuiccOperation(5, j, null, i, false, str);
    }

    static EuiccOperation forSwitchNoPrivileges(long j, int i, String str) {
        return new EuiccOperation(6, j, null, i, false, str);
    }

    EuiccOperation(int i, long j, DownloadableSubscription downloadableSubscription, int i2, boolean z, String str) {
        this.mAction = i;
        this.mCallingToken = j;
        this.mDownloadableSubscription = downloadableSubscription;
        this.mSubscriptionId = i2;
        this.mSwitchAfterDownload = z;
        this.mCallingPackage = str;
    }

    EuiccOperation(Parcel parcel) {
        this.mAction = parcel.readInt();
        this.mCallingToken = parcel.readLong();
        this.mDownloadableSubscription = (DownloadableSubscription) parcel.readTypedObject(DownloadableSubscription.CREATOR);
        this.mSubscriptionId = parcel.readInt();
        this.mSwitchAfterDownload = parcel.readBoolean();
        this.mCallingPackage = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mAction);
        parcel.writeLong(this.mCallingToken);
        parcel.writeTypedObject(this.mDownloadableSubscription, i);
        parcel.writeInt(this.mSubscriptionId);
        parcel.writeBoolean(this.mSwitchAfterDownload);
        parcel.writeString(this.mCallingPackage);
    }

    public void continueOperation(Bundle bundle, PendingIntent pendingIntent) {
        Binder.restoreCallingIdentity(this.mCallingToken);
        switch (this.mAction) {
            case 1:
                resolvedGetMetadataDeactivateSim(bundle.getBoolean("android.service.euicc.extra.RESOLUTION_CONSENT"), pendingIntent);
                break;
            case 2:
                resolvedDownloadDeactivateSim(bundle.getBoolean("android.service.euicc.extra.RESOLUTION_CONSENT"), pendingIntent);
                break;
            case 3:
                resolvedDownloadNoPrivileges(bundle.getBoolean("android.service.euicc.extra.RESOLUTION_CONSENT"), pendingIntent);
                break;
            case 4:
                resolvedGetDefaultListDeactivateSim(bundle.getBoolean("android.service.euicc.extra.RESOLUTION_CONSENT"), pendingIntent);
                break;
            case 5:
                resolvedSwitchDeactivateSim(bundle.getBoolean("android.service.euicc.extra.RESOLUTION_CONSENT"), pendingIntent);
                break;
            case 6:
                resolvedSwitchNoPrivileges(bundle.getBoolean("android.service.euicc.extra.RESOLUTION_CONSENT"), pendingIntent);
                break;
            case 7:
                resolvedDownloadConfirmationCode(bundle.getString("android.service.euicc.extra.RESOLUTION_CONFIRMATION_CODE"), pendingIntent);
                break;
            default:
                Log.wtf(TAG, "Unknown action: " + this.mAction);
                break;
        }
    }

    private void resolvedGetMetadataDeactivateSim(boolean z, PendingIntent pendingIntent) {
        if (z) {
            EuiccController.get().getDownloadableSubscriptionMetadata(this.mDownloadableSubscription, true, this.mCallingPackage, pendingIntent);
        } else {
            fail(pendingIntent);
        }
    }

    private void resolvedDownloadDeactivateSim(boolean z, PendingIntent pendingIntent) {
        if (z) {
            EuiccController.get().downloadSubscription(this.mDownloadableSubscription, this.mSwitchAfterDownload, this.mCallingPackage, true, pendingIntent);
        } else {
            fail(pendingIntent);
        }
    }

    private void resolvedDownloadNoPrivileges(boolean z, PendingIntent pendingIntent) {
        if (z) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                EuiccController.get().downloadSubscriptionPrivileged(jClearCallingIdentity, this.mDownloadableSubscription, this.mSwitchAfterDownload, true, this.mCallingPackage, pendingIntent);
                return;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        fail(pendingIntent);
    }

    private void resolvedDownloadConfirmationCode(String str, PendingIntent pendingIntent) {
        if (TextUtils.isEmpty(str)) {
            fail(pendingIntent);
        } else {
            this.mDownloadableSubscription.setConfirmationCode(str);
            EuiccController.get().downloadSubscription(this.mDownloadableSubscription, this.mSwitchAfterDownload, this.mCallingPackage, true, pendingIntent);
        }
    }

    private void resolvedGetDefaultListDeactivateSim(boolean z, PendingIntent pendingIntent) {
        if (z) {
            EuiccController.get().getDefaultDownloadableSubscriptionList(true, this.mCallingPackage, pendingIntent);
        } else {
            fail(pendingIntent);
        }
    }

    private void resolvedSwitchDeactivateSim(boolean z, PendingIntent pendingIntent) {
        if (z) {
            EuiccController.get().switchToSubscription(this.mSubscriptionId, true, this.mCallingPackage, pendingIntent);
        } else {
            fail(pendingIntent);
        }
    }

    private void resolvedSwitchNoPrivileges(boolean z, PendingIntent pendingIntent) {
        if (z) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                EuiccController.get().switchToSubscriptionPrivileged(jClearCallingIdentity, this.mSubscriptionId, true, this.mCallingPackage, pendingIntent);
                return;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        fail(pendingIntent);
    }

    private static void fail(PendingIntent pendingIntent) {
        EuiccController.get().sendResult(pendingIntent, 2, null);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
