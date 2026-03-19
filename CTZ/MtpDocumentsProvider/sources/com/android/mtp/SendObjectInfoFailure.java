package com.android.mtp;

import java.io.IOException;

class SendObjectInfoFailure extends IOException {
    SendObjectInfoFailure() {
        super("Failed to MtpDevice#sendObjectInfo.");
    }
}
