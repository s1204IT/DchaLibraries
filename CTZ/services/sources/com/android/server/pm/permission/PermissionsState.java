package com.android.server.pm.permission;

import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.util.ArrayUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class PermissionsState {
    private static final int[] NO_GIDS = new int[0];
    public static final int PERMISSION_OPERATION_FAILURE = -1;
    public static final int PERMISSION_OPERATION_SUCCESS = 0;
    public static final int PERMISSION_OPERATION_SUCCESS_GIDS_CHANGED = 1;
    private int[] mGlobalGids = NO_GIDS;
    private SparseBooleanArray mPermissionReviewRequired;
    private ArrayMap<String, PermissionData> mPermissions;

    public PermissionsState() {
    }

    public PermissionsState(PermissionsState permissionsState) {
        copyFrom(permissionsState);
    }

    public void setGlobalGids(int[] iArr) {
        if (!ArrayUtils.isEmpty(iArr)) {
            this.mGlobalGids = Arrays.copyOf(iArr, iArr.length);
        }
    }

    public void copyFrom(PermissionsState permissionsState) {
        if (permissionsState == this) {
            return;
        }
        if (this.mPermissions != null) {
            if (permissionsState.mPermissions == null) {
                this.mPermissions = null;
            } else {
                this.mPermissions.clear();
            }
        }
        if (permissionsState.mPermissions != null) {
            if (this.mPermissions == null) {
                this.mPermissions = new ArrayMap<>();
            }
            int size = permissionsState.mPermissions.size();
            for (int i = 0; i < size; i++) {
                this.mPermissions.put(permissionsState.mPermissions.keyAt(i), new PermissionData(permissionsState.mPermissions.valueAt(i)));
            }
        }
        this.mGlobalGids = NO_GIDS;
        if (permissionsState.mGlobalGids != NO_GIDS) {
            this.mGlobalGids = Arrays.copyOf(permissionsState.mGlobalGids, permissionsState.mGlobalGids.length);
        }
        if (this.mPermissionReviewRequired != null) {
            if (permissionsState.mPermissionReviewRequired == null) {
                this.mPermissionReviewRequired = null;
            } else {
                this.mPermissionReviewRequired.clear();
            }
        }
        if (permissionsState.mPermissionReviewRequired != null) {
            if (this.mPermissionReviewRequired == null) {
                this.mPermissionReviewRequired = new SparseBooleanArray();
            }
            int size2 = permissionsState.mPermissionReviewRequired.size();
            for (int i2 = 0; i2 < size2; i2++) {
                this.mPermissionReviewRequired.put(i2, permissionsState.mPermissionReviewRequired.valueAt(i2));
            }
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PermissionsState permissionsState = (PermissionsState) obj;
        if (this.mPermissions == null) {
            if (permissionsState.mPermissions != null) {
                return false;
            }
        } else if (!this.mPermissions.equals(permissionsState.mPermissions)) {
            return false;
        }
        if (this.mPermissionReviewRequired == null) {
            if (permissionsState.mPermissionReviewRequired != null) {
                return false;
            }
        } else if (!this.mPermissionReviewRequired.equals(permissionsState.mPermissionReviewRequired)) {
            return false;
        }
        return Arrays.equals(this.mGlobalGids, permissionsState.mGlobalGids);
    }

    public boolean isPermissionReviewRequired(int i) {
        return this.mPermissionReviewRequired != null && this.mPermissionReviewRequired.get(i);
    }

    public int grantInstallPermission(BasePermission basePermission) {
        return grantPermission(basePermission, -1);
    }

    public int revokeInstallPermission(BasePermission basePermission) {
        return revokePermission(basePermission, -1);
    }

    public int grantRuntimePermission(BasePermission basePermission, int i) {
        enforceValidUserId(i);
        if (i == -1) {
            return -1;
        }
        return grantPermission(basePermission, i);
    }

    public int revokeRuntimePermission(BasePermission basePermission, int i) {
        enforceValidUserId(i);
        if (i == -1) {
            return -1;
        }
        return revokePermission(basePermission, i);
    }

    public boolean hasRuntimePermission(String str, int i) {
        enforceValidUserId(i);
        return !hasInstallPermission(str) && hasPermission(str, i);
    }

    public boolean hasInstallPermission(String str) {
        return hasPermission(str, -1);
    }

    public boolean hasPermission(String str, int i) {
        PermissionData permissionData;
        enforceValidUserId(i);
        return (this.mPermissions == null || (permissionData = this.mPermissions.get(str)) == null || !permissionData.isGranted(i)) ? false : true;
    }

    public boolean hasRequestedPermission(ArraySet<String> arraySet) {
        if (this.mPermissions == null) {
            return false;
        }
        for (int size = arraySet.size() - 1; size >= 0; size--) {
            if (this.mPermissions.get(arraySet.valueAt(size)) != null) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getPermissions(int i) {
        enforceValidUserId(i);
        if (this.mPermissions == null) {
            return Collections.emptySet();
        }
        ArraySet arraySet = new ArraySet(this.mPermissions.size());
        int size = this.mPermissions.size();
        for (int i2 = 0; i2 < size; i2++) {
            String strKeyAt = this.mPermissions.keyAt(i2);
            if (hasInstallPermission(strKeyAt)) {
                arraySet.add(strKeyAt);
            } else if (i != -1 && hasRuntimePermission(strKeyAt, i)) {
                arraySet.add(strKeyAt);
            }
        }
        return arraySet;
    }

    public PermissionState getInstallPermissionState(String str) {
        return getPermissionState(str, -1);
    }

    public PermissionState getRuntimePermissionState(String str, int i) {
        enforceValidUserId(i);
        return getPermissionState(str, i);
    }

    public List<PermissionState> getInstallPermissionStates() {
        return getPermissionStatesInternal(-1);
    }

    public List<PermissionState> getRuntimePermissionStates(int i) {
        enforceValidUserId(i);
        return getPermissionStatesInternal(i);
    }

    public int getPermissionFlags(String str, int i) {
        PermissionState installPermissionState = getInstallPermissionState(str);
        if (installPermissionState != null) {
            return installPermissionState.getFlags();
        }
        PermissionState runtimePermissionState = getRuntimePermissionState(str, i);
        if (runtimePermissionState != null) {
            return runtimePermissionState.getFlags();
        }
        return 0;
    }

    public boolean updatePermissionFlags(BasePermission basePermission, int i, int i2, int i3) {
        enforceValidUserId(i);
        boolean z = (i3 == 0 && i2 == 0) ? false : true;
        if (this.mPermissions == null) {
            if (!z) {
                return false;
            }
            ensurePermissionData(basePermission);
        }
        PermissionData permissionDataEnsurePermissionData = this.mPermissions.get(basePermission.getName());
        if (permissionDataEnsurePermissionData == null) {
            if (!z) {
                return false;
            }
            permissionDataEnsurePermissionData = ensurePermissionData(basePermission);
        }
        int flags = permissionDataEnsurePermissionData.getFlags(i);
        boolean zUpdateFlags = permissionDataEnsurePermissionData.updateFlags(i, i2, i3);
        if (zUpdateFlags) {
            int flags2 = permissionDataEnsurePermissionData.getFlags(i);
            int i4 = flags & 64;
            if (i4 == 0 && (flags2 & 64) != 0) {
                if (this.mPermissionReviewRequired == null) {
                    this.mPermissionReviewRequired = new SparseBooleanArray();
                }
                this.mPermissionReviewRequired.put(i, true);
            } else if (i4 != 0 && (flags2 & 64) == 0 && this.mPermissionReviewRequired != null && !hasPermissionRequiringReview(i)) {
                this.mPermissionReviewRequired.delete(i);
                if (this.mPermissionReviewRequired.size() <= 0) {
                    this.mPermissionReviewRequired = null;
                }
            }
        }
        return zUpdateFlags;
    }

    private boolean hasPermissionRequiringReview(int i) {
        int size = this.mPermissions.size();
        for (int i2 = 0; i2 < size; i2++) {
            if ((this.mPermissions.valueAt(i2).getFlags(i) & 64) != 0) {
                return true;
            }
        }
        return false;
    }

    public boolean updatePermissionFlagsForAllPermissions(int i, int i2, int i3) {
        enforceValidUserId(i);
        if (this.mPermissions == null) {
            return false;
        }
        int size = this.mPermissions.size();
        boolean zUpdateFlags = false;
        for (int i4 = 0; i4 < size; i4++) {
            zUpdateFlags |= this.mPermissions.valueAt(i4).updateFlags(i, i2, i3);
        }
        return zUpdateFlags;
    }

    public int[] computeGids(int i) {
        int[] iArrComputeGids;
        enforceValidUserId(i);
        int[] iArrAppendInts = this.mGlobalGids;
        if (this.mPermissions != null) {
            int size = this.mPermissions.size();
            for (int i2 = 0; i2 < size; i2++) {
                if (hasPermission(this.mPermissions.keyAt(i2), i) && (iArrComputeGids = this.mPermissions.valueAt(i2).computeGids(i)) != NO_GIDS) {
                    iArrAppendInts = appendInts(iArrAppendInts, iArrComputeGids);
                }
            }
        }
        return iArrAppendInts;
    }

    public int[] computeGids(int[] iArr) {
        int[] iArrAppendInts = this.mGlobalGids;
        for (int i : iArr) {
            iArrAppendInts = appendInts(iArrAppendInts, computeGids(i));
        }
        return iArrAppendInts;
    }

    public void reset() {
        this.mGlobalGids = NO_GIDS;
        this.mPermissions = null;
        this.mPermissionReviewRequired = null;
    }

    private PermissionState getPermissionState(String str, int i) {
        PermissionData permissionData;
        if (this.mPermissions == null || (permissionData = this.mPermissions.get(str)) == null) {
            return null;
        }
        return permissionData.getPermissionState(i);
    }

    private List<PermissionState> getPermissionStatesInternal(int i) {
        enforceValidUserId(i);
        if (this.mPermissions == null) {
            return Collections.emptyList();
        }
        ArrayList arrayList = new ArrayList();
        int size = this.mPermissions.size();
        for (int i2 = 0; i2 < size; i2++) {
            PermissionState permissionState = this.mPermissions.valueAt(i2).getPermissionState(i);
            if (permissionState != null) {
                arrayList.add(permissionState);
            }
        }
        return arrayList;
    }

    private int grantPermission(BasePermission basePermission, int i) {
        if (hasPermission(basePermission.getName(), i)) {
            return -1;
        }
        boolean z = !ArrayUtils.isEmpty(basePermission.computeGids(i));
        int[] iArrComputeGids = z ? computeGids(i) : NO_GIDS;
        if (!ensurePermissionData(basePermission).grant(i)) {
            return -1;
        }
        if (z) {
            if (iArrComputeGids.length != computeGids(i).length) {
                return 1;
            }
            return 0;
        }
        return 0;
    }

    private int revokePermission(BasePermission basePermission, int i) {
        String name = basePermission.getName();
        if (!hasPermission(name, i)) {
            return -1;
        }
        boolean z = !ArrayUtils.isEmpty(basePermission.computeGids(i));
        int[] iArrComputeGids = z ? computeGids(i) : NO_GIDS;
        PermissionData permissionData = this.mPermissions.get(name);
        if (!permissionData.revoke(i)) {
            return -1;
        }
        if (permissionData.isDefault()) {
            ensureNoPermissionData(name);
        }
        if (z) {
            if (iArrComputeGids.length != computeGids(i).length) {
                return 1;
            }
            return 0;
        }
        return 0;
    }

    private static int[] appendInts(int[] iArr, int[] iArr2) {
        if (iArr != null && iArr2 != null) {
            for (int i : iArr2) {
                iArr = ArrayUtils.appendInt(iArr, i);
            }
        }
        return iArr;
    }

    private static void enforceValidUserId(int i) {
        if (i != -1 && i < 0) {
            throw new IllegalArgumentException("Invalid userId:" + i);
        }
    }

    private PermissionData ensurePermissionData(BasePermission basePermission) {
        String name = basePermission.getName();
        if (this.mPermissions == null) {
            this.mPermissions = new ArrayMap<>();
        }
        PermissionData permissionData = this.mPermissions.get(name);
        if (permissionData == null) {
            PermissionData permissionData2 = new PermissionData(basePermission);
            this.mPermissions.put(name, permissionData2);
            return permissionData2;
        }
        return permissionData;
    }

    private void ensureNoPermissionData(String str) {
        if (this.mPermissions == null) {
            return;
        }
        this.mPermissions.remove(str);
        if (this.mPermissions.isEmpty()) {
            this.mPermissions = null;
        }
    }

    private static final class PermissionData {
        private final BasePermission mPerm;
        private SparseArray<PermissionState> mUserStates;

        public PermissionData(BasePermission basePermission) {
            this.mUserStates = new SparseArray<>();
            this.mPerm = basePermission;
        }

        public PermissionData(PermissionData permissionData) {
            this(permissionData.mPerm);
            int size = permissionData.mUserStates.size();
            for (int i = 0; i < size; i++) {
                this.mUserStates.put(permissionData.mUserStates.keyAt(i), new PermissionState(permissionData.mUserStates.valueAt(i)));
            }
        }

        public int[] computeGids(int i) {
            return this.mPerm.computeGids(i);
        }

        public boolean isGranted(int i) {
            if (isInstallPermission()) {
                i = -1;
            }
            PermissionState permissionState = this.mUserStates.get(i);
            if (permissionState == null) {
                return false;
            }
            return permissionState.mGranted;
        }

        public boolean grant(int i) {
            if (!isCompatibleUserId(i) || isGranted(i)) {
                return false;
            }
            PermissionState permissionState = this.mUserStates.get(i);
            if (permissionState == null) {
                permissionState = new PermissionState(this.mPerm.getName());
                this.mUserStates.put(i, permissionState);
            }
            permissionState.mGranted = true;
            return true;
        }

        public boolean revoke(int i) {
            if (!isCompatibleUserId(i) || !isGranted(i)) {
                return false;
            }
            PermissionState permissionState = this.mUserStates.get(i);
            permissionState.mGranted = false;
            if (permissionState.isDefault()) {
                this.mUserStates.remove(i);
                return true;
            }
            return true;
        }

        public PermissionState getPermissionState(int i) {
            return this.mUserStates.get(i);
        }

        public int getFlags(int i) {
            PermissionState permissionState = this.mUserStates.get(i);
            if (permissionState == null) {
                return 0;
            }
            return permissionState.mFlags;
        }

        public boolean isDefault() {
            return this.mUserStates.size() <= 0;
        }

        public static boolean isInstallPermissionKey(int i) {
            return i == -1;
        }

        public boolean updateFlags(int i, int i2, int i3) {
            if (isInstallPermission()) {
                i = -1;
            }
            if (!isCompatibleUserId(i)) {
                return false;
            }
            int i4 = i3 & i2;
            PermissionState permissionState = this.mUserStates.get(i);
            if (permissionState == null) {
                if (i4 == 0) {
                    return false;
                }
                PermissionState permissionState2 = new PermissionState(this.mPerm.getName());
                permissionState2.mFlags = i4;
                this.mUserStates.put(i, permissionState2);
                return true;
            }
            int i5 = permissionState.mFlags;
            permissionState.mFlags = ((~i2) & permissionState.mFlags) | i4;
            if (permissionState.isDefault()) {
                this.mUserStates.remove(i);
            }
            return permissionState.mFlags != i5;
        }

        private boolean isCompatibleUserId(int i) {
            if (!isDefault()) {
                if (isInstallPermissionKey(i) ^ isInstallPermission()) {
                    return false;
                }
            }
            return true;
        }

        private boolean isInstallPermission() {
            return this.mUserStates.size() == 1 && this.mUserStates.get(-1) != null;
        }
    }

    public static final class PermissionState {
        private int mFlags;
        private boolean mGranted;
        private final String mName;

        public PermissionState(String str) {
            this.mName = str;
        }

        public PermissionState(PermissionState permissionState) {
            this.mName = permissionState.mName;
            this.mGranted = permissionState.mGranted;
            this.mFlags = permissionState.mFlags;
        }

        public boolean isDefault() {
            return !this.mGranted && this.mFlags == 0;
        }

        public String getName() {
            return this.mName;
        }

        public boolean isGranted() {
            return this.mGranted;
        }

        public int getFlags() {
            return this.mFlags;
        }
    }

    public void updateReviewRequiredCache(int i) {
        if (this.mPermissions == null) {
            return;
        }
        Iterator<PermissionData> it = this.mPermissions.values().iterator();
        while (it.hasNext()) {
            if ((it.next().getFlags(i) & 64) != 0) {
                if (this.mPermissionReviewRequired == null) {
                    this.mPermissionReviewRequired = new SparseBooleanArray();
                }
                this.mPermissionReviewRequired.put(i, true);
                return;
            }
        }
        if (this.mPermissionReviewRequired != null) {
            this.mPermissionReviewRequired.delete(i);
            if (this.mPermissionReviewRequired.size() <= 0) {
                this.mPermissionReviewRequired = null;
            }
        }
    }
}
