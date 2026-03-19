package com.android.server.locksettings;

import android.content.Context;
import android.content.pm.UserInfo;
import android.hardware.weaver.V1_0.IWeaver;
import android.hardware.weaver.V1_0.WeaverConfig;
import android.hardware.weaver.V1_0.WeaverReadResponse;
import android.os.RemoteException;
import android.os.UserManager;
import android.service.gatekeeper.GateKeeperResponse;
import android.service.gatekeeper.IGateKeeperService;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.widget.ICheckCredentialProgressCallback;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.server.locksettings.LockSettingsStorage;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import libcore.util.HexEncoding;

public class SyntheticPasswordManager {
    public static final long DEFAULT_HANDLE = 0;
    private static final String DEFAULT_PASSWORD = "default-password";
    private static final int INVALID_WEAVER_SLOT = -1;
    private static final String PASSWORD_DATA_NAME = "pwd";
    private static final int PASSWORD_SALT_LENGTH = 16;
    private static final int PASSWORD_SCRYPT_N = 11;
    private static final int PASSWORD_SCRYPT_P = 1;
    private static final int PASSWORD_SCRYPT_R = 3;
    private static final int PASSWORD_TOKEN_LENGTH = 32;
    private static final int SECDISCARDABLE_LENGTH = 16384;
    private static final String SECDISCARDABLE_NAME = "secdis";
    private static final String SP_BLOB_NAME = "spblob";
    private static final String SP_E0_NAME = "e0";
    private static final String SP_HANDLE_NAME = "handle";
    private static final String SP_P1_NAME = "p1";
    private static final byte SYNTHETIC_PASSWORD_LENGTH = 32;
    private static final byte SYNTHETIC_PASSWORD_PASSWORD_BASED = 0;
    private static final byte SYNTHETIC_PASSWORD_TOKEN_BASED = 1;
    private static final byte SYNTHETIC_PASSWORD_VERSION = 2;
    private static final byte SYNTHETIC_PASSWORD_VERSION_V1 = 1;
    private static final String TAG = "SyntheticPasswordManager";
    private static final String WEAVER_SLOT_NAME = "weaver";
    private static final byte WEAVER_VERSION = 1;
    private final Context mContext;
    private LockSettingsStorage mStorage;
    private final UserManager mUserManager;
    private IWeaver mWeaver;
    private WeaverConfig mWeaverConfig;
    private ArrayMap<Integer, ArrayMap<Long, TokenData>> tokenMap = new ArrayMap<>();
    private static final byte[] PERSONALISATION_SECDISCARDABLE = "secdiscardable-transform".getBytes();
    private static final byte[] PERSONALIZATION_KEY_STORE_PASSWORD = "keystore-password".getBytes();
    private static final byte[] PERSONALIZATION_USER_GK_AUTH = "user-gk-authentication".getBytes();
    private static final byte[] PERSONALIZATION_SP_GK_AUTH = "sp-gk-authentication".getBytes();
    private static final byte[] PERSONALIZATION_FBE_KEY = "fbe-key".getBytes();
    private static final byte[] PERSONALIZATION_AUTHSECRET_KEY = "authsecret-hal".getBytes();
    private static final byte[] PERSONALIZATION_SP_SPLIT = "sp-split".getBytes();
    private static final byte[] PERSONALIZATION_PASSWORD_HASH = "pw-hash".getBytes();
    private static final byte[] PERSONALIZATION_E0 = "e0-encryption".getBytes();
    private static final byte[] PERSONALISATION_WEAVER_PASSWORD = "weaver-pwd".getBytes();
    private static final byte[] PERSONALISATION_WEAVER_KEY = "weaver-key".getBytes();
    private static final byte[] PERSONALISATION_WEAVER_TOKEN = "weaver-token".getBytes();
    protected static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    native byte[] nativeScrypt(byte[] bArr, byte[] bArr2, int i, int i2, int i3, int i4);

    native long nativeSidFromPasswordHandle(byte[] bArr);

    static class AuthenticationResult {
        public AuthenticationToken authToken;
        public int credentialType;
        public VerifyCredentialResponse gkResponse;

        AuthenticationResult() {
        }
    }

    static class AuthenticationToken {
        private byte[] E0;
        private byte[] P1;
        private String syntheticPassword;

        AuthenticationToken() {
        }

        public String deriveKeyStorePassword() {
            return SyntheticPasswordManager.bytesToHex(SyntheticPasswordCrypto.personalisedHash(SyntheticPasswordManager.PERSONALIZATION_KEY_STORE_PASSWORD, this.syntheticPassword.getBytes()));
        }

        public byte[] deriveGkPassword() {
            return SyntheticPasswordCrypto.personalisedHash(SyntheticPasswordManager.PERSONALIZATION_SP_GK_AUTH, this.syntheticPassword.getBytes());
        }

        public byte[] deriveDiskEncryptionKey() {
            return SyntheticPasswordCrypto.personalisedHash(SyntheticPasswordManager.PERSONALIZATION_FBE_KEY, this.syntheticPassword.getBytes());
        }

        public byte[] deriveVendorAuthSecret() {
            return SyntheticPasswordCrypto.personalisedHash(SyntheticPasswordManager.PERSONALIZATION_AUTHSECRET_KEY, this.syntheticPassword.getBytes());
        }

        public byte[] derivePasswordHashFactor() {
            return SyntheticPasswordCrypto.personalisedHash(SyntheticPasswordManager.PERSONALIZATION_PASSWORD_HASH, this.syntheticPassword.getBytes());
        }

        private void initialize(byte[] bArr, byte[] bArr2) {
            this.P1 = bArr2;
            this.syntheticPassword = String.valueOf(HexEncoding.encode(SyntheticPasswordCrypto.personalisedHash(SyntheticPasswordManager.PERSONALIZATION_SP_SPLIT, bArr, bArr2)));
            this.E0 = SyntheticPasswordCrypto.encrypt(this.syntheticPassword.getBytes(), SyntheticPasswordManager.PERSONALIZATION_E0, bArr);
        }

