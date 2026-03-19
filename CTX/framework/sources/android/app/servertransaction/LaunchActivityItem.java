package android.app.servertransaction;

import android.app.ActivityThread;
import android.app.ClientTransactionHandler;
import android.app.ProfilerInfo;
import android.app.ResultInfo;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.os.BaseBundle;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.Trace;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.content.ReferrerIntent;
import com.android.internal.logging.nano.MetricsProto;
import java.util.List;
import java.util.Objects;

public class LaunchActivityItem extends ClientTransactionItem {
    public static final Parcelable.Creator<LaunchActivityItem> CREATOR = new Parcelable.Creator<LaunchActivityItem>() {
        @Override
        public LaunchActivityItem createFromParcel(Parcel parcel) {
            return new LaunchActivityItem(parcel);
        }

        @Override
        public LaunchActivityItem[] newArray(int i) {
            return new LaunchActivityItem[i];
        }
    };
    private CompatibilityInfo mCompatInfo;
    private Configuration mCurConfig;
    private int mIdent;
    private ActivityInfo mInfo;
    private Intent mIntent;
    private boolean mIsForward;
    private Configuration mOverrideConfig;
    private List<ReferrerIntent> mPendingNewIntents;
    private List<ResultInfo> mPendingResults;
    private PersistableBundle mPersistentState;
    private int mProcState;
    private ProfilerInfo mProfilerInfo;
    private String mReferrer;
    private Bundle mState;
    private IVoiceInteractor mVoiceInteractor;

    @Override
    public void preExecute(ClientTransactionHandler clientTransactionHandler, IBinder iBinder) {
        clientTransactionHandler.updateProcessState(this.mProcState, false);
        clientTransactionHandler.updatePendingConfiguration(this.mCurConfig);
    }

    @Override
    public void execute(ClientTransactionHandler clientTransactionHandler, IBinder iBinder, PendingTransactionActions pendingTransactionActions) {
        Trace.traceBegin(64L, "activityStart");
        clientTransactionHandler.handleLaunchActivity(new ActivityThread.ActivityClientRecord(iBinder, this.mIntent, this.mIdent, this.mInfo, this.mOverrideConfig, this.mCompatInfo, this.mReferrer, this.mVoiceInteractor, this.mState, this.mPersistentState, this.mPendingResults, this.mPendingNewIntents, this.mIsForward, this.mProfilerInfo, clientTransactionHandler), pendingTransactionActions, null);
        Trace.traceEnd(64L);
    }

    private LaunchActivityItem() {
    }

    public static LaunchActivityItem obtain(Intent intent, int i, ActivityInfo activityInfo, Configuration configuration, Configuration configuration2, CompatibilityInfo compatibilityInfo, String str, IVoiceInteractor iVoiceInteractor, int i2, Bundle bundle, PersistableBundle persistableBundle, List<ResultInfo> list, List<ReferrerIntent> list2, boolean z, ProfilerInfo profilerInfo) {
        LaunchActivityItem launchActivityItem = (LaunchActivityItem) ObjectPool.obtain(LaunchActivityItem.class);
        if (launchActivityItem == null) {
            launchActivityItem = new LaunchActivityItem();
        }
        setValues(launchActivityItem, intent, i, activityInfo, configuration, configuration2, compatibilityInfo, str, iVoiceInteractor, i2, bundle, persistableBundle, list, list2, z, profilerInfo);
        return launchActivityItem;
    }

    @Override
    public void recycle() {
        setValues(this, null, 0, null, null, null, null, null, null, 0, null, null, null, null, false, null);
        ObjectPool.recycle(this);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedObject(this.mIntent, i);
        parcel.writeInt(this.mIdent);
        parcel.writeTypedObject(this.mInfo, i);
        parcel.writeTypedObject(this.mCurConfig, i);
        parcel.writeTypedObject(this.mOverrideConfig, i);
        parcel.writeTypedObject(this.mCompatInfo, i);
        parcel.writeString(this.mReferrer);
        parcel.writeStrongInterface(this.mVoiceInteractor);
        parcel.writeInt(this.mProcState);
        parcel.writeBundle(this.mState);
        parcel.writePersistableBundle(this.mPersistentState);
        parcel.writeTypedList(this.mPendingResults, i);
        parcel.writeTypedList(this.mPendingNewIntents, i);
        parcel.writeBoolean(this.mIsForward);
        parcel.writeTypedObject(this.mProfilerInfo, i);
    }

