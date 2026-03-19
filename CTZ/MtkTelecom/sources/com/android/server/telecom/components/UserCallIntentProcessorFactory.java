package com.android.server.telecom.components;

import android.content.Context;
import android.os.UserHandle;

public interface UserCallIntentProcessorFactory {
    UserCallIntentProcessor create(Context context, UserHandle userHandle);
}