        public void recreate(byte[] bArr) {
            initialize(bArr, this.P1);
        }

        protected static AuthenticationToken create() {
            AuthenticationToken authenticationToken = new AuthenticationToken();
            authenticationToken.initialize(SyntheticPasswordManager.secureRandom(32), SyntheticPasswordManager.secureRandom(32));
            return authenticationToken;
        }

        public byte[] computeP0() {
            if (this.E0 != null) {
                return SyntheticPasswordCrypto.decrypt(this.syntheticPassword.getBytes(), SyntheticPasswordManager.PERSONALIZATION_E0, this.E0);
            }
            return null;
        }
    }

    static class PasswordData {
        public byte[] passwordHandle;
        public int passwordType;
        byte[] salt;
        byte scryptN;
        byte scryptP;
        byte scryptR;

        PasswordData() {
        }

        public static PasswordData create(int i) {
            PasswordData passwordData = new PasswordData();
            passwordData.scryptN = (byte) 11;
            passwordData.scryptR = (byte) 3;
            passwordData.scryptP = (byte) 1;
            passwordData.passwordType = i;
            passwordData.salt = SyntheticPasswordManager.secureRandom(16);
            return passwordData;
        }

        public static PasswordData fromBytes(byte[] bArr) {
            PasswordData passwordData = new PasswordData();
            ByteBuffer byteBufferAllocate = ByteBuffer.allocate(bArr.length);
            byteBufferAllocate.put(bArr, 0, bArr.length);
            byteBufferAllocate.flip();
            passwordData.passwordType = byteBufferAllocate.getInt();
            passwordData.scryptN = byteBufferAllocate.get();
            passwordData.scryptR = byteBufferAllocate.get();
            passwordData.scryptP = byteBufferAllocate.get();
            passwordData.salt = new byte[byteBufferAllocate.getInt()];
            byteBufferAllocate.get(passwordData.salt);
            int i = byteBufferAllocate.getInt();
            if (i > 0) {
                passwordData.passwordHandle = new byte[i];
                byteBufferAllocate.get(passwordData.passwordHandle);
            } else {
                passwordData.passwordHandle = null;
            }
            return passwordData;
        }

        public byte[] toBytes() {
            ByteBuffer byteBufferAllocate = ByteBuffer.allocate(11 + this.salt.length + 4 + (this.passwordHandle != null ? this.passwordHandle.length : 0));
            byteBufferAllocate.putInt(this.passwordType);
            byteBufferAllocate.put(this.scryptN);
            byteBufferAllocate.put(this.scryptR);
            byteBufferAllocate.put(this.scryptP);
            byteBufferAllocate.putInt(this.salt.length);
            byteBufferAllocate.put(this.salt);
            if (this.passwordHandle != null && this.passwordHandle.length > 0) {
                byteBufferAllocate.putInt(this.passwordHandle.length);
                byteBufferAllocate.put(this.passwordHandle);
            } else {
                byteBufferAllocate.putInt(0);
            }
            return byteBufferAllocate.array();
        }
    }

    static class TokenData {
        byte[] aggregatedSecret;
        byte[] secdiscardableOnDisk;
        byte[] weaverSecret;

        TokenData() {
        }
    }

    public SyntheticPasswordManager(Context context, LockSettingsStorage lockSettingsStorage, UserManager userManager) {
        this.mContext = context;
        this.mStorage = lockSettingsStorage;
        this.mUserManager = userManager;
    }

    @VisibleForTesting
    protected IWeaver getWeaverService() throws RemoteException {
        try {
            return IWeaver.getService();
        } catch (NoSuchElementException e) {
            Slog.i(TAG, "Device does not support weaver");
            return null;
        }
    }

