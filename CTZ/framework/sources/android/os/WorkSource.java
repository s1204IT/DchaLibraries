package android.os;

import android.annotation.SystemApi;
import android.content.Context;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class WorkSource implements Parcelable {
    static final boolean DEBUG = false;
    static final String TAG = "WorkSource";
    static WorkSource sGoneWork;
    static WorkSource sNewbWork;
    private ArrayList<WorkChain> mChains;
    String[] mNames;
    int mNum;
    int[] mUids;
    static final WorkSource sTmpWorkSource = new WorkSource(0);
    public static final Parcelable.Creator<WorkSource> CREATOR = new Parcelable.Creator<WorkSource>() {
        @Override
        public WorkSource createFromParcel(Parcel parcel) {
            return new WorkSource(parcel);
        }

        @Override
        public WorkSource[] newArray(int i) {
            return new WorkSource[i];
        }
    };

    public WorkSource() {
        this.mNum = 0;
        this.mChains = null;
    }

    public WorkSource(WorkSource workSource) {
        if (workSource == null) {
            this.mNum = 0;
            this.mChains = null;
            return;
        }
        this.mNum = workSource.mNum;
        if (workSource.mUids != null) {
            this.mUids = (int[]) workSource.mUids.clone();
            this.mNames = workSource.mNames != null ? (String[]) workSource.mNames.clone() : null;
        } else {
            this.mUids = null;
            this.mNames = null;
        }
        if (workSource.mChains != null) {
            this.mChains = new ArrayList<>(workSource.mChains.size());
            Iterator<WorkChain> it = workSource.mChains.iterator();
            while (it.hasNext()) {
                this.mChains.add(new WorkChain(it.next()));
            }
            return;
        }
        this.mChains = null;
    }

    public WorkSource(int i) {
        this.mNum = 1;
        this.mUids = new int[]{i, 0};
        this.mNames = null;
        this.mChains = null;
    }

    public WorkSource(int i, String str) {
        if (str == null) {
            throw new NullPointerException("Name can't be null");
        }
        this.mNum = 1;
        this.mUids = new int[]{i, 0};
        this.mNames = new String[]{str, null};
        this.mChains = null;
    }

    WorkSource(Parcel parcel) {
        this.mNum = parcel.readInt();
        this.mUids = parcel.createIntArray();
        this.mNames = parcel.createStringArray();
        int i = parcel.readInt();
        if (i > 0) {
            this.mChains = new ArrayList<>(i);
            parcel.readParcelableList(this.mChains, WorkChain.class.getClassLoader());
        } else {
            this.mChains = null;
        }
    }

    public static boolean isChainedBatteryAttributionEnabled(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.CHAINED_BATTERY_ATTRIBUTION_ENABLED, 0) == 1;
    }

    public int size() {
        return this.mNum;
    }

    public int get(int i) {
        return this.mUids[i];
    }

    public String getName(int i) {
        if (this.mNames != null) {
            return this.mNames[i];
        }
        return null;
    }

    public void clearNames() {
        if (this.mNames != null) {
            this.mNames = null;
            int i = this.mNum;
            int i2 = 1;
            for (int i3 = 1; i3 < this.mNum; i3++) {
                if (this.mUids[i3] == this.mUids[i3 - 1]) {
                    i--;
                } else {
                    this.mUids[i2] = this.mUids[i3];
                    i2++;
                }
            }
            this.mNum = i;
        }
    }

    public void clear() {
        this.mNum = 0;
        if (this.mChains != null) {
            this.mChains.clear();
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof WorkSource)) {
            return false;
        }
        WorkSource workSource = (WorkSource) obj;
        if (diff(workSource)) {
            return false;
        }
        if (this.mChains == null || this.mChains.isEmpty()) {
            return workSource.mChains == null || workSource.mChains.isEmpty();
        }
        return this.mChains.equals(workSource.mChains);
    }

    public int hashCode() {
        int iHashCode = 0;
        for (int i = 0; i < this.mNum; i++) {
            iHashCode = ((iHashCode >>> 28) | (iHashCode << 4)) ^ this.mUids[i];
        }
        if (this.mNames != null) {
            for (int i2 = 0; i2 < this.mNum; i2++) {
                iHashCode = this.mNames[i2].hashCode() ^ ((iHashCode << 4) | (iHashCode >>> 28));
            }
        }
        if (this.mChains != null) {
            return ((iHashCode << 4) | (iHashCode >>> 28)) ^ this.mChains.hashCode();
        }
        return iHashCode;
    }

    public boolean diff(WorkSource workSource) {
        int i = this.mNum;
        if (i != workSource.mNum) {
            return true;
        }
        int[] iArr = this.mUids;
        int[] iArr2 = workSource.mUids;
        String[] strArr = this.mNames;
        String[] strArr2 = workSource.mNames;
        for (int i2 = 0; i2 < i; i2++) {
            if (iArr[i2] != iArr2[i2]) {
                return true;
            }
            if (strArr != null && strArr2 != null && !strArr[i2].equals(strArr2[i2])) {
                return true;
            }
        }
        return false;
    }

    public void set(WorkSource workSource) {
        if (workSource == null) {
            this.mNum = 0;
            if (this.mChains != null) {
                this.mChains.clear();
                return;
            }
            return;
        }
        this.mNum = workSource.mNum;
        if (workSource.mUids != null) {
            if (this.mUids != null && this.mUids.length >= this.mNum) {
                System.arraycopy(workSource.mUids, 0, this.mUids, 0, this.mNum);
            } else {
                this.mUids = (int[]) workSource.mUids.clone();
            }
            if (workSource.mNames != null) {
                if (this.mNames != null && this.mNames.length >= this.mNum) {
                    System.arraycopy(workSource.mNames, 0, this.mNames, 0, this.mNum);
                } else {
                    this.mNames = (String[]) workSource.mNames.clone();
                }
            } else {
                this.mNames = null;
            }
        } else {
            this.mUids = null;
            this.mNames = null;
        }
        if (workSource.mChains != null) {
            if (this.mChains != null) {
                this.mChains.clear();
            } else {
                this.mChains = new ArrayList<>(workSource.mChains.size());
            }
            Iterator<WorkChain> it = workSource.mChains.iterator();
            while (it.hasNext()) {
                this.mChains.add(new WorkChain(it.next()));
            }
        }
    }

    public void set(int i) {
        this.mNum = 1;
        if (this.mUids == null) {
            this.mUids = new int[2];
        }
        this.mUids[0] = i;
        this.mNames = null;
        if (this.mChains != null) {
            this.mChains.clear();
        }
    }

    public void set(int i, String str) {
        if (str == null) {
            throw new NullPointerException("Name can't be null");
        }
        this.mNum = 1;
        if (this.mUids == null) {
            this.mUids = new int[2];
            this.mNames = new String[2];
        }
        this.mUids[0] = i;
        this.mNames[0] = str;
        if (this.mChains != null) {
            this.mChains.clear();
        }
    }

    @Deprecated
    public WorkSource[] setReturningDiffs(WorkSource workSource) {
        synchronized (sTmpWorkSource) {
            sNewbWork = null;
            sGoneWork = null;
            updateLocked(workSource, true, true);
            if (sNewbWork == null && sGoneWork == null) {
                return null;
            }
            return new WorkSource[]{sNewbWork, sGoneWork};
        }
    }

    public boolean add(WorkSource workSource) {
        boolean z;
        synchronized (sTmpWorkSource) {
            z = false;
            boolean zUpdateLocked = updateLocked(workSource, false, false);
            if (workSource.mChains != null) {
                if (this.mChains == null) {
                    this.mChains = new ArrayList<>(workSource.mChains.size());
                }
                for (WorkChain workChain : workSource.mChains) {
                    if (!this.mChains.contains(workChain)) {
                        this.mChains.add(new WorkChain(workChain));
                    }
                }
            }
            if (zUpdateLocked) {
                z = true;
            }
        }
        return z;
    }

    @Deprecated
    public WorkSource addReturningNewbs(WorkSource workSource) {
        WorkSource workSource2;
        synchronized (sTmpWorkSource) {
            sNewbWork = null;
            updateLocked(workSource, false, true);
            workSource2 = sNewbWork;
        }
        return workSource2;
    }

    public boolean add(int i) {
        if (this.mNum <= 0) {
            this.mNames = null;
            insert(0, i);
            return true;
        }
        if (this.mNames != null) {
            throw new IllegalArgumentException("Adding without name to named " + this);
        }
        int iBinarySearch = Arrays.binarySearch(this.mUids, 0, this.mNum, i);
        if (iBinarySearch >= 0) {
            return false;
        }
        insert((-iBinarySearch) - 1, i);
        return true;
    }

    public boolean add(int i, String str) {
        if (this.mNum <= 0) {
            insert(0, i, str);
            return true;
        }
        if (this.mNames == null) {
            throw new IllegalArgumentException("Adding name to unnamed " + this);
        }
        int i2 = 0;
        while (i2 < this.mNum && this.mUids[i2] <= i) {
            if (this.mUids[i2] == i) {
                int iCompareTo = this.mNames[i2].compareTo(str);
                if (iCompareTo > 0) {
                    break;
                }
                if (iCompareTo == 0) {
                    return false;
                }
            }
            i2++;
        }
        insert(i2, i, str);
        return true;
    }

    public boolean remove(WorkSource workSource) {
        boolean zRemoveUidsAndNames;
        boolean zRemoveAll;
        if (isEmpty() || workSource.isEmpty()) {
            return false;
        }
        if (this.mNames == null && workSource.mNames == null) {
            zRemoveUidsAndNames = removeUids(workSource);
        } else {
            if (this.mNames == null) {
                throw new IllegalArgumentException("Other " + workSource + " has names, but target " + this + " does not");
            }
            if (workSource.mNames == null) {
                throw new IllegalArgumentException("Target " + this + " has names, but other " + workSource + " does not");
            }
            zRemoveUidsAndNames = removeUidsAndNames(workSource);
        }
        if (workSource.mChains != null && this.mChains != null) {
            zRemoveAll = this.mChains.removeAll(workSource.mChains);
        } else {
            zRemoveAll = false;
        }
        return zRemoveUidsAndNames || zRemoveAll;
    }

    @SystemApi
    public WorkChain createWorkChain() {
        if (this.mChains == null) {
            this.mChains = new ArrayList<>(4);
        }
        WorkChain workChain = new WorkChain();
        this.mChains.add(workChain);
        return workChain;
    }

    public boolean isEmpty() {
        return this.mNum == 0 && (this.mChains == null || this.mChains.isEmpty());
    }

    public ArrayList<WorkChain> getWorkChains() {
        return this.mChains;
    }

    public void transferWorkChains(WorkSource workSource) {
        if (this.mChains != null) {
            this.mChains.clear();
        }
        if (workSource.mChains == null || workSource.mChains.isEmpty()) {
            return;
        }
        if (this.mChains == null) {
            this.mChains = new ArrayList<>(4);
        }
        this.mChains.addAll(workSource.mChains);
        workSource.mChains.clear();
    }

    private boolean removeUids(WorkSource workSource) {
        int i = this.mNum;
        int[] iArr = this.mUids;
        int i2 = workSource.mNum;
        int[] iArr2 = workSource.mUids;
        int i3 = 0;
        int i4 = 0;
        boolean z = false;
        while (i3 < i && i4 < i2) {
            if (iArr2[i4] == iArr[i3]) {
                i--;
                if (i3 < i) {
                    System.arraycopy(iArr, i3 + 1, iArr, i3, i - i3);
                }
                i4++;
                z = true;
            } else if (iArr2[i4] > iArr[i3]) {
                i3++;
            } else {
                i4++;
            }
        }
        this.mNum = i;
        return z;
    }

    private boolean removeUidsAndNames(WorkSource workSource) {
        int i = this.mNum;
        int[] iArr = this.mUids;
        String[] strArr = this.mNames;
        int i2 = workSource.mNum;
        int[] iArr2 = workSource.mUids;
        String[] strArr2 = workSource.mNames;
        int i3 = 0;
        int i4 = 0;
        boolean z = false;
        while (i3 < i && i4 < i2) {
            if (iArr2[i4] == iArr[i3] && strArr2[i4].equals(strArr[i3])) {
                i--;
                if (i3 < i) {
                    int i5 = i3 + 1;
                    int i6 = i - i3;
                    System.arraycopy(iArr, i5, iArr, i3, i6);
                    System.arraycopy(strArr, i5, strArr, i3, i6);
                }
                i4++;
                z = true;
            } else if (iArr2[i4] > iArr[i3] || (iArr2[i4] == iArr[i3] && strArr2[i4].compareTo(strArr[i3]) > 0)) {
                i3++;
            } else {
                i4++;
            }
        }
        this.mNum = i;
        return z;
    }

    private boolean updateLocked(WorkSource workSource, boolean z, boolean z2) {
        if (this.mNames == null && workSource.mNames == null) {
            return updateUidsLocked(workSource, z, z2);
        }
        if (this.mNum > 0 && this.mNames == null) {
            throw new IllegalArgumentException("Other " + workSource + " has names, but target " + this + " does not");
        }
        if (workSource.mNum > 0 && workSource.mNames == null) {
            throw new IllegalArgumentException("Target " + this + " has names, but other " + workSource + " does not");
        }
        return updateUidsAndNamesLocked(workSource, z, z2);
    }

    private static WorkSource addWork(WorkSource workSource, int i) {
        if (workSource == null) {
            return new WorkSource(i);
        }
        workSource.insert(workSource.mNum, i);
        return workSource;
    }

    private boolean updateUidsLocked(WorkSource workSource, boolean z, boolean z2) {
        int i = this.mNum;
        int[] iArr = this.mUids;
        int i2 = workSource.mNum;
        int[] iArr2 = workSource.mUids;
        int[] iArr3 = iArr;
        int i3 = 0;
        boolean z3 = false;
        int i4 = i;
        int i5 = 0;
        while (true) {
            if (i5 < i4 || i3 < i2) {
                if (i5 >= i4 || (i3 < i2 && iArr2[i3] < iArr3[i5])) {
                    if (iArr3 == null) {
                        iArr3 = new int[4];
                        iArr3[0] = iArr2[i3];
                    } else if (i4 >= iArr3.length) {
                        int[] iArr4 = new int[(iArr3.length * 3) / 2];
                        if (i5 > 0) {
                            System.arraycopy(iArr3, 0, iArr4, 0, i5);
                        }
                        if (i5 < i4) {
                            System.arraycopy(iArr3, i5, iArr4, i5 + 1, i4 - i5);
                        }
                        iArr4[i5] = iArr2[i3];
                        iArr3 = iArr4;
                    } else {
                        if (i5 < i4) {
                            System.arraycopy(iArr3, i5, iArr3, i5 + 1, i4 - i5);
                        }
                        iArr3[i5] = iArr2[i3];
                    }
                    if (z2) {
                        sNewbWork = addWork(sNewbWork, iArr2[i3]);
                    }
                    i4++;
                    i5++;
                    i3++;
                    z3 = true;
                } else if (!z) {
                    if (i3 < i2 && iArr2[i3] == iArr3[i5]) {
                        i3++;
                    }
                    i5++;
                } else {
                    int i6 = i5;
                    while (i6 < i4 && (i3 >= i2 || iArr2[i3] > iArr3[i6])) {
                        sGoneWork = addWork(sGoneWork, iArr3[i6]);
                        i6++;
                    }
                    if (i5 < i6) {
                        System.arraycopy(iArr3, i6, iArr3, i5, i4 - i6);
                        i4 -= i6 - i5;
                    } else {
                        i5 = i6;
                    }
                    if (i5 < i4 && i3 < i2 && iArr2[i3] == iArr3[i5]) {
                        i5++;
                        i3++;
                    }
                }
            } else {
                this.mNum = i4;
                this.mUids = iArr3;
                return z3;
            }
        }
    }

    private int compare(WorkSource workSource, int i, int i2) {
        int i3 = this.mUids[i] - workSource.mUids[i2];
        if (i3 != 0) {
            return i3;
        }
        return this.mNames[i].compareTo(workSource.mNames[i2]);
    }

    private static WorkSource addWork(WorkSource workSource, int i, String str) {
        if (workSource == null) {
            return new WorkSource(i, str);
        }
        workSource.insert(workSource.mNum, i, str);
        return workSource;
    }

    private boolean updateUidsAndNamesLocked(WorkSource workSource, boolean z, boolean z2) {
        int iCompare;
        int i = workSource.mNum;
        int[] iArr = workSource.mUids;
        String[] strArr = workSource.mNames;
        int i2 = 0;
        int i3 = 0;
        boolean z3 = false;
        while (true) {
            if (i2 < this.mNum || i3 < i) {
                if (i2 < this.mNum) {
                    if (i3 < i) {
                        iCompare = compare(workSource, i2, i3);
                        if (iCompare > 0) {
                        }
                    } else {
                        iCompare = -1;
                    }
                    if (!z) {
                        if (i3 < i && iCompare == 0) {
                            i3++;
                        }
                        i2++;
                    } else {
                        int i4 = i2;
                        while (iCompare < 0) {
                            sGoneWork = addWork(sGoneWork, this.mUids[i4], this.mNames[i4]);
                            i4++;
                            if (i4 >= this.mNum) {
                                break;
                            }
                            iCompare = i3 < i ? compare(workSource, i4, i3) : -1;
                        }
                        if (i2 < i4) {
                            System.arraycopy(this.mUids, i4, this.mUids, i2, this.mNum - i4);
                            System.arraycopy(this.mNames, i4, this.mNames, i2, this.mNum - i4);
                            this.mNum -= i4 - i2;
                        } else {
                            i2 = i4;
                        }
                        if (i2 < this.mNum && iCompare == 0) {
                            i2++;
                            i3++;
                        }
                    }
                }
                insert(i2, iArr[i3], strArr[i3]);
                if (z2) {
                    sNewbWork = addWork(sNewbWork, iArr[i3], strArr[i3]);
                }
                i2++;
                i3++;
                z3 = true;
            } else {
                return z3;
            }
        }
    }

    private void insert(int i, int i2) {
        if (this.mUids == null) {
            this.mUids = new int[4];
            this.mUids[0] = i2;
            this.mNum = 1;
        } else {
            if (this.mNum >= this.mUids.length) {
                int[] iArr = new int[(this.mNum * 3) / 2];
                if (i > 0) {
                    System.arraycopy(this.mUids, 0, iArr, 0, i);
                }
                if (i < this.mNum) {
                    System.arraycopy(this.mUids, i, iArr, i + 1, this.mNum - i);
                }
                this.mUids = iArr;
                this.mUids[i] = i2;
                this.mNum++;
                return;
            }
            if (i < this.mNum) {
                System.arraycopy(this.mUids, i, this.mUids, i + 1, this.mNum - i);
            }
            this.mUids[i] = i2;
            this.mNum++;
        }
    }

    private void insert(int i, int i2, String str) {
        if (this.mUids == null) {
            this.mUids = new int[4];
            this.mUids[0] = i2;
            this.mNames = new String[4];
            this.mNames[0] = str;
            this.mNum = 1;
            return;
        }
        if (this.mNum >= this.mUids.length) {
            int[] iArr = new int[(this.mNum * 3) / 2];
            String[] strArr = new String[(this.mNum * 3) / 2];
            if (i > 0) {
                System.arraycopy(this.mUids, 0, iArr, 0, i);
                System.arraycopy(this.mNames, 0, strArr, 0, i);
            }
            if (i < this.mNum) {
                int i3 = i + 1;
                System.arraycopy(this.mUids, i, iArr, i3, this.mNum - i);
                System.arraycopy(this.mNames, i, strArr, i3, this.mNum - i);
            }
            this.mUids = iArr;
            this.mNames = strArr;
            this.mUids[i] = i2;
            this.mNames[i] = str;
            this.mNum++;
            return;
        }
        if (i < this.mNum) {
            int i4 = i + 1;
            System.arraycopy(this.mUids, i, this.mUids, i4, this.mNum - i);
            System.arraycopy(this.mNames, i, this.mNames, i4, this.mNum - i);
        }
        this.mUids[i] = i2;
        this.mNames[i] = str;
        this.mNum++;
    }

    @SystemApi
    public static final class WorkChain implements Parcelable {
        public static final Parcelable.Creator<WorkChain> CREATOR = new Parcelable.Creator<WorkChain>() {
            @Override
            public WorkChain createFromParcel(Parcel parcel) {
                return new WorkChain(parcel);
            }

            @Override
            public WorkChain[] newArray(int i) {
                return new WorkChain[i];
            }
        };
        private int mSize;
        private String[] mTags;
        private int[] mUids;

        public WorkChain() {
            this.mSize = 0;
            this.mUids = new int[4];
            this.mTags = new String[4];
        }

        @VisibleForTesting
        public WorkChain(WorkChain workChain) {
            this.mSize = workChain.mSize;
            this.mUids = (int[]) workChain.mUids.clone();
            this.mTags = (String[]) workChain.mTags.clone();
        }

        private WorkChain(Parcel parcel) {
            this.mSize = parcel.readInt();
            this.mUids = parcel.createIntArray();
            this.mTags = parcel.createStringArray();
        }

        public WorkChain addNode(int i, String str) {
            if (this.mSize == this.mUids.length) {
                resizeArrays();
            }
            this.mUids[this.mSize] = i;
            this.mTags[this.mSize] = str;
            this.mSize++;
            return this;
        }

        public int getAttributionUid() {
            return this.mUids[0];
        }

        public String getAttributionTag() {
            return this.mTags[0];
        }

        @VisibleForTesting
        public int[] getUids() {
            int[] iArr = new int[this.mSize];
            System.arraycopy(this.mUids, 0, iArr, 0, this.mSize);
            return iArr;
        }

        @VisibleForTesting
        public String[] getTags() {
            String[] strArr = new String[this.mSize];
            System.arraycopy(this.mTags, 0, strArr, 0, this.mSize);
            return strArr;
        }

        @VisibleForTesting
        public int getSize() {
            return this.mSize;
        }

        private void resizeArrays() {
            int i = this.mSize * 2;
            int[] iArr = new int[i];
            String[] strArr = new String[i];
            System.arraycopy(this.mUids, 0, iArr, 0, this.mSize);
            System.arraycopy(this.mTags, 0, strArr, 0, this.mSize);
            this.mUids = iArr;
            this.mTags = strArr;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("WorkChain{");
            for (int i = 0; i < this.mSize; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append("(");
                sb.append(this.mUids[i]);
                if (this.mTags[i] != null) {
                    sb.append(", ");
                    sb.append(this.mTags[i]);
                }
                sb.append(")");
            }
            sb.append("}");
            return sb.toString();
        }

        public int hashCode() {
            return ((this.mSize + (Arrays.hashCode(this.mUids) * 31)) * 31) + Arrays.hashCode(this.mTags);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof WorkChain)) {
                return false;
            }
            WorkChain workChain = (WorkChain) obj;
            return this.mSize == workChain.mSize && Arrays.equals(this.mUids, workChain.mUids) && Arrays.equals(this.mTags, workChain.mTags);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.mSize);
            parcel.writeIntArray(this.mUids);
            parcel.writeStringArray(this.mTags);
        }
    }

    public static ArrayList<WorkChain>[] diffChains(WorkSource workSource, WorkSource workSource2) {
        ArrayList<WorkChain> arrayList;
        ArrayList<WorkChain> arrayList2;
        if (workSource.mChains != null) {
            arrayList = null;
            for (int i = 0; i < workSource.mChains.size(); i++) {
                WorkChain workChain = workSource.mChains.get(i);
                if (workSource2.mChains == null || !workSource2.mChains.contains(workChain)) {
                    if (arrayList == null) {
                        arrayList = new ArrayList<>(workSource.mChains.size());
                    }
                    arrayList.add(workChain);
                }
            }
        } else {
            arrayList = null;
        }
        if (workSource2.mChains != null) {
            arrayList2 = null;
            for (int i2 = 0; i2 < workSource2.mChains.size(); i2++) {
                WorkChain workChain2 = workSource2.mChains.get(i2);
                if (workSource.mChains == null || !workSource.mChains.contains(workChain2)) {
                    if (arrayList2 == null) {
                        arrayList2 = new ArrayList<>(workSource2.mChains.size());
                    }
                    arrayList2.add(workChain2);
                }
            }
        } else {
            arrayList2 = null;
        }
        if (arrayList2 == null && arrayList == null) {
            return null;
        }
        return new ArrayList[]{arrayList2, arrayList};
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mNum);
        parcel.writeIntArray(this.mUids);
        parcel.writeStringArray(this.mNames);
        if (this.mChains == null) {
            parcel.writeInt(-1);
        } else {
            parcel.writeInt(this.mChains.size());
            parcel.writeParcelableList(this.mChains, i);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WorkSource{");
        for (int i = 0; i < this.mNum; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(this.mUids[i]);
            if (this.mNames != null) {
                sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                sb.append(this.mNames[i]);
            }
        }
        if (this.mChains != null) {
            sb.append(" chains=");
            for (int i2 = 0; i2 < this.mChains.size(); i2++) {
                if (i2 != 0) {
                    sb.append(", ");
                }
                sb.append(this.mChains.get(i2));
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long j2;
        long j3;
        long jStart = protoOutputStream.start(j);
        int i = 0;
        while (true) {
            j2 = 1120986464257L;
            j3 = 2246267895809L;
            if (i >= this.mNum) {
                break;
            }
            long jStart2 = protoOutputStream.start(2246267895809L);
            protoOutputStream.write(1120986464257L, this.mUids[i]);
            if (this.mNames != null) {
                protoOutputStream.write(1138166333442L, this.mNames[i]);
            }
            protoOutputStream.end(jStart2);
            i++;
        }
        if (this.mChains != null) {
            int i2 = 0;
            while (i2 < this.mChains.size()) {
                WorkChain workChain = this.mChains.get(i2);
                long jStart3 = protoOutputStream.start(2246267895810L);
                String[] tags = workChain.getTags();
                int[] uids = workChain.getUids();
                int i3 = 0;
                while (i3 < tags.length) {
                    long jStart4 = protoOutputStream.start(j3);
                    protoOutputStream.write(j2, uids[i3]);
                    protoOutputStream.write(1138166333442L, tags[i3]);
                    protoOutputStream.end(jStart4);
                    i3++;
                    j2 = 1120986464257L;
                    j3 = 2246267895809L;
                }
                protoOutputStream.end(jStart3);
                i2++;
                j2 = 1120986464257L;
                j3 = 2246267895809L;
            }
        }
        protoOutputStream.end(jStart);
    }
}
