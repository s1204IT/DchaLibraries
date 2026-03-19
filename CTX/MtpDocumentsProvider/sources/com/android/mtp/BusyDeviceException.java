package com.android.mtp;

import java.io.IOException;

class BusyDeviceException extends IOException {
    BusyDeviceException() {
        super("The MTP device is busy.");
    }
}