    public synchronized void initWeaverService() {
        if (this.mWeaver != null) {
            return;
        }
        try {
            this.mWeaverConfig = null;
            this.mWeaver = getWeaverService();
            if (this.mWeaver != null) {
                this.mWeaver.getConfig(new IWeaver.getConfigCallback() {
                    @Override
                    public final void onValues(int i, WeaverConfig weaverConfig) {
                        SyntheticPasswordManager.lambda$initWeaverService$0(this.f$0, i, weaverConfig);
                    }
                });
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to get weaver service", e);
        }
    }

    public static void lambda$initWeaverService$0(SyntheticPasswordManager syntheticPasswordManager, int i, WeaverConfig weaverConfig) {
        if (i == 0 && weaverConfig.slots > 0) {
            syntheticPasswordManager.mWeaverConfig = weaverConfig;
            return;
        }
        Slog.e(TAG, "Failed to get weaver config, status " + i + " slots: " + weaverConfig.slots);
        syntheticPasswordManager.mWeaver = null;
    }

    private synchronized boolean isWeaverAvailable() {
        boolean z;
        if (this.mWeaver == null) {
            initWeaverService();
        }
        if (this.mWeaver != null) {
            z = this.mWeaverConfig.slots > 0;
        }
        return z;
    }

    private byte[] weaverEnroll(int i, byte[] bArr, byte[] bArr2) throws RemoteException {
        if (i == -1 || i >= this.mWeaverConfig.slots) {
            throw new RuntimeException("Invalid slot for weaver");
        }
        if (bArr == null) {
            bArr = new byte[this.mWeaverConfig.keySize];
        } else if (bArr.length != this.mWeaverConfig.keySize) {
            throw new RuntimeException("Invalid key size for weaver");
        }
        if (bArr2 == null) {
            bArr2 = secureRandom(this.mWeaverConfig.valueSize);
        }
        int iWrite = this.mWeaver.write(i, toByteArrayList(bArr), toByteArrayList(bArr2));
        if (iWrite != 0) {
            Log.e(TAG, "weaver write failed, slot: " + i + " status: " + iWrite);
            return null;
        }
        return bArr2;
    }

    private VerifyCredentialResponse weaverVerify(final int i, byte[] bArr) throws RemoteException {
        if (i == -1 || i >= this.mWeaverConfig.slots) {
            throw new RuntimeException("Invalid slot for weaver");
        }
        if (bArr == null) {
            bArr = new byte[this.mWeaverConfig.keySize];
        } else if (bArr.length != this.mWeaverConfig.keySize) {
            throw new RuntimeException("Invalid key size for weaver");
        }
        final VerifyCredentialResponse[] verifyCredentialResponseArr = new VerifyCredentialResponse[1];
        this.mWeaver.read(i, toByteArrayList(bArr), new IWeaver.readCallback() {
            @Override
            public final void onValues(int i2, WeaverReadResponse weaverReadResponse) {
                SyntheticPasswordManager.lambda$weaverVerify$1(verifyCredentialResponseArr, i, i2, weaverReadResponse);
            }
        });
        return verifyCredentialResponseArr[0];
    }

    static void lambda$weaverVerify$1(VerifyCredentialResponse[] verifyCredentialResponseArr, int i, int i2, WeaverReadResponse weaverReadResponse) {
        switch (i2) {
            case 0:
                verifyCredentialResponseArr[0] = new VerifyCredentialResponse(fromByteArrayList(weaverReadResponse.value));
                break;
            case 1:
                verifyCredentialResponseArr[0] = VerifyCredentialResponse.ERROR;
                Log.e(TAG, "weaver read failed (FAILED), slot: " + i);
                break;
            case 2:
                if (weaverReadResponse.timeout == 0) {
                    verifyCredentialResponseArr[0] = VerifyCredentialResponse.ERROR;
                    Log.e(TAG, "weaver read failed (INCORRECT_KEY), slot: " + i);
                } else {
                    verifyCredentialResponseArr[0] = new VerifyCredentialResponse(weaverReadResponse.timeout);
                    Log.e(TAG, "weaver read failed (INCORRECT_KEY/THROTTLE), slot: " + i);
                }
                break;
            case 3:
                verifyCredentialResponseArr[0] = new VerifyCredentialResponse(weaverReadResponse.timeout);
                Log.e(TAG, "weaver read failed (THROTTLE), slot: " + i);
                break;
            default:
                verifyCredentialResponseArr[0] = VerifyCredentialResponse.ERROR;
                Log.e(TAG, "weaver read unknown status " + i2 + ", slot: " + i);
                break;
        }
    }

    public void removeUser(int i) {
        Iterator<Long> it = this.mStorage.listSyntheticPasswordHandlesForUser(SP_BLOB_NAME, i).iterator();
        while (it.hasNext()) {
            long jLongValue = it.next().longValue();
            destroyWeaverSlot(jLongValue, i);
            destroySPBlobKey(getHandleName(jLongValue));
        }
    }

    public int getCredentialType(long j, int i) {
        byte[] bArrLoadState = loadState(PASSWORD_DATA_NAME, j, i);
        if (bArrLoadState == null) {
            Log.w(TAG, "getCredentialType: encountered empty password data for user " + i);
            return -1;
        }
        return PasswordData.fromBytes(bArrLoadState).passwordType;
    }

    public AuthenticationToken newSyntheticPasswordAndSid(IGateKeeperService iGateKeeperService, byte[] bArr, String str, int i) throws RemoteException {
        AuthenticationToken authenticationTokenCreate = AuthenticationToken.create();
        if (bArr != null) {
            GateKeeperResponse gateKeeperResponseEnroll = iGateKeeperService.enroll(i, bArr, str.getBytes(), authenticationTokenCreate.deriveGkPassword());
            if (gateKeeperResponseEnroll.getResponseCode() != 0) {
                Log.w(TAG, "Fail to migrate SID, assuming no SID, user " + i);
                clearSidForUser(i);
            } else {
                saveSyntheticPasswordHandle(gateKeeperResponseEnroll.getPayload(), i);
            }
        } else {
            clearSidForUser(i);
        }
        saveEscrowData(authenticationTokenCreate, i);
        return authenticationTokenCreate;
    }

    public void newSidForUser(IGateKeeperService iGateKeeperService, AuthenticationToken authenticationToken, int i) throws RemoteException {
        GateKeeperResponse gateKeeperResponseEnroll = iGateKeeperService.enroll(i, (byte[]) null, (byte[]) null, authenticationToken.deriveGkPassword());
        if (gateKeeperResponseEnroll.getResponseCode() != 0) {
            Log.e(TAG, "Fail to create new SID for user " + i);
            return;
        }
        saveSyntheticPasswordHandle(gateKeeperResponseEnroll.getPayload(), i);
    }

    public void clearSidForUser(int i) {
        destroyState(SP_HANDLE_NAME, 0L, i);
    }

    public boolean hasSidForUser(int i) {
        return hasState(SP_HANDLE_NAME, 0L, i);
    }

    private byte[] loadSyntheticPasswordHandle(int i) {
        return loadState(SP_HANDLE_NAME, 0L, i);
    }

    private void saveSyntheticPasswordHandle(byte[] bArr, int i) {
        saveState(SP_HANDLE_NAME, bArr, 0L, i);
    }

    private boolean loadEscrowData(AuthenticationToken authenticationToken, int i) {
        authenticationToken.E0 = loadState(SP_E0_NAME, 0L, i);
        authenticationToken.P1 = loadState(SP_P1_NAME, 0L, i);
        return (authenticationToken.E0 == null || authenticationToken.P1 == null) ? false : true;
    }

    private void saveEscrowData(AuthenticationToken authenticationToken, int i) {
        saveState(SP_E0_NAME, authenticationToken.E0, 0L, i);
        saveState(SP_P1_NAME, authenticationToken.P1, 0L, i);
    }

    public boolean hasEscrowData(int i) {
        return hasState(SP_E0_NAME, 0L, i) && hasState(SP_P1_NAME, 0L, i);
    }

    public void destroyEscrowData(int i) {
        destroyState(SP_E0_NAME, 0L, i);
        destroyState(SP_P1_NAME, 0L, i);
    }

    private int loadWeaverSlot(long j, int i) {
        byte[] bArrLoadState = loadState(WEAVER_SLOT_NAME, j, i);
        if (bArrLoadState == null || bArrLoadState.length != 5) {
            return -1;
        }
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(5);
        byteBufferAllocate.put(bArrLoadState, 0, bArrLoadState.length);
        byteBufferAllocate.flip();
        if (byteBufferAllocate.get() != 1) {
            Log.e(TAG, "Invalid weaver slot version of handle " + j);
            return -1;
        }
        return byteBufferAllocate.getInt();
    }

    private void saveWeaverSlot(int i, long j, int i2) {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(5);
        byteBufferAllocate.put((byte) 1);
        byteBufferAllocate.putInt(i);
        saveState(WEAVER_SLOT_NAME, byteBufferAllocate.array(), j, i2);
    }

    private void destroyWeaverSlot(long j, int i) {
        int iLoadWeaverSlot = loadWeaverSlot(j, i);
        destroyState(WEAVER_SLOT_NAME, j, i);
        if (iLoadWeaverSlot != -1) {
            if (!getUsedWeaverSlots().contains(Integer.valueOf(iLoadWeaverSlot))) {
                Log.i(TAG, "Destroy weaver slot " + iLoadWeaverSlot + " for user " + i);
                try {
                    weaverEnroll(iLoadWeaverSlot, null, null);
                    return;
                } catch (RemoteException e) {
                    Log.w(TAG, "Failed to destroy slot", e);
                    return;
                }
            }
            Log.w(TAG, "Skip destroying reused weaver slot " + iLoadWeaverSlot + " for user " + i);
        }
    }

    private Set<Integer> getUsedWeaverSlots() {
        Map<Integer, List<Long>> mapListSyntheticPasswordHandlesForAllUsers = this.mStorage.listSyntheticPasswordHandlesForAllUsers(WEAVER_SLOT_NAME);
        HashSet hashSet = new HashSet();
        for (Map.Entry<Integer, List<Long>> entry : mapListSyntheticPasswordHandlesForAllUsers.entrySet()) {
            Iterator<Long> it = entry.getValue().iterator();
            while (it.hasNext()) {
                hashSet.add(Integer.valueOf(loadWeaverSlot(it.next().longValue(), entry.getKey().intValue())));
            }
        }
        return hashSet;
    }

    private int getNextAvailableWeaverSlot() {
        Set<Integer> usedWeaverSlots = getUsedWeaverSlots();
        for (int i = 0; i < this.mWeaverConfig.slots; i++) {
            if (!usedWeaverSlots.contains(Integer.valueOf(i))) {
                return i;
            }
        }
        throw new RuntimeException("Run out of weaver slots.");
    }

    public long createPasswordBasedSyntheticPassword(IGateKeeperService iGateKeeperService, String str, int i, AuthenticationToken authenticationToken, int i2, int i3) throws RemoteException {
        String str2;
        byte[] bArrTransformUnderWeaverSecret;
        int i4 = -1;
        if (str == null || i == -1) {
            str2 = DEFAULT_PASSWORD;
        } else {
            str2 = str;
            i4 = i;
        }
        long jGenerateHandle = generateHandle();
        PasswordData passwordDataCreate = PasswordData.create(i4);
        byte[] bArrComputePasswordToken = computePasswordToken(str2, passwordDataCreate);
        long j = 0;
        if (isWeaverAvailable()) {
            int nextAvailableWeaverSlot = getNextAvailableWeaverSlot();
            Log.i(TAG, "Weaver enroll password to slot " + nextAvailableWeaverSlot + " for user " + i3);
            byte[] bArrWeaverEnroll = weaverEnroll(nextAvailableWeaverSlot, passwordTokenToWeaverKey(bArrComputePasswordToken), null);
            if (bArrWeaverEnroll == null) {
                Log.e(TAG, "Fail to enroll user password under weaver " + i3);
                return 0L;
            }
            saveWeaverSlot(nextAvailableWeaverSlot, jGenerateHandle, i3);
            synchronizeWeaverFrpPassword(passwordDataCreate, i2, i3, nextAvailableWeaverSlot);
            passwordDataCreate.passwordHandle = null;
            bArrTransformUnderWeaverSecret = transformUnderWeaverSecret(bArrComputePasswordToken, bArrWeaverEnroll);
        } else {
            iGateKeeperService.clearSecureUserId(fakeUid(i3));
            GateKeeperResponse gateKeeperResponseEnroll = iGateKeeperService.enroll(fakeUid(i3), (byte[]) null, (byte[]) null, passwordTokenToGkInput(bArrComputePasswordToken));
            if (gateKeeperResponseEnroll.getResponseCode() != 0) {
                Log.e(TAG, "Fail to enroll user password when creating SP for user " + i3);
                return 0L;
            }
            passwordDataCreate.passwordHandle = gateKeeperResponseEnroll.getPayload();
            long jSidFromPasswordHandle = sidFromPasswordHandle(passwordDataCreate.passwordHandle);
            byte[] bArrTransformUnderSecdiscardable = transformUnderSecdiscardable(bArrComputePasswordToken, createSecdiscardable(jGenerateHandle, i3));
            synchronizeFrpPassword(passwordDataCreate, i2, i3);
            bArrTransformUnderWeaverSecret = bArrTransformUnderSecdiscardable;
            j = jSidFromPasswordHandle;
        }
        saveState(PASSWORD_DATA_NAME, passwordDataCreate.toBytes(), jGenerateHandle, i3);
        createSyntheticPasswordBlob(jGenerateHandle, (byte) 0, authenticationToken, bArrTransformUnderWeaverSecret, j, i3);
        return jGenerateHandle;
    }

    public VerifyCredentialResponse verifyFrpCredential(IGateKeeperService iGateKeeperService, String str, int i, ICheckCredentialProgressCallback iCheckCredentialProgressCallback) throws RemoteException {
        LockSettingsStorage.PersistentData persistentDataBlock = this.mStorage.readPersistentDataBlock();
        if (persistentDataBlock.type == 1) {
            PasswordData passwordDataFromBytes = PasswordData.fromBytes(persistentDataBlock.payload);
            return VerifyCredentialResponse.fromGateKeeperResponse(iGateKeeperService.verifyChallenge(fakeUid(persistentDataBlock.userId), 0L, passwordDataFromBytes.passwordHandle, passwordTokenToGkInput(computePasswordToken(str, passwordDataFromBytes))));
        }
        if (persistentDataBlock.type == 2) {
            return weaverVerify(persistentDataBlock.userId, passwordTokenToWeaverKey(computePasswordToken(str, PasswordData.fromBytes(persistentDataBlock.payload)))).stripPayload();
        }
        Log.e(TAG, "persistentData.type must be TYPE_SP or TYPE_SP_WEAVER, but is " + persistentDataBlock.type);
        return VerifyCredentialResponse.ERROR;
    }

    public void migrateFrpPasswordLocked(long j, UserInfo userInfo, int i) {
        if (this.mStorage.getPersistentDataBlock() != null && LockPatternUtils.userOwnsFrpCredential(this.mContext, userInfo)) {
            PasswordData passwordDataFromBytes = PasswordData.fromBytes(loadState(PASSWORD_DATA_NAME, j, userInfo.id));
            if (passwordDataFromBytes.passwordType != -1) {
                int iLoadWeaverSlot = loadWeaverSlot(j, userInfo.id);
                if (iLoadWeaverSlot != -1) {
                    synchronizeWeaverFrpPassword(passwordDataFromBytes, i, userInfo.id, iLoadWeaverSlot);
                } else {
                    synchronizeFrpPassword(passwordDataFromBytes, i, userInfo.id);
                }
            }
        }
    }

    private void synchronizeFrpPassword(PasswordData passwordData, int i, int i2) {
        if (this.mStorage.getPersistentDataBlock() != null && LockPatternUtils.userOwnsFrpCredential(this.mContext, this.mUserManager.getUserInfo(i2))) {
            if (passwordData.passwordType != -1) {
                this.mStorage.writePersistentDataBlock(1, i2, i, passwordData.toBytes());
            } else {
                this.mStorage.writePersistentDataBlock(0, i2, 0, null);
            }
        }
    }

    private void synchronizeWeaverFrpPassword(PasswordData passwordData, int i, int i2, int i3) {
        if (this.mStorage.getPersistentDataBlock() != null && LockPatternUtils.userOwnsFrpCredential(this.mContext, this.mUserManager.getUserInfo(i2))) {
            if (passwordData.passwordType != -1) {
                this.mStorage.writePersistentDataBlock(2, i3, i, passwordData.toBytes());
            } else {
                this.mStorage.writePersistentDataBlock(0, 0, 0, null);
            }
        }
    }

    public long createTokenBasedSyntheticPassword(byte[] bArr, int i) {
        long jGenerateHandle = generateHandle();
        if (!this.tokenMap.containsKey(Integer.valueOf(i))) {
            this.tokenMap.put(Integer.valueOf(i), new ArrayMap<>());
        }
        TokenData tokenData = new TokenData();
        byte[] bArrSecureRandom = secureRandom(16384);
        if (isWeaverAvailable()) {
            tokenData.weaverSecret = secureRandom(this.mWeaverConfig.valueSize);
            tokenData.secdiscardableOnDisk = SyntheticPasswordCrypto.encrypt(tokenData.weaverSecret, PERSONALISATION_WEAVER_TOKEN, bArrSecureRandom);
        } else {
            tokenData.secdiscardableOnDisk = bArrSecureRandom;
            tokenData.weaverSecret = null;
        }
        tokenData.aggregatedSecret = transformUnderSecdiscardable(bArr, bArrSecureRandom);
        this.tokenMap.get(Integer.valueOf(i)).put(Long.valueOf(jGenerateHandle), tokenData);
        return jGenerateHandle;
    }

    public Set<Long> getPendingTokensForUser(int i) {
        if (!this.tokenMap.containsKey(Integer.valueOf(i))) {
            return Collections.emptySet();
        }
        return this.tokenMap.get(Integer.valueOf(i)).keySet();
    }

    public boolean removePendingToken(long j, int i) {
        return this.tokenMap.containsKey(Integer.valueOf(i)) && this.tokenMap.get(Integer.valueOf(i)).remove(Long.valueOf(j)) != null;
    }

    public boolean activateTokenBasedSyntheticPassword(long j, AuthenticationToken authenticationToken, int i) {
        TokenData tokenData;
        if (!this.tokenMap.containsKey(Integer.valueOf(i)) || (tokenData = this.tokenMap.get(Integer.valueOf(i)).get(Long.valueOf(j))) == null) {
            return false;
        }
        if (!loadEscrowData(authenticationToken, i)) {
            Log.w(TAG, "User is not escrowable");
            return false;
        }
        if (isWeaverAvailable()) {
            int nextAvailableWeaverSlot = getNextAvailableWeaverSlot();
            try {
                Log.i(TAG, "Weaver enroll token to slot " + nextAvailableWeaverSlot + " for user " + i);
                weaverEnroll(nextAvailableWeaverSlot, null, tokenData.weaverSecret);
                saveWeaverSlot(nextAvailableWeaverSlot, j, i);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to enroll weaver secret when activating token", e);
                return false;
            }
        }
        saveSecdiscardable(j, tokenData.secdiscardableOnDisk, i);
        createSyntheticPasswordBlob(j, (byte) 1, authenticationToken, tokenData.aggregatedSecret, 0L, i);
        this.tokenMap.get(Integer.valueOf(i)).remove(Long.valueOf(j));
        return true;
    }

    private void createSyntheticPasswordBlob(long j, byte b, AuthenticationToken authenticationToken, byte[] bArr, long j2, int i) {
        byte[] bytes;
        if (b != 1) {
            bytes = authenticationToken.syntheticPassword.getBytes();
        } else {
            bytes = authenticationToken.computeP0();
        }
        byte[] bArrCreateSPBlob = createSPBlob(getHandleName(j), bytes, bArr, j2);
        byte[] bArr2 = new byte[bArrCreateSPBlob.length + 1 + 1];
        bArr2[0] = 2;
        bArr2[1] = b;
        System.arraycopy(bArrCreateSPBlob, 0, bArr2, 2, bArrCreateSPBlob.length);
        saveState(SP_BLOB_NAME, bArr2, j, i);
    }

    public AuthenticationResult unwrapPasswordBasedSyntheticPassword(IGateKeeperService iGateKeeperService, long j, String str, int i, ICheckCredentialProgressCallback iCheckCredentialProgressCallback) throws RemoteException {
        String str2;
        IGateKeeperService iGateKeeperService2;
        long jSidFromPasswordHandle;
        byte[] bArrTransformUnderSecdiscardable;
        int i2;
        if (str == null) {
            str2 = DEFAULT_PASSWORD;
        } else {
            str2 = str;
        }
        AuthenticationResult authenticationResult = new AuthenticationResult();
        PasswordData passwordDataFromBytes = PasswordData.fromBytes(loadState(PASSWORD_DATA_NAME, j, i));
        authenticationResult.credentialType = passwordDataFromBytes.passwordType;
        byte[] bArrComputePasswordToken = computePasswordToken(str2, passwordDataFromBytes);
        int iLoadWeaverSlot = loadWeaverSlot(j, i);
        if (iLoadWeaverSlot != -1) {
            if (!isWeaverAvailable()) {
                Log.e(TAG, "No weaver service to unwrap password based SP");
                authenticationResult.gkResponse = VerifyCredentialResponse.ERROR;
                return authenticationResult;
            }
            authenticationResult.gkResponse = weaverVerify(iLoadWeaverSlot, passwordTokenToWeaverKey(bArrComputePasswordToken));
            if (authenticationResult.gkResponse.getResponseCode() != 0) {
                return authenticationResult;
            }
            jSidFromPasswordHandle = 0;
            bArrTransformUnderSecdiscardable = transformUnderWeaverSecret(bArrComputePasswordToken, authenticationResult.gkResponse.getPayload());
            iGateKeeperService2 = iGateKeeperService;
        } else {
            byte[] bArrPasswordTokenToGkInput = passwordTokenToGkInput(bArrComputePasswordToken);
            GateKeeperResponse gateKeeperResponseVerifyChallenge = iGateKeeperService.verifyChallenge(fakeUid(i), 0L, passwordDataFromBytes.passwordHandle, bArrPasswordTokenToGkInput);
            int responseCode = gateKeeperResponseVerifyChallenge.getResponseCode();
            if (responseCode == 0) {
                authenticationResult.gkResponse = VerifyCredentialResponse.OK;
                if (gateKeeperResponseVerifyChallenge.getShouldReEnroll()) {
                    iGateKeeperService2 = iGateKeeperService;
                    GateKeeperResponse gateKeeperResponseEnroll = iGateKeeperService2.enroll(fakeUid(i), passwordDataFromBytes.passwordHandle, bArrPasswordTokenToGkInput, bArrPasswordTokenToGkInput);
                    if (gateKeeperResponseEnroll.getResponseCode() == 0) {
                        passwordDataFromBytes.passwordHandle = gateKeeperResponseEnroll.getPayload();
                        saveState(PASSWORD_DATA_NAME, passwordDataFromBytes.toBytes(), j, i);
                        if (passwordDataFromBytes.passwordType == 1) {
                            i2 = 65536;
                        } else {
                            i2 = 327680;
                        }
                        synchronizeFrpPassword(passwordDataFromBytes, i2, i);
                    } else {
                        Log.w(TAG, "Fail to re-enroll user password for user " + i);
                    }
                } else {
                    iGateKeeperService2 = iGateKeeperService;
                }
                jSidFromPasswordHandle = sidFromPasswordHandle(passwordDataFromBytes.passwordHandle);
                bArrTransformUnderSecdiscardable = transformUnderSecdiscardable(bArrComputePasswordToken, loadSecdiscardable(j, i));
            } else {
                if (responseCode == 1) {
                    authenticationResult.gkResponse = new VerifyCredentialResponse(gateKeeperResponseVerifyChallenge.getTimeout());
                    return authenticationResult;
                }
                authenticationResult.gkResponse = VerifyCredentialResponse.ERROR;
                return authenticationResult;
            }
        }
        long j2 = jSidFromPasswordHandle;
        byte[] bArr = bArrTransformUnderSecdiscardable;
        if (iCheckCredentialProgressCallback != null) {
            iCheckCredentialProgressCallback.onCredentialVerified();
        }
        authenticationResult.authToken = unwrapSyntheticPasswordBlob(j, (byte) 0, bArr, j2, i);
        authenticationResult.gkResponse = verifyChallenge(iGateKeeperService2, authenticationResult.authToken, 0L, i);
        return authenticationResult;
    }

    public AuthenticationResult unwrapTokenBasedSyntheticPassword(IGateKeeperService iGateKeeperService, long j, byte[] bArr, int i) throws RemoteException {
        AuthenticationResult authenticationResult = new AuthenticationResult();
        byte[] bArrLoadSecdiscardable = loadSecdiscardable(j, i);
        int iLoadWeaverSlot = loadWeaverSlot(j, i);
        if (iLoadWeaverSlot != -1) {
            if (!isWeaverAvailable()) {
                Log.e(TAG, "No weaver service to unwrap token based SP");
                authenticationResult.gkResponse = VerifyCredentialResponse.ERROR;
                return authenticationResult;
            }
            VerifyCredentialResponse verifyCredentialResponseWeaverVerify = weaverVerify(iLoadWeaverSlot, null);
            if (verifyCredentialResponseWeaverVerify.getResponseCode() != 0 || verifyCredentialResponseWeaverVerify.getPayload() == null) {
                Log.e(TAG, "Failed to retrieve weaver secret when unwrapping token");
                authenticationResult.gkResponse = VerifyCredentialResponse.ERROR;
                return authenticationResult;
            }
            bArrLoadSecdiscardable = SyntheticPasswordCrypto.decrypt(verifyCredentialResponseWeaverVerify.getPayload(), PERSONALISATION_WEAVER_TOKEN, bArrLoadSecdiscardable);
        }
        authenticationResult.authToken = unwrapSyntheticPasswordBlob(j, (byte) 1, transformUnderSecdiscardable(bArr, bArrLoadSecdiscardable), 0L, i);
        if (authenticationResult.authToken != null) {
            authenticationResult.gkResponse = verifyChallenge(iGateKeeperService, authenticationResult.authToken, 0L, i);
            if (authenticationResult.gkResponse == null) {
                authenticationResult.gkResponse = VerifyCredentialResponse.OK;
            }
        } else {
            authenticationResult.gkResponse = VerifyCredentialResponse.ERROR;
        }
        return authenticationResult;
    }

    private AuthenticationToken unwrapSyntheticPasswordBlob(long j, byte b, byte[] bArr, long j2, int i) {
        byte[] bArrDecryptSPBlob;
        byte[] bArrLoadState = loadState(SP_BLOB_NAME, j, i);
        if (bArrLoadState == null) {
            return null;
        }
        byte b2 = bArrLoadState[0];
        if (b2 != 2 && b2 != 1) {
            throw new RuntimeException("Unknown blob version");
        }
        if (bArrLoadState[1] != b) {
            throw new RuntimeException("Invalid blob type");
        }
        if (b2 == 1) {
            bArrDecryptSPBlob = SyntheticPasswordCrypto.decryptBlobV1(getHandleName(j), Arrays.copyOfRange(bArrLoadState, 2, bArrLoadState.length), bArr);
        } else {
            bArrDecryptSPBlob = decryptSPBlob(getHandleName(j), Arrays.copyOfRange(bArrLoadState, 2, bArrLoadState.length), bArr);
        }
        if (bArrDecryptSPBlob == null) {
            Log.e(TAG, "Fail to decrypt SP for user " + i);
            return null;
        }
        AuthenticationToken authenticationToken = new AuthenticationToken();
        if (b != 1) {
            authenticationToken.syntheticPassword = new String(bArrDecryptSPBlob);
        } else {
            if (!loadEscrowData(authenticationToken, i)) {
                Log.e(TAG, "User is not escrowable: " + i);
                return null;
            }
            authenticationToken.recreate(bArrDecryptSPBlob);
        }
        if (b2 == 1) {
            Log.i(TAG, "Upgrade v1 SP blob for user " + i + ", type = " + ((int) b));
            createSyntheticPasswordBlob(j, b, authenticationToken, bArr, j2, i);
        }
        return authenticationToken;
    }

    public VerifyCredentialResponse verifyChallenge(IGateKeeperService iGateKeeperService, AuthenticationToken authenticationToken, long j, int i) throws RemoteException {
        byte[] bArrLoadSyntheticPasswordHandle = loadSyntheticPasswordHandle(i);
        if (bArrLoadSyntheticPasswordHandle == null) {
            return null;
        }
        GateKeeperResponse gateKeeperResponseVerifyChallenge = iGateKeeperService.verifyChallenge(i, j, bArrLoadSyntheticPasswordHandle, authenticationToken.deriveGkPassword());
        int responseCode = gateKeeperResponseVerifyChallenge.getResponseCode();
        if (responseCode == 0) {
            VerifyCredentialResponse verifyCredentialResponse = new VerifyCredentialResponse(gateKeeperResponseVerifyChallenge.getPayload());
            if (gateKeeperResponseVerifyChallenge.getShouldReEnroll()) {
                GateKeeperResponse gateKeeperResponseEnroll = iGateKeeperService.enroll(i, bArrLoadSyntheticPasswordHandle, bArrLoadSyntheticPasswordHandle, authenticationToken.deriveGkPassword());
                if (gateKeeperResponseEnroll.getResponseCode() == 0) {
                    saveSyntheticPasswordHandle(gateKeeperResponseEnroll.getPayload(), i);
                    return verifyChallenge(iGateKeeperService, authenticationToken, j, i);
                }
                Log.w(TAG, "Fail to re-enroll SP handle for user " + i);
                return verifyCredentialResponse;
            }
            return verifyCredentialResponse;
        }
        if (responseCode == 1) {
            return new VerifyCredentialResponse(gateKeeperResponseVerifyChallenge.getTimeout());
        }
        return VerifyCredentialResponse.ERROR;
    }

    public boolean existsHandle(long j, int i) {
        return hasState(SP_BLOB_NAME, j, i);
    }

    public void destroyTokenBasedSyntheticPassword(long j, int i) {
        destroySyntheticPassword(j, i);
        destroyState(SECDISCARDABLE_NAME, j, i);
    }

    public void destroyPasswordBasedSyntheticPassword(long j, int i) {
        destroySyntheticPassword(j, i);
        destroyState(SECDISCARDABLE_NAME, j, i);
        destroyState(PASSWORD_DATA_NAME, j, i);
    }

    private void destroySyntheticPassword(long j, int i) {
        destroyState(SP_BLOB_NAME, j, i);
        destroySPBlobKey(getHandleName(j));
        if (hasState(WEAVER_SLOT_NAME, j, i)) {
            destroyWeaverSlot(j, i);
        }
    }

    private byte[] transformUnderWeaverSecret(byte[] bArr, byte[] bArr2) {
        byte[] bArrPersonalisedHash = SyntheticPasswordCrypto.personalisedHash(PERSONALISATION_WEAVER_PASSWORD, bArr2);
        byte[] bArr3 = new byte[bArr.length + bArrPersonalisedHash.length];
        System.arraycopy(bArr, 0, bArr3, 0, bArr.length);
        System.arraycopy(bArrPersonalisedHash, 0, bArr3, bArr.length, bArrPersonalisedHash.length);
        return bArr3;
    }

    private byte[] transformUnderSecdiscardable(byte[] bArr, byte[] bArr2) {
        byte[] bArrPersonalisedHash = SyntheticPasswordCrypto.personalisedHash(PERSONALISATION_SECDISCARDABLE, bArr2);
        byte[] bArr3 = new byte[bArr.length + bArrPersonalisedHash.length];
        System.arraycopy(bArr, 0, bArr3, 0, bArr.length);
        System.arraycopy(bArrPersonalisedHash, 0, bArr3, bArr.length, bArrPersonalisedHash.length);
        return bArr3;
    }

    private byte[] createSecdiscardable(long j, int i) {
        byte[] bArrSecureRandom = secureRandom(16384);
        saveSecdiscardable(j, bArrSecureRandom, i);
        return bArrSecureRandom;
    }

    private void saveSecdiscardable(long j, byte[] bArr, int i) {
        saveState(SECDISCARDABLE_NAME, bArr, j, i);
    }

    private byte[] loadSecdiscardable(long j, int i) {
        return loadState(SECDISCARDABLE_NAME, j, i);
    }

    private boolean hasState(String str, long j, int i) {
        return !ArrayUtils.isEmpty(loadState(str, j, i));
    }

    private byte[] loadState(String str, long j, int i) {
        return this.mStorage.readSyntheticPasswordState(i, j, str);
    }

    private void saveState(String str, byte[] bArr, long j, int i) {
        this.mStorage.writeSyntheticPasswordState(i, j, str, bArr);
    }

    private void destroyState(String str, long j, int i) {
        this.mStorage.deleteSyntheticPasswordState(i, j, str);
    }

    protected byte[] decryptSPBlob(String str, byte[] bArr, byte[] bArr2) {
        return SyntheticPasswordCrypto.decryptBlob(str, bArr, bArr2);
    }

    protected byte[] createSPBlob(String str, byte[] bArr, byte[] bArr2, long j) {
        return SyntheticPasswordCrypto.createBlob(str, bArr, bArr2, j);
    }

    protected void destroySPBlobKey(String str) {
        SyntheticPasswordCrypto.destroyBlobKey(str);
    }

    public static long generateHandle() {
        long jNextLong;
        SecureRandom secureRandom = new SecureRandom();
        do {
            jNextLong = secureRandom.nextLong();
        } while (jNextLong == 0);
        return jNextLong;
    }

    private int fakeUid(int i) {
        return 100000 + i;
    }

    protected static byte[] secureRandom(int i) {
        try {
            return SecureRandom.getInstance("SHA1PRNG").generateSeed(i);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getHandleName(long j) {
        return String.format("%s%x", "synthetic_password_", Long.valueOf(j));
    }

    private byte[] computePasswordToken(String str, PasswordData passwordData) {
        return scrypt(str, passwordData.salt, 1 << passwordData.scryptN, 1 << passwordData.scryptR, 1 << passwordData.scryptP, 32);
    }

    private byte[] passwordTokenToGkInput(byte[] bArr) {
        return SyntheticPasswordCrypto.personalisedHash(PERSONALIZATION_USER_GK_AUTH, bArr);
    }

    private byte[] passwordTokenToWeaverKey(byte[] bArr) {
        byte[] bArrPersonalisedHash = SyntheticPasswordCrypto.personalisedHash(PERSONALISATION_WEAVER_KEY, bArr);
        if (bArrPersonalisedHash.length < this.mWeaverConfig.keySize) {
            throw new RuntimeException("weaver key length too small");
        }
        return Arrays.copyOf(bArrPersonalisedHash, this.mWeaverConfig.keySize);
    }

    protected long sidFromPasswordHandle(byte[] bArr) {
        return nativeSidFromPasswordHandle(bArr);
    }

    protected byte[] scrypt(String str, byte[] bArr, int i, int i2, int i3, int i4) {
        return nativeScrypt(str.getBytes(), bArr, i, i2, i3, i4);
    }

    protected static ArrayList<Byte> toByteArrayList(byte[] bArr) {
        ArrayList<Byte> arrayList = new ArrayList<>(bArr.length);
        for (byte b : bArr) {
            arrayList.add(Byte.valueOf(b));
        }
        return arrayList;
    }

    protected static byte[] fromByteArrayList(ArrayList<Byte> arrayList) {
        byte[] bArr = new byte[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            bArr[i] = arrayList.get(i).byteValue();
        }
        return bArr;
    }

    public static String bytesToHex(byte[] bArr) {
        if (bArr == null) {
            return "null";
        }
        char[] cArr = new char[bArr.length * 2];
        for (int i = 0; i < bArr.length; i++) {
            int i2 = bArr[i] & 255;
            int i3 = i * 2;
            cArr[i3] = hexArray[i2 >>> 4];
            cArr[i3 + 1] = hexArray[i2 & 15];
        }
        return new String(cArr);
    }
}
