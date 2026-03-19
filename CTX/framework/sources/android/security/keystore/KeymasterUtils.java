package android.security.keystore;

import android.hardware.fingerprint.FingerprintManager;
import android.security.GateKeeper;
import android.security.KeyStore;
import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.KeymasterDefs;
import android.security.keystore.KeyProperties;
import com.android.internal.logging.nano.MetricsProto;
import java.security.ProviderException;

public abstract class KeymasterUtils {
    private KeymasterUtils() {
    }

    public static int getDigestOutputSizeBits(int i) {
        switch (i) {
            case 0:
                return -1;
            case 1:
                return 128;
            case 2:
                return 160;
            case 3:
                return 224;
            case 4:
                return 256;
            case 5:
                return MetricsProto.MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION;
            case 6:
                return 512;
            default:
                throw new IllegalArgumentException("Unknown digest: " + i);
        }
    }

    public static boolean isKeymasterBlockModeIndCpaCompatibleWithSymmetricCrypto(int i) {
        if (i != 32) {
            switch (i) {
                case 1:
                    return false;
                case 2:
                case 3:
                    return true;
                default:
                    throw new IllegalArgumentException("Unsupported block mode: " + i);
            }
        }
        return true;
    }

    public static boolean isKeymasterPaddingSchemeIndCpaCompatibleWithAsymmetricCrypto(int i) {
        if (i != 4) {
            switch (i) {
                case 1:
                    return false;
                case 2:
                    return true;
                default:
                    throw new IllegalArgumentException("Unsupported asymmetric encryption padding scheme: " + i);
            }
        }
        return true;
    }

    public static void addUserAuthArgs(KeymasterArguments keymasterArguments, UserAuthArgs userAuthArgs) {
        long rootSid;
        if (userAuthArgs.isUserConfirmationRequired()) {
            keymasterArguments.addBoolean(KeymasterDefs.KM_TAG_TRUSTED_CONFIRMATION_REQUIRED);
        }
        if (userAuthArgs.isUserPresenceRequired()) {
            keymasterArguments.addBoolean(KeymasterDefs.KM_TAG_TRUSTED_USER_PRESENCE_REQUIRED);
        }
        if (userAuthArgs.isUnlockedDeviceRequired()) {
            keymasterArguments.addBoolean(KeymasterDefs.KM_TAG_UNLOCKED_DEVICE_REQUIRED);
        }
        if (!userAuthArgs.isUserAuthenticationRequired()) {
            keymasterArguments.addBoolean(KeymasterDefs.KM_TAG_NO_AUTH_REQUIRED);
            return;
        }
        if (userAuthArgs.getUserAuthenticationValidityDurationSeconds() == -1) {
            FingerprintManager fingerprintManager = (FingerprintManager) KeyStore.getApplicationContext().getSystemService(FingerprintManager.class);
            long authenticatorId = fingerprintManager != null ? fingerprintManager.getAuthenticatorId() : 0L;
            if (authenticatorId == 0) {
                throw new IllegalStateException("At least one fingerprint must be enrolled to create keys requiring user authentication for every use");
            }
            if (userAuthArgs.getBoundToSpecificSecureUserId() != 0) {
                authenticatorId = userAuthArgs.getBoundToSpecificSecureUserId();
            } else if (!userAuthArgs.isInvalidatedByBiometricEnrollment()) {
                authenticatorId = getRootSid();
            }
            keymasterArguments.addUnsignedLong(KeymasterDefs.KM_TAG_USER_SECURE_ID, KeymasterArguments.toUint64(authenticatorId));
            keymasterArguments.addEnum(KeymasterDefs.KM_TAG_USER_AUTH_TYPE, 2);
            if (userAuthArgs.isUserAuthenticationValidWhileOnBody()) {
                throw new ProviderException("Key validity extension while device is on-body is not supported for keys requiring fingerprint authentication");
            }
            return;
        }
        if (userAuthArgs.getBoundToSpecificSecureUserId() != 0) {
            rootSid = userAuthArgs.getBoundToSpecificSecureUserId();
        } else {
            rootSid = getRootSid();
        }
        keymasterArguments.addUnsignedLong(KeymasterDefs.KM_TAG_USER_SECURE_ID, KeymasterArguments.toUint64(rootSid));
        keymasterArguments.addEnum(KeymasterDefs.KM_TAG_USER_AUTH_TYPE, 3);
        keymasterArguments.addUnsignedInt(KeymasterDefs.KM_TAG_AUTH_TIMEOUT, userAuthArgs.getUserAuthenticationValidityDurationSeconds());
        if (userAuthArgs.isUserAuthenticationValidWhileOnBody()) {
            keymasterArguments.addBoolean(KeymasterDefs.KM_TAG_ALLOW_WHILE_ON_BODY);
        }
    }

    public static void addMinMacLengthAuthorizationIfNecessary(KeymasterArguments keymasterArguments, int i, int[] iArr, int[] iArr2) {
        if (i == 32) {
            if (com.android.internal.util.ArrayUtils.contains(iArr, 32)) {
                keymasterArguments.addUnsignedInt(KeymasterDefs.KM_TAG_MIN_MAC_LENGTH, 96L);
                return;
            }
            return;
        }
        if (i == 128) {
            if (iArr2.length != 1) {
                throw new ProviderException("Unsupported number of authorized digests for HMAC key: " + iArr2.length + ". Exactly one digest must be authorized");
            }
            int i2 = iArr2[0];
            int digestOutputSizeBits = getDigestOutputSizeBits(i2);
            if (digestOutputSizeBits != -1) {
                keymasterArguments.addUnsignedInt(KeymasterDefs.KM_TAG_MIN_MAC_LENGTH, digestOutputSizeBits);
                return;
            }
            throw new ProviderException("HMAC key authorized for unsupported digest: " + KeyProperties.Digest.fromKeymaster(i2));
        }
    }

    private static long getRootSid() {
        long secureUserId = GateKeeper.getSecureUserId();
        if (secureUserId == 0) {
            throw new IllegalStateException("Secure lock screen must be enabled to create keys requiring user authentication");
        }
        return secureUserId;
    }
}
