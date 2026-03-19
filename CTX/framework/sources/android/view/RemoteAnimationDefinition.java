package android.view;

import android.app.WindowConfiguration;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;
import android.util.SparseArray;

public class RemoteAnimationDefinition implements Parcelable {
    public static final Parcelable.Creator<RemoteAnimationDefinition> CREATOR = new Parcelable.Creator<RemoteAnimationDefinition>() {
        @Override
        public RemoteAnimationDefinition createFromParcel(Parcel parcel) {
            return new RemoteAnimationDefinition(parcel);
        }

        @Override
        public RemoteAnimationDefinition[] newArray(int i) {
            return new RemoteAnimationDefinition[i];
        }
    };
    private final SparseArray<RemoteAnimationAdapterEntry> mTransitionAnimationMap;

    public RemoteAnimationDefinition() {
        this.mTransitionAnimationMap = new SparseArray<>();
    }

    public void addRemoteAnimation(int i, @WindowConfiguration.ActivityType int i2, RemoteAnimationAdapter remoteAnimationAdapter) {
        this.mTransitionAnimationMap.put(i, new RemoteAnimationAdapterEntry(remoteAnimationAdapter, i2));
    }

    public void addRemoteAnimation(int i, RemoteAnimationAdapter remoteAnimationAdapter) {
        addRemoteAnimation(i, 0, remoteAnimationAdapter);
    }

    public boolean hasTransition(int i, ArraySet<Integer> arraySet) {
        return getAdapter(i, arraySet) != null;
    }

    public RemoteAnimationAdapter getAdapter(int i, ArraySet<Integer> arraySet) {
        RemoteAnimationAdapterEntry remoteAnimationAdapterEntry = this.mTransitionAnimationMap.get(i);
        if (remoteAnimationAdapterEntry == null) {
            return null;
        }
        if (remoteAnimationAdapterEntry.activityTypeFilter != 0 && !arraySet.contains(Integer.valueOf(remoteAnimationAdapterEntry.activityTypeFilter))) {
            return null;
        }
        return remoteAnimationAdapterEntry.adapter;
    }

    public RemoteAnimationDefinition(Parcel parcel) {
        int i = parcel.readInt();
        this.mTransitionAnimationMap = new SparseArray<>(i);
        for (int i2 = 0; i2 < i; i2++) {
            this.mTransitionAnimationMap.put(parcel.readInt(), (RemoteAnimationAdapterEntry) parcel.readTypedObject(RemoteAnimationAdapterEntry.CREATOR));
        }
    }

    public void setCallingPid(int i) {
        for (int size = this.mTransitionAnimationMap.size() - 1; size >= 0; size--) {
            this.mTransitionAnimationMap.valueAt(size).adapter.setCallingPid(i);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        int size = this.mTransitionAnimationMap.size();
        parcel.writeInt(size);
        for (int i2 = 0; i2 < size; i2++) {
            parcel.writeInt(this.mTransitionAnimationMap.keyAt(i2));
            parcel.writeTypedObject(this.mTransitionAnimationMap.valueAt(i2), i);
        }
    }

    private static class RemoteAnimationAdapterEntry implements Parcelable {
        private static final Parcelable.Creator<RemoteAnimationAdapterEntry> CREATOR = new Parcelable.Creator<RemoteAnimationAdapterEntry>() {
            @Override
            public RemoteAnimationAdapterEntry createFromParcel(Parcel parcel) {
                return new RemoteAnimationAdapterEntry(parcel);
            }

            @Override
            public RemoteAnimationAdapterEntry[] newArray(int i) {
                return new RemoteAnimationAdapterEntry[i];
            }
        };

        @WindowConfiguration.ActivityType
        final int activityTypeFilter;
        final RemoteAnimationAdapter adapter;

        RemoteAnimationAdapterEntry(RemoteAnimationAdapter remoteAnimationAdapter, int i) {
            this.adapter = remoteAnimationAdapter;
            this.activityTypeFilter = i;
        }

        private RemoteAnimationAdapterEntry(Parcel parcel) {
            this.adapter = (RemoteAnimationAdapter) parcel.readParcelable(RemoteAnimationAdapter.class.getClassLoader());
            this.activityTypeFilter = parcel.readInt();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeParcelable(this.adapter, i);
            parcel.writeInt(this.activityTypeFilter);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }
}
