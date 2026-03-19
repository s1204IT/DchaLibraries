package com.android.server.pm;

import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.util.ArrayUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    public PermissionsState(PermissionsState prototype) {
        copyFrom(prototype);
    }

    public void setGlobalGids(int[] globalGids) {
        if (ArrayUtils.isEmpty(globalGids)) {
            return;
        }
        this.mGlobalGids = Arrays.copyOf(globalGids, globalGids.length);
    }

    public void copyFrom(PermissionsState other) {
        if (other == this) {
            return;
        }
        if (this.mPermissions != null) {
            if (other.mPermissions == null) {
                this.mPermissions = null;
            } else {
                this.mPermissions.clear();
            }
        }
        if (other.mPermissions != null) {
            if (this.mPermissions == null) {
                this.mPermissions = new ArrayMap<>();
            }
            int permissionCount = other.mPermissions.size();
            for (int i = 0; i < permissionCount; i++) {
                String name = other.mPermissions.keyAt(i);
                PermissionData permissionData = other.mPermissions.valueAt(i);
                this.mPermissions.put(name, new PermissionData(permissionData));
            }
        }
        this.mGlobalGids = NO_GIDS;
        if (other.mGlobalGids != NO_GIDS) {
            this.mGlobalGids = Arrays.copyOf(other.mGlobalGids, other.mGlobalGids.length);
        }
        if (this.mPermissionReviewRequired != null) {
            if (other.mPermissionReviewRequired == null) {
                this.mPermissionReviewRequired = null;
            } else {
                this.mPermissionReviewRequired.clear();
            }
        }
        if (other.mPermissionReviewRequired == null) {
            return;
        }
        if (this.mPermissionReviewRequired == null) {
            this.mPermissionReviewRequired = new SparseBooleanArray();
        }
        int userCount = other.mPermissionReviewRequired.size();
        for (int i2 = 0; i2 < userCount; i2++) {
            boolean reviewRequired = other.mPermissionReviewRequired.valueAt(i2);
            this.mPermissionReviewRequired.put(i2, reviewRequired);
        }
    }

    public boolean isPermissionReviewRequired(int userId) {
        if (this.mPermissionReviewRequired != null) {
            return this.mPermissionReviewRequired.get(userId);
        }
        return false;
    }

    public int grantInstallPermission(BasePermission permission) {
        return grantPermission(permission, -1);
    }

    public int revokeInstallPermission(BasePermission permission) {
        return revokePermission(permission, -1);
    }

    public int grantRuntimePermission(BasePermission permission, int userId) {
        enforceValidUserId(userId);
        if (userId == -1) {
            return -1;
        }
        return grantPermission(permission, userId);
    }

    public int revokeRuntimePermission(BasePermission permission, int userId) {
        enforceValidUserId(userId);
        if (userId == -1) {
            return -1;
        }
        return revokePermission(permission, userId);
    }

    public boolean hasRuntimePermission(String name, int userId) {
        enforceValidUserId(userId);
        if (hasInstallPermission(name)) {
            return false;
        }
        return hasPermission(name, userId);
    }

    public boolean hasInstallPermission(String name) {
        return hasPermission(name, -1);
    }

    public boolean hasPermission(String name, int userId) {
        PermissionData permissionData;
        enforceValidUserId(userId);
        if (this.mPermissions == null || (permissionData = this.mPermissions.get(name)) == null) {
            return false;
        }
        return permissionData.isGranted(userId);
    }

    public boolean hasRequestedPermission(ArraySet<String> names) {
        if (this.mPermissions == null) {
            return false;
        }
        for (int i = names.size() - 1; i >= 0; i--) {
            if (this.mPermissions.get(names.valueAt(i)) != null) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getPermissions(int userId) {
        enforceValidUserId(userId);
        if (this.mPermissions == null) {
            return Collections.emptySet();
        }
        Set<String> permissions = new ArraySet<>();
        int permissionCount = this.mPermissions.size();
        for (int i = 0; i < permissionCount; i++) {
            String permission = this.mPermissions.keyAt(i);
            if (hasInstallPermission(permission)) {
                permissions.add(permission);
            }
            if (userId != -1 && hasRuntimePermission(permission, userId)) {
                permissions.add(permission);
            }
        }
        return permissions;
    }

    public PermissionState getInstallPermissionState(String name) {
        return getPermissionState(name, -1);
    }

    public PermissionState getRuntimePermissionState(String name, int userId) {
        enforceValidUserId(userId);
        return getPermissionState(name, userId);
    }

    public List<PermissionState> getInstallPermissionStates() {
        return getPermissionStatesInternal(-1);
    }

    public List<PermissionState> getRuntimePermissionStates(int userId) {
        enforceValidUserId(userId);
        return getPermissionStatesInternal(userId);
    }

    public int getPermissionFlags(String name, int userId) {
        PermissionState installPermState = getInstallPermissionState(name);
        if (installPermState != null) {
            return installPermState.getFlags();
        }
        PermissionState runtimePermState = getRuntimePermissionState(name, userId);
        if (runtimePermState != null) {
            return runtimePermState.getFlags();
        }
        return 0;
    }

    public boolean updatePermissionFlags(BasePermission permission, int userId, int flagMask, int flagValues) {
        enforceValidUserId(userId);
        boolean mayChangeFlags = (flagValues == 0 && flagMask == 0) ? false : true;
        if (this.mPermissions == null) {
            if (!mayChangeFlags) {
                return false;
            }
            ensurePermissionData(permission);
        }
        PermissionData permissionData = this.mPermissions.get(permission.name);
        if (permissionData == null) {
            if (!mayChangeFlags) {
                return false;
            }
            permissionData = ensurePermissionData(permission);
        }
        int oldFlags = permissionData.getFlags(userId);
        boolean updated = permissionData.updateFlags(userId, flagMask, flagValues);
        if (updated) {
            int newFlags = permissionData.getFlags(userId);
            if ((oldFlags & 64) == 0 && (newFlags & 64) != 0) {
                if (this.mPermissionReviewRequired == null) {
                    this.mPermissionReviewRequired = new SparseBooleanArray();
                }
                this.mPermissionReviewRequired.put(userId, true);
            } else if ((oldFlags & 64) != 0 && (newFlags & 64) == 0 && this.mPermissionReviewRequired != null) {
                this.mPermissionReviewRequired.delete(userId);
                if (this.mPermissionReviewRequired.size() <= 0) {
                    this.mPermissionReviewRequired = null;
                }
            }
        }
        return updated;
    }

    public boolean updatePermissionFlagsForAllPermissions(int userId, int flagMask, int flagValues) {
        enforceValidUserId(userId);
        if (this.mPermissions == null) {
            return false;
        }
        boolean changed = false;
        int permissionCount = this.mPermissions.size();
        for (int i = 0; i < permissionCount; i++) {
            PermissionData permissionData = this.mPermissions.valueAt(i);
            changed |= permissionData.updateFlags(userId, flagMask, flagValues);
        }
        return changed;
    }

    public int[] computeGids(int userId) {
        enforceValidUserId(userId);
        int[] gids = this.mGlobalGids;
        if (this.mPermissions != null) {
            int permissionCount = this.mPermissions.size();
            for (int i = 0; i < permissionCount; i++) {
                String permission = this.mPermissions.keyAt(i);
                if (hasPermission(permission, userId)) {
                    PermissionData permissionData = this.mPermissions.valueAt(i);
                    int[] permGids = permissionData.computeGids(userId);
                    if (permGids != NO_GIDS) {
                        gids = appendInts(gids, permGids);
                    }
                }
            }
        }
        return gids;
    }

    public int[] computeGids(int[] userIds) {
        int[] gids = this.mGlobalGids;
        for (int userId : userIds) {
            int[] userGids = computeGids(userId);
            gids = appendInts(gids, userGids);
        }
        return gids;
    }

    public void reset() {
        this.mGlobalGids = NO_GIDS;
        this.mPermissions = null;
        this.mPermissionReviewRequired = null;
    }

    private PermissionState getPermissionState(String name, int userId) {
        PermissionData permissionData;
        if (this.mPermissions == null || (permissionData = this.mPermissions.get(name)) == null) {
            return null;
        }
        return permissionData.getPermissionState(userId);
    }

    private List<PermissionState> getPermissionStatesInternal(int userId) {
        enforceValidUserId(userId);
        if (this.mPermissions == null) {
            return Collections.emptyList();
        }
        List<PermissionState> permissionStates = new ArrayList<>();
        int permissionCount = this.mPermissions.size();
        for (int i = 0; i < permissionCount; i++) {
            PermissionData permissionData = this.mPermissions.valueAt(i);
            PermissionState permissionState = permissionData.getPermissionState(userId);
            if (permissionState != null) {
                permissionStates.add(permissionState);
            }
        }
        return permissionStates;
    }

    private int grantPermission(BasePermission permission, int userId) {
        if (hasPermission(permission.name, userId)) {
            return -1;
        }
        boolean hasGids = !ArrayUtils.isEmpty(permission.computeGids(userId));
        int[] oldGids = hasGids ? computeGids(userId) : NO_GIDS;
        PermissionData permissionData = ensurePermissionData(permission);
        if (!permissionData.grant(userId)) {
            return -1;
        }
        if (hasGids) {
            int[] newGids = computeGids(userId);
            if (oldGids.length != newGids.length) {
                return 1;
            }
        }
        return 0;
    }

    private int revokePermission(BasePermission permission, int userId) {
        if (!hasPermission(permission.name, userId)) {
            return -1;
        }
        boolean hasGids = !ArrayUtils.isEmpty(permission.computeGids(userId));
        int[] oldGids = hasGids ? computeGids(userId) : NO_GIDS;
        PermissionData permissionData = this.mPermissions.get(permission.name);
        if (!permissionData.revoke(userId)) {
            return -1;
        }
        if (permissionData.isDefault()) {
            ensureNoPermissionData(permission.name);
        }
        if (hasGids) {
            int[] newGids = computeGids(userId);
            if (oldGids.length != newGids.length) {
                return 1;
            }
        }
        return 0;
    }

    private static int[] appendInts(int[] current, int[] added) {
        if (current != null && added != null) {
            for (int guid : added) {
                current = ArrayUtils.appendInt(current, guid);
            }
        }
        return current;
    }

    private static void enforceValidUserId(int userId) {
        if (userId == -1 || userId >= 0) {
        } else {
            throw new IllegalArgumentException("Invalid userId:" + userId);
        }
    }

    private PermissionData ensurePermissionData(BasePermission permission) {
        if (this.mPermissions == null) {
            this.mPermissions = new ArrayMap<>();
        }
        PermissionData permissionData = this.mPermissions.get(permission.name);
        if (permissionData == null) {
            PermissionData permissionData2 = new PermissionData(permission);
            this.mPermissions.put(permission.name, permissionData2);
            return permissionData2;
        }
        return permissionData;
    }

    private void ensureNoPermissionData(String name) {
        if (this.mPermissions == null) {
            return;
        }
        this.mPermissions.remove(name);
        if (!this.mPermissions.isEmpty()) {
            return;
        }
        this.mPermissions = null;
    }

    public void updateReviewRequiredCache(int userId) {
        if (this.mPermissions == null) {
            return;
        }
        int permissionCount = this.mPermissions.size();
        for (int i = 0; i < permissionCount; i++) {
            PermissionData permissionData = this.mPermissions.valueAt(i);
            int flags = permissionData.getFlags(userId);
            if ((flags & 64) != 0) {
                if (this.mPermissionReviewRequired == null) {
                    this.mPermissionReviewRequired = new SparseBooleanArray();
                }
                this.mPermissionReviewRequired.put(userId, true);
                return;
            }
        }
        if (this.mPermissionReviewRequired == null) {
            return;
        }
        this.mPermissionReviewRequired.delete(userId);
        if (this.mPermissionReviewRequired.size() > 0) {
            return;
        }
        this.mPermissionReviewRequired = null;
    }

    private static final class PermissionData {
        private final BasePermission mPerm;
        private SparseArray<PermissionState> mUserStates;

        public PermissionData(BasePermission perm) {
            this.mUserStates = new SparseArray<>();
            this.mPerm = perm;
        }

        public PermissionData(PermissionData other) {
            this(other.mPerm);
            int otherStateCount = other.mUserStates.size();
            for (int i = 0; i < otherStateCount; i++) {
                int otherUserId = other.mUserStates.keyAt(i);
                PermissionState otherState = other.mUserStates.valueAt(i);
                this.mUserStates.put(otherUserId, new PermissionState(otherState));
            }
        }

        public int[] computeGids(int userId) {
            return this.mPerm.computeGids(userId);
        }

        public boolean isGranted(int userId) {
            if (isInstallPermission()) {
                userId = -1;
            }
            PermissionState userState = this.mUserStates.get(userId);
            if (userState == null) {
                return false;
            }
            return userState.mGranted;
        }

        public boolean grant(int userId) {
            if (!isCompatibleUserId(userId) || isGranted(userId)) {
                return false;
            }
            PermissionState userState = this.mUserStates.get(userId);
            if (userState == null) {
                userState = new PermissionState(this.mPerm.name);
                this.mUserStates.put(userId, userState);
            }
            userState.mGranted = true;
            return true;
        }

        public boolean revoke(int userId) {
            if (!isCompatibleUserId(userId) || !isGranted(userId)) {
                return false;
            }
            PermissionState userState = this.mUserStates.get(userId);
            userState.mGranted = false;
            if (userState.isDefault()) {
                this.mUserStates.remove(userId);
                return true;
            }
            return true;
        }

        public PermissionState getPermissionState(int userId) {
            return this.mUserStates.get(userId);
        }

        public int getFlags(int userId) {
            PermissionState userState = this.mUserStates.get(userId);
            if (userState != null) {
                return userState.mFlags;
            }
            return 0;
        }

        public boolean isDefault() {
            return this.mUserStates.size() <= 0;
        }

        public static boolean isInstallPermissionKey(int userId) {
            return userId == -1;
        }

        public boolean updateFlags(int userId, int flagMask, int flagValues) {
            if (isInstallPermission()) {
                userId = -1;
            }
            if (!isCompatibleUserId(userId)) {
                return false;
            }
            int newFlags = flagValues & flagMask;
            PermissionState userState = this.mUserStates.get(userId);
            if (userState != null) {
                int oldFlags = userState.mFlags;
                userState.mFlags = (userState.mFlags & (~flagMask)) | newFlags;
                if (userState.isDefault()) {
                    this.mUserStates.remove(userId);
                }
                return userState.mFlags != oldFlags;
            }
            if (newFlags == 0) {
                return false;
            }
            PermissionState userState2 = new PermissionState(this.mPerm.name);
            userState2.mFlags = newFlags;
            this.mUserStates.put(userId, userState2);
            return true;
        }

        private boolean isCompatibleUserId(int userId) {
            return isDefault() || !(isInstallPermission() ^ isInstallPermissionKey(userId));
        }

        private boolean isInstallPermission() {
            return this.mUserStates.size() == 1 && this.mUserStates.get(-1) != null;
        }
    }

    public static final class PermissionState {
        private int mFlags;
        private boolean mGranted;
        private final String mName;

        public PermissionState(String name) {
            this.mName = name;
        }

        public PermissionState(PermissionState other) {
            this.mName = other.mName;
            this.mGranted = other.mGranted;
            this.mFlags = other.mFlags;
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
}
