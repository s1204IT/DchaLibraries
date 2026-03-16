package com.android.calendar;

import android.content.Context;
import android.os.Bundle;
import java.io.IOException;

public interface CloudNotificationBackplane {
    void close();

    boolean open(Context context);

    void send(String str, String str2, Bundle bundle) throws IOException;

    boolean subscribeToGroup(String str, String str2, String str3) throws IOException;
}