    private LaunchActivityItem(Parcel parcel) {
        setValues(this, (Intent) parcel.readTypedObject(Intent.CREATOR), parcel.readInt(), (ActivityInfo) parcel.readTypedObject(ActivityInfo.CREATOR), (Configuration) parcel.readTypedObject(Configuration.CREATOR), (Configuration) parcel.readTypedObject(Configuration.CREATOR), (CompatibilityInfo) parcel.readTypedObject(CompatibilityInfo.CREATOR), parcel.readString(), IVoiceInteractor.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), parcel.readBundle(getClass().getClassLoader()), parcel.readPersistableBundle(getClass().getClassLoader()), parcel.createTypedArrayList(ResultInfo.CREATOR), parcel.createTypedArrayList(ReferrerIntent.CREATOR), parcel.readBoolean(), (ProfilerInfo) parcel.readTypedObject(ProfilerInfo.CREATOR));
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        LaunchActivityItem launchActivityItem = (LaunchActivityItem) obj;
        if (((this.mIntent == null && launchActivityItem.mIntent == null) || (this.mIntent != null && this.mIntent.filterEquals(launchActivityItem.mIntent))) && this.mIdent == launchActivityItem.mIdent && activityInfoEqual(launchActivityItem.mInfo) && Objects.equals(this.mCurConfig, launchActivityItem.mCurConfig) && Objects.equals(this.mOverrideConfig, launchActivityItem.mOverrideConfig) && Objects.equals(this.mCompatInfo, launchActivityItem.mCompatInfo) && Objects.equals(this.mReferrer, launchActivityItem.mReferrer) && this.mProcState == launchActivityItem.mProcState && areBundlesEqual(this.mState, launchActivityItem.mState) && areBundlesEqual(this.mPersistentState, launchActivityItem.mPersistentState) && Objects.equals(this.mPendingResults, launchActivityItem.mPendingResults) && Objects.equals(this.mPendingNewIntents, launchActivityItem.mPendingNewIntents) && this.mIsForward == launchActivityItem.mIsForward && Objects.equals(this.mProfilerInfo, launchActivityItem.mProfilerInfo)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (((((((((((((((((((((((MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + this.mIntent.filterHashCode()) * 31) + this.mIdent) * 31) + Objects.hashCode(this.mCurConfig)) * 31) + Objects.hashCode(this.mOverrideConfig)) * 31) + Objects.hashCode(this.mCompatInfo)) * 31) + Objects.hashCode(this.mReferrer)) * 31) + Objects.hashCode(Integer.valueOf(this.mProcState))) * 31) + (this.mState != null ? this.mState.size() : 0)) * 31) + (this.mPersistentState != null ? this.mPersistentState.size() : 0)) * 31) + Objects.hashCode(this.mPendingResults)) * 31) + Objects.hashCode(this.mPendingNewIntents)) * 31) + (this.mIsForward ? 1 : 0))) + Objects.hashCode(this.mProfilerInfo);
    }

    private boolean activityInfoEqual(ActivityInfo activityInfo) {
        return this.mInfo == null ? activityInfo == null : activityInfo != null && this.mInfo.flags == activityInfo.flags && this.mInfo.maxAspectRatio == activityInfo.maxAspectRatio && Objects.equals(this.mInfo.launchToken, activityInfo.launchToken) && Objects.equals(this.mInfo.getComponentName(), activityInfo.getComponentName());
    }

    private static boolean areBundlesEqual(BaseBundle baseBundle, BaseBundle baseBundle2) {
        if (baseBundle == null || baseBundle2 == null) {
            return baseBundle == baseBundle2;
        }
        if (baseBundle.size() != baseBundle2.size()) {
            return false;
        }
        for (String str : baseBundle.keySet()) {
            if (str != null && !Objects.equals(baseBundle.get(str), baseBundle2.get(str))) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return "LaunchActivityItem{intent=" + this.mIntent + ",ident=" + this.mIdent + ",info=" + this.mInfo + ",curConfig=" + this.mCurConfig + ",overrideConfig=" + this.mOverrideConfig + ",referrer=" + this.mReferrer + ",procState=" + this.mProcState + ",state=" + this.mState + ",persistentState=" + this.mPersistentState + ",pendingResults=" + this.mPendingResults + ",pendingNewIntents=" + this.mPendingNewIntents + ",profilerInfo=" + this.mProfilerInfo + "}";
    }

    private static void setValues(LaunchActivityItem launchActivityItem, Intent intent, int i, ActivityInfo activityInfo, Configuration configuration, Configuration configuration2, CompatibilityInfo compatibilityInfo, String str, IVoiceInteractor iVoiceInteractor, int i2, Bundle bundle, PersistableBundle persistableBundle, List<ResultInfo> list, List<ReferrerIntent> list2, boolean z, ProfilerInfo profilerInfo) {
        launchActivityItem.mIntent = intent;
        launchActivityItem.mIdent = i;
        launchActivityItem.mInfo = activityInfo;
        launchActivityItem.mCurConfig = configuration;
        launchActivityItem.mOverrideConfig = configuration2;
        launchActivityItem.mCompatInfo = compatibilityInfo;
        launchActivityItem.mReferrer = str;
        launchActivityItem.mVoiceInteractor = iVoiceInteractor;
        launchActivityItem.mProcState = i2;
        launchActivityItem.mState = bundle;
        launchActivityItem.mPersistentState = persistableBundle;
        launchActivityItem.mPendingResults = list;
        launchActivityItem.mPendingNewIntents = list2;
        launchActivityItem.mIsForward = z;
        launchActivityItem.mProfilerInfo = profilerInfo;
    }
}
