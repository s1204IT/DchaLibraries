package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.os.ParcelUuid;
import android.util.Log;

public final class BluetoothDeviceFilter {
    public static final Filter ALL_FILTER = new AllFilter(null);
    public static final Filter BONDED_DEVICE_FILTER = new BondedDeviceFilter(0 == true ? 1 : 0);
    public static final Filter UNBONDED_DEVICE_FILTER = new UnbondedDeviceFilter(0 == true ? 1 : 0);
    private static final Filter[] FILTERS = {ALL_FILTER, new AudioFilter(0 == true ? 1 : 0), new TransferFilter(0 == true ? 1 : 0), new PanuFilter(0 == true ? 1 : 0), new NapFilter(0 == true ? 1 : 0)};

    public interface Filter {
        boolean matches(BluetoothDevice bluetoothDevice);
    }

    private BluetoothDeviceFilter() {
    }

    public static Filter getFilter(int filterType) {
        if (filterType >= 0 && filterType < FILTERS.length) {
            return FILTERS[filterType];
        }
        Log.w("BluetoothDeviceFilter", "Invalid filter type " + filterType + " for device picker");
        return ALL_FILTER;
    }

    private static final class AllFilter implements Filter {
        AllFilter(AllFilter allFilter) {
            this();
        }

        private AllFilter() {
        }

        @Override
        public boolean matches(BluetoothDevice device) {
            return true;
        }
    }

    private static final class BondedDeviceFilter implements Filter {
        BondedDeviceFilter(BondedDeviceFilter bondedDeviceFilter) {
            this();
        }

        private BondedDeviceFilter() {
        }

        @Override
        public boolean matches(BluetoothDevice device) {
            return device.getBondState() == 12;
        }
    }

    private static final class UnbondedDeviceFilter implements Filter {
        UnbondedDeviceFilter(UnbondedDeviceFilter unbondedDeviceFilter) {
            this();
        }

        private UnbondedDeviceFilter() {
        }

        @Override
        public boolean matches(BluetoothDevice device) {
            return device.getBondState() != 12;
        }
    }

    private static abstract class ClassUuidFilter implements Filter {
        ClassUuidFilter(ClassUuidFilter classUuidFilter) {
            this();
        }

        abstract boolean matches(ParcelUuid[] parcelUuidArr, BluetoothClass bluetoothClass);

        private ClassUuidFilter() {
        }

        @Override
        public boolean matches(BluetoothDevice device) {
            return matches(device.getUuids(), device.getBluetoothClass());
        }
    }

    private static final class AudioFilter extends ClassUuidFilter {
        AudioFilter(AudioFilter audioFilter) {
            this();
        }

        private AudioFilter() {
            super(null);
        }

        @Override
        boolean matches(ParcelUuid[] uuids, BluetoothClass btClass) {
            if (uuids != null) {
                if (BluetoothUuid.containsAnyUuid(uuids, A2dpProfile.SINK_UUIDS) || BluetoothUuid.containsAnyUuid(uuids, HeadsetProfile.UUIDS)) {
                    return true;
                }
            } else if (btClass != null && (btClass.doesClassMatch(1) || btClass.doesClassMatch(0))) {
                return true;
            }
            return false;
        }
    }

    private static final class TransferFilter extends ClassUuidFilter {
        TransferFilter(TransferFilter transferFilter) {
            this();
        }

        private TransferFilter() {
            super(null);
        }

        @Override
        boolean matches(ParcelUuid[] uuids, BluetoothClass btClass) {
            if (uuids != null && BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.ObexObjectPush)) {
                return true;
            }
            if (btClass != null) {
                return btClass.doesClassMatch(2);
            }
            return false;
        }
    }

    private static final class PanuFilter extends ClassUuidFilter {
        PanuFilter(PanuFilter panuFilter) {
            this();
        }

        private PanuFilter() {
            super(null);
        }

        @Override
        boolean matches(ParcelUuid[] uuids, BluetoothClass btClass) {
            if (uuids != null && BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.PANU)) {
                return true;
            }
            if (btClass != null) {
                return btClass.doesClassMatch(4);
            }
            return false;
        }
    }

    private static final class NapFilter extends ClassUuidFilter {
        NapFilter(NapFilter napFilter) {
            this();
        }

        private NapFilter() {
            super(null);
        }

        @Override
        boolean matches(ParcelUuid[] uuids, BluetoothClass btClass) {
            if (uuids != null && BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.NAP)) {
                return true;
            }
            if (btClass != null) {
                return btClass.doesClassMatch(5);
            }
            return false;
        }
    }
}
