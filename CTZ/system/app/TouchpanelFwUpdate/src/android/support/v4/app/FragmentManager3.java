package android.support.v4.app;

import android.os.Parcel;
import android.os.Parcelable;

/* compiled from: FragmentManager.java */
/* renamed from: android.support.v4.app.FragmentManagerState, reason: use source file name */
/* loaded from: classes.dex */
final class FragmentManager3 implements Parcelable {
    public static final Parcelable.Creator<FragmentManager3> CREATOR = new Parcelable.Creator<FragmentManager3>() { // from class: android.support.v4.app.FragmentManagerState.1
        /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public FragmentManager3 createFromParcel(Parcel in) {
            return new FragmentManager3(in);
        }

        /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public FragmentManager3[] newArray(int size) {
            return new FragmentManager3[size];
        }
    };
    FragmentState[] mActive;
    int[] mAdded;
    BackStackRecord2[] mBackStack;
    int mNextFragmentIndex;
    int mPrimaryNavActiveIndex;

    public FragmentManager3() {
        this.mPrimaryNavActiveIndex = -1;
    }

    public FragmentManager3(Parcel in) {
        this.mPrimaryNavActiveIndex = -1;
        this.mActive = (FragmentState[]) in.createTypedArray(FragmentState.CREATOR);
        this.mAdded = in.createIntArray();
        this.mBackStack = (BackStackRecord2[]) in.createTypedArray(BackStackRecord2.CREATOR);
        this.mPrimaryNavActiveIndex = in.readInt();
        this.mNextFragmentIndex = in.readInt();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedArray(this.mActive, flags);
        dest.writeIntArray(this.mAdded);
        dest.writeTypedArray(this.mBackStack, flags);
        dest.writeInt(this.mPrimaryNavActiveIndex);
        dest.writeInt(this.mNextFragmentIndex);
    }
}
