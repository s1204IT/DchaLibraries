package android.bluetooth;

import android.os.ParcelUuid;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

public final class BluetoothUuid {
    public static final int UUID_BYTES_128_BIT = 16;
    public static final int UUID_BYTES_16_BIT = 2;
    public static final int UUID_BYTES_32_BIT = 4;
    public static final ParcelUuid AudioSink = ParcelUuid.fromString("0000110B-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid AudioSource = ParcelUuid.fromString("0000110A-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid AdvAudioDist = ParcelUuid.fromString("0000110D-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid HSP = ParcelUuid.fromString("00001108-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid HSP_AG = ParcelUuid.fromString("00001112-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid Handsfree = ParcelUuid.fromString("0000111E-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid Handsfree_AG = ParcelUuid.fromString("0000111F-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid AvrcpController = ParcelUuid.fromString("0000110E-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid AvrcpTarget = ParcelUuid.fromString("0000110C-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid ObexObjectPush = ParcelUuid.fromString("00001105-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid Hid = ParcelUuid.fromString("00001124-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid Hogp = ParcelUuid.fromString("00001812-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid PANU = ParcelUuid.fromString("00001115-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid NAP = ParcelUuid.fromString("00001116-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid BNEP = ParcelUuid.fromString("0000000f-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid PBAP_PCE = ParcelUuid.fromString("0000112e-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid PBAP_PSE = ParcelUuid.fromString("0000112f-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid MAP = ParcelUuid.fromString("00001134-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid MNS = ParcelUuid.fromString("00001133-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid MAS = ParcelUuid.fromString("00001132-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid SAP = ParcelUuid.fromString("0000112D-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid HearingAid = ParcelUuid.fromString("0000FDF0-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid BASE_UUID = ParcelUuid.fromString("00000000-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid[] RESERVED_UUIDS = {AudioSink, AudioSource, AdvAudioDist, HSP, Handsfree, AvrcpController, AvrcpTarget, ObexObjectPush, PANU, NAP, MAP, MNS, MAS, SAP};

    public static boolean isAudioSource(ParcelUuid parcelUuid) {
        return parcelUuid.equals(AudioSource);
    }

    public static boolean isAudioSink(ParcelUuid parcelUuid) {
        return parcelUuid.equals(AudioSink);
    }

    public static boolean isAdvAudioDist(ParcelUuid parcelUuid) {
        return parcelUuid.equals(AdvAudioDist);
    }

    public static boolean isHandsfree(ParcelUuid parcelUuid) {
        return parcelUuid.equals(Handsfree);
    }

    public static boolean isHeadset(ParcelUuid parcelUuid) {
        return parcelUuid.equals(HSP);
    }

    public static boolean isAvrcpController(ParcelUuid parcelUuid) {
        return parcelUuid.equals(AvrcpController);
    }

    public static boolean isAvrcpTarget(ParcelUuid parcelUuid) {
        return parcelUuid.equals(AvrcpTarget);
    }

    public static boolean isInputDevice(ParcelUuid parcelUuid) {
        return parcelUuid.equals(Hid);
    }

    public static boolean isPanu(ParcelUuid parcelUuid) {
        return parcelUuid.equals(PANU);
    }

    public static boolean isNap(ParcelUuid parcelUuid) {
        return parcelUuid.equals(NAP);
    }

    public static boolean isBnep(ParcelUuid parcelUuid) {
        return parcelUuid.equals(BNEP);
    }

    public static boolean isMap(ParcelUuid parcelUuid) {
        return parcelUuid.equals(MAP);
    }

    public static boolean isMns(ParcelUuid parcelUuid) {
        return parcelUuid.equals(MNS);
    }

    public static boolean isMas(ParcelUuid parcelUuid) {
        return parcelUuid.equals(MAS);
    }

    public static boolean isSap(ParcelUuid parcelUuid) {
        return parcelUuid.equals(SAP);
    }

    public static boolean isUuidPresent(ParcelUuid[] parcelUuidArr, ParcelUuid parcelUuid) {
        if ((parcelUuidArr == null || parcelUuidArr.length == 0) && parcelUuid == null) {
            return true;
        }
        if (parcelUuidArr == null) {
            return false;
        }
        for (ParcelUuid parcelUuid2 : parcelUuidArr) {
            if (parcelUuid2.equals(parcelUuid)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsAnyUuid(ParcelUuid[] parcelUuidArr, ParcelUuid[] parcelUuidArr2) {
        if (parcelUuidArr == null && parcelUuidArr2 == null) {
            return true;
        }
        if (parcelUuidArr == null) {
            if (parcelUuidArr2.length == 0) {
                return true;
            }
            return false;
        }
        if (parcelUuidArr2 == null) {
            if (parcelUuidArr.length == 0) {
                return true;
            }
            return false;
        }
        HashSet hashSet = new HashSet(Arrays.asList(parcelUuidArr));
        for (ParcelUuid parcelUuid : parcelUuidArr2) {
            if (hashSet.contains(parcelUuid)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsAllUuids(ParcelUuid[] parcelUuidArr, ParcelUuid[] parcelUuidArr2) {
        if (parcelUuidArr == null && parcelUuidArr2 == null) {
            return true;
        }
        if (parcelUuidArr == null) {
            if (parcelUuidArr2.length == 0) {
                return true;
            }
            return false;
        }
        if (parcelUuidArr2 == null) {
            return true;
        }
        HashSet hashSet = new HashSet(Arrays.asList(parcelUuidArr));
        for (ParcelUuid parcelUuid : parcelUuidArr2) {
            if (!hashSet.contains(parcelUuid)) {
                return false;
            }
        }
        return true;
    }

    public static int getServiceIdentifierFromParcelUuid(ParcelUuid parcelUuid) {
        return (int) ((parcelUuid.getUuid().getMostSignificantBits() & (-4294967296L)) >>> 32);
    }

    public static ParcelUuid parseUuidFrom(byte[] bArr) {
        long j;
        if (bArr == null) {
            throw new IllegalArgumentException("uuidBytes cannot be null");
        }
        int length = bArr.length;
        if (length == 2 || length == 4 || length == 16) {
            if (length == 16) {
                ByteBuffer byteBufferOrder = ByteBuffer.wrap(bArr).order(ByteOrder.LITTLE_ENDIAN);
                return new ParcelUuid(new UUID(byteBufferOrder.getLong(8), byteBufferOrder.getLong(0)));
            }
            if (length != 2) {
                j = ((long) ((bArr[3] & 255) << 24)) + ((long) (bArr[0] & 255)) + ((long) ((bArr[1] & 255) << 8)) + ((long) ((bArr[2] & 255) << 16));
            } else {
                j = ((long) (bArr[0] & 255)) + ((long) ((bArr[1] & 255) << 8));
            }
            return new ParcelUuid(new UUID(BASE_UUID.getUuid().getMostSignificantBits() + (j << 32), BASE_UUID.getUuid().getLeastSignificantBits()));
        }
        throw new IllegalArgumentException("uuidBytes length invalid - " + length);
    }

    public static byte[] uuidToBytes(ParcelUuid parcelUuid) {
        if (parcelUuid == null) {
            throw new IllegalArgumentException("uuid cannot be null");
        }
        if (is16BitUuid(parcelUuid)) {
            int serviceIdentifierFromParcelUuid = getServiceIdentifierFromParcelUuid(parcelUuid);
            return new byte[]{(byte) (serviceIdentifierFromParcelUuid & 255), (byte) ((serviceIdentifierFromParcelUuid & 65280) >> 8)};
        }
        if (is32BitUuid(parcelUuid)) {
            int serviceIdentifierFromParcelUuid2 = getServiceIdentifierFromParcelUuid(parcelUuid);
            return new byte[]{(byte) (serviceIdentifierFromParcelUuid2 & 255), (byte) ((65280 & serviceIdentifierFromParcelUuid2) >> 8), (byte) ((16711680 & serviceIdentifierFromParcelUuid2) >> 16), (byte) ((serviceIdentifierFromParcelUuid2 & (-16777216)) >> 24)};
        }
        long mostSignificantBits = parcelUuid.getUuid().getMostSignificantBits();
        long leastSignificantBits = parcelUuid.getUuid().getLeastSignificantBits();
        byte[] bArr = new byte[16];
        ByteBuffer byteBufferOrder = ByteBuffer.wrap(bArr).order(ByteOrder.LITTLE_ENDIAN);
        byteBufferOrder.putLong(8, mostSignificantBits);
        byteBufferOrder.putLong(0, leastSignificantBits);
        return bArr;
    }

    public static boolean is16BitUuid(ParcelUuid parcelUuid) {
        UUID uuid = parcelUuid.getUuid();
        return uuid.getLeastSignificantBits() == BASE_UUID.getUuid().getLeastSignificantBits() && (uuid.getMostSignificantBits() & (-281470681743361L)) == 4096;
    }

    public static boolean is32BitUuid(ParcelUuid parcelUuid) {
        UUID uuid = parcelUuid.getUuid();
        return uuid.getLeastSignificantBits() == BASE_UUID.getUuid().getLeastSignificantBits() && !is16BitUuid(parcelUuid) && (uuid.getMostSignificantBits() & 4294967295L) == 4096;
    }
}
