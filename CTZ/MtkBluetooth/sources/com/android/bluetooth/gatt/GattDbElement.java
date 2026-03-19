package com.android.bluetooth.gatt;

import java.util.UUID;

public class GattDbElement {
    public static final int TYPE_CHARACTERISTIC = 3;
    public static final int TYPE_DESCRIPTOR = 4;
    public static final int TYPE_INCLUDED_SERVICE = 2;
    public static final int TYPE_PRIMARY_SERVICE = 0;
    public static final int TYPE_SECONDARY_SERVICE = 1;
    public int attributeHandle;
    public int endHandle;
    public int id;
    public int permissions;
    public int properties;
    public int startHandle;
    public int type;
    public UUID uuid;

    public static GattDbElement createPrimaryService(UUID uuid) {
        GattDbElement gattDbElement = new GattDbElement();
        gattDbElement.type = 0;
        gattDbElement.uuid = uuid;
        return gattDbElement;
    }

    public static GattDbElement createSecondaryService(UUID uuid) {
        GattDbElement gattDbElement = new GattDbElement();
        gattDbElement.type = 1;
        gattDbElement.uuid = uuid;
        return gattDbElement;
    }

    public static GattDbElement createCharacteristic(UUID uuid, int i, int i2) {
        GattDbElement gattDbElement = new GattDbElement();
        gattDbElement.type = 3;
        gattDbElement.uuid = uuid;
        gattDbElement.properties = i;
        gattDbElement.permissions = i2;
        return gattDbElement;
    }

    public static GattDbElement createDescriptor(UUID uuid, int i) {
        GattDbElement gattDbElement = new GattDbElement();
        gattDbElement.type = 4;
        gattDbElement.uuid = uuid;
        gattDbElement.permissions = i;
        return gattDbElement;
    }

    public static GattDbElement createIncludedService(int i) {
        GattDbElement gattDbElement = new GattDbElement();
        gattDbElement.type = 2;
        gattDbElement.attributeHandle = i;
        return gattDbElement;
    }
}
