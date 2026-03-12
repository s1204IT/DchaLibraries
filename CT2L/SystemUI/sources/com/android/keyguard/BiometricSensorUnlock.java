package com.android.keyguard;

import android.view.View;

interface BiometricSensorUnlock {
    void initializeView(View view);

    boolean start();

    boolean stop();

    void stopAndShowBackup();
}
