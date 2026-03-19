package android.content;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;

public class SyncStatusInfo implements Parcelable {
    public static final Parcelable.Creator<SyncStatusInfo> CREATOR = new Parcelable.Creator<SyncStatusInfo>() {
        @Override
        public SyncStatusInfo createFromParcel(Parcel in) {
            return new SyncStatusInfo(in);
        }

        @Override
        public SyncStatusInfo[] newArray(int size) {
            return new SyncStatusInfo[size];
        }
    };
    private static final String TAG = "Sync";
    static final int VERSION = 2;
    public final int authorityId;
    public long initialFailureTime;
    public boolean initialize;
    public String lastFailureMesg;
    public int lastFailureSource;
    public long lastFailureTime;
    public int lastSuccessSource;
    public long lastSuccessTime;
    public int numSourceLocal;
    public int numSourcePeriodic;
    public int numSourcePoll;
    public int numSourceServer;
    public int numSourceUser;
    public int numSyncs;
    public boolean pending;
    private ArrayList<Long> periodicSyncTimes;
    public long totalElapsedTime;

    public SyncStatusInfo(int authorityId) {
        this.authorityId = authorityId;
    }

    public int getLastFailureMesgAsInt(int def) {
        int i = ContentResolver.syncErrorStringToInt(this.lastFailureMesg);
        if (i > 0) {
            return i;
        }
        Log.d(TAG, "Unknown lastFailureMesg:" + this.lastFailureMesg);
        return def;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(2);
        parcel.writeInt(this.authorityId);
        parcel.writeLong(this.totalElapsedTime);
        parcel.writeInt(this.numSyncs);
        parcel.writeInt(this.numSourcePoll);
        parcel.writeInt(this.numSourceServer);
        parcel.writeInt(this.numSourceLocal);
        parcel.writeInt(this.numSourceUser);
        parcel.writeLong(this.lastSuccessTime);
        parcel.writeInt(this.lastSuccessSource);
        parcel.writeLong(this.lastFailureTime);
        parcel.writeInt(this.lastFailureSource);
        parcel.writeString(this.lastFailureMesg);
        parcel.writeLong(this.initialFailureTime);
        parcel.writeInt(this.pending ? 1 : 0);
        parcel.writeInt(this.initialize ? 1 : 0);
        if (this.periodicSyncTimes != null) {
            parcel.writeInt(this.periodicSyncTimes.size());
            Iterator periodicSyncTime$iterator = this.periodicSyncTimes.iterator();
            while (periodicSyncTime$iterator.hasNext()) {
                long periodicSyncTime = ((Long) periodicSyncTime$iterator.next()).longValue();
                parcel.writeLong(periodicSyncTime);
            }
            return;
        }
        parcel.writeInt(-1);
    }

    public SyncStatusInfo(Parcel parcel) {
        int version = parcel.readInt();
        if (version != 2 && version != 1) {
            Log.w("SyncStatusInfo", "Unknown version: " + version);
        }
        this.authorityId = parcel.readInt();
        this.totalElapsedTime = parcel.readLong();
        this.numSyncs = parcel.readInt();
        this.numSourcePoll = parcel.readInt();
        this.numSourceServer = parcel.readInt();
        this.numSourceLocal = parcel.readInt();
        this.numSourceUser = parcel.readInt();
        this.lastSuccessTime = parcel.readLong();
        this.lastSuccessSource = parcel.readInt();
        this.lastFailureTime = parcel.readLong();
        this.lastFailureSource = parcel.readInt();
        this.lastFailureMesg = parcel.readString();
        this.initialFailureTime = parcel.readLong();
        this.pending = parcel.readInt() != 0;
        this.initialize = parcel.readInt() != 0;
        if (version == 1) {
            this.periodicSyncTimes = null;
            return;
        }
        int N = parcel.readInt();
        if (N < 0) {
            this.periodicSyncTimes = null;
            return;
        }
        this.periodicSyncTimes = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            this.periodicSyncTimes.add(Long.valueOf(parcel.readLong()));
        }
    }

    public SyncStatusInfo(SyncStatusInfo other) {
        this.authorityId = other.authorityId;
        this.totalElapsedTime = other.totalElapsedTime;
        this.numSyncs = other.numSyncs;
        this.numSourcePoll = other.numSourcePoll;
        this.numSourceServer = other.numSourceServer;
        this.numSourceLocal = other.numSourceLocal;
        this.numSourceUser = other.numSourceUser;
        this.numSourcePeriodic = other.numSourcePeriodic;
        this.lastSuccessTime = other.lastSuccessTime;
        this.lastSuccessSource = other.lastSuccessSource;
        this.lastFailureTime = other.lastFailureTime;
        this.lastFailureSource = other.lastFailureSource;
        this.lastFailureMesg = other.lastFailureMesg;
        this.initialFailureTime = other.initialFailureTime;
        this.pending = other.pending;
        this.initialize = other.initialize;
        if (other.periodicSyncTimes == null) {
            return;
        }
        this.periodicSyncTimes = new ArrayList<>(other.periodicSyncTimes);
    }

    public void setPeriodicSyncTime(int index, long when) {
        synchronized (this) {
            ensurePeriodicSyncTimeSize(index);
            this.periodicSyncTimes.set(index, Long.valueOf(when));
        }
    }

    public long getPeriodicSyncTime(int index) {
        if (this.periodicSyncTimes != null && index < this.periodicSyncTimes.size()) {
            return this.periodicSyncTimes.get(index).longValue();
        }
        return 0L;
    }

    public void removePeriodicSyncTime(int index) {
        synchronized (this) {
            if (this.periodicSyncTimes != null && index < this.periodicSyncTimes.size()) {
                this.periodicSyncTimes.remove(index);
            }
        }
    }

    private void ensurePeriodicSyncTimeSize(int index) {
        if (this.periodicSyncTimes == null) {
            this.periodicSyncTimes = new ArrayList<>(0);
        }
        int requiredSize = index + 1;
        if (this.periodicSyncTimes.size() >= requiredSize) {
            return;
        }
        for (int i = this.periodicSyncTimes.size(); i < requiredSize; i++) {
            this.periodicSyncTimes.add(0L);
        }
    }
}
