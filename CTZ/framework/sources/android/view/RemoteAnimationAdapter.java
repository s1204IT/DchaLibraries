package android.view;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.IRemoteAnimationRunner;

public class RemoteAnimationAdapter implements Parcelable {
    public static final Parcelable.Creator<RemoteAnimationAdapter> CREATOR = new Parcelable.Creator<RemoteAnimationAdapter>() {
        @Override
        public RemoteAnimationAdapter createFromParcel(Parcel parcel) {
            return new RemoteAnimationAdapter(parcel);
        }

        @Override
        public RemoteAnimationAdapter[] newArray(int i) {
            return new RemoteAnimationAdapter[i];
        }
    };
    private int mCallingPid;
    private final long mDuration;
    private final IRemoteAnimationRunner mRunner;
    private final long mStatusBarTransitionDelay;

    public RemoteAnimationAdapter(IRemoteAnimationRunner iRemoteAnimationRunner, long j, long j2) {
        this.mRunner = iRemoteAnimationRunner;
        this.mDuration = j;
        this.mStatusBarTransitionDelay = j2;
    }

    public RemoteAnimationAdapter(Parcel parcel) {
        this.mRunner = IRemoteAnimationRunner.Stub.asInterface(parcel.readStrongBinder());
        this.mDuration = parcel.readLong();
        this.mStatusBarTransitionDelay = parcel.readLong();
    }

    public IRemoteAnimationRunner getRunner() {
        return this.mRunner;
    }

    public long getDuration() {
        return this.mDuration;
    }

    public long getStatusBarTransitionDelay() {
        return this.mStatusBarTransitionDelay;
    }

    public void setCallingPid(int i) {
        this.mCallingPid = i;
    }

    public int getCallingPid() {
        return this.mCallingPid;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStrongInterface(this.mRunner);
        parcel.writeLong(this.mDuration);
        parcel.writeLong(this.mStatusBarTransitionDelay);
    }
}
