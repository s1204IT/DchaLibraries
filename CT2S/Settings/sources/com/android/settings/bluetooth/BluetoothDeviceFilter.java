package com.android.settings.bluetooth;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.os.ParcelUuid;
import android.util.Log;

final class BluetoothDeviceFilter {
    static final Filter ALL_FILTER;
    static final Filter BONDED_DEVICE_FILTER;
    private static final Filter[] FILTERS;
    static final Filter UNBONDED_DEVICE_FILTER;

    interface Filter {
        boolean matches(BluetoothDevice bluetoothDevice);
    }

    static {
        ALL_FILTER = new AllFilter();
        BONDED_DEVICE_FILTER = new BondedDeviceFilter();
        UNBONDED_DEVICE_FILTER = new UnbondedDeviceFilter();
        FILTERS = new Filter[]{ALL_FILTER, new AudioFilter(), new TransferFilter(), new PanuFilter(), new NapFilter()};
    }

    private BluetoothDeviceFilter() {
    }

    static Filter getFilter(int filterType) {
        if (filterType >= 0 && filterType < FILTERS.length) {
            return FILTERS[filterType];
        }
        Log.w("BluetoothDeviceFilter", "Invalid filter type " + filterType + " for device picker");
        return ALL_FILTER;
    }

    private static final class AllFilter implements Filter {
        private AllFilter() {
        }

        @Override
        public boolean matches(BluetoothDevice device) {
            return true;
        }
    }

    private static final class BondedDeviceFilter implements Filter {
        private BondedDeviceFilter() {
        }

        @Override
        public boolean matches(BluetoothDevice device) {
            return device.getBondState() == 12;
        }
    }

    private static final class UnbondedDeviceFilter implements Filter {
        private UnbondedDeviceFilter() {
        }

        @Override
        public boolean matches(BluetoothDevice device) {
            return device.getBondState() != 12;
        }
    }

    private static abstract class ClassUuidFilter implements Filter {
        abstract boolean matches(ParcelUuid[] parcelUuidArr, BluetoothClass bluetoothClass);

        private ClassUuidFilter() {
        }

        @Override
        public boolean matches(BluetoothDevice device) {
            return matches(device.getUuids(), device.getBluetoothClass());
        }
    }

    private static final class AudioFilter extends ClassUuidFilter {
        private AudioFilter() {
            super();
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
        private TransferFilter() {
            super();
        }

        @Override
        boolean matches(ParcelUuid[] uuids, BluetoothClass btClass) {
            if (uuids == null || !BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.ObexObjectPush)) {
                return btClass != null && btClass.doesClassMatch(2);
            }
            return true;
        }
    }

    private static final class PanuFilter extends ClassUuidFilter {
        private PanuFilter() {
            super();
        }

        @Override
        boolean matches(ParcelUuid[] uuids, BluetoothClass btClass) {
            if (uuids == null || !BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.PANU)) {
                return btClass != null && btClass.doesClassMatch(4);
            }
            return true;
        }
    }

    private static final class NapFilter extends ClassUuidFilter {
        private NapFilter() {
            super();
        }

        @Override
        boolean matches(ParcelUuid[] uuids, BluetoothClass btClass) {
            if (uuids == null || !BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.NAP)) {
                return btClass != null && btClass.doesClassMatch(5);
            }
            return true;
        }
    }
}
