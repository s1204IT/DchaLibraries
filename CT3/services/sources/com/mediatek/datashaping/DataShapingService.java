package com.mediatek.datashaping;

import android.content.Context;
import android.util.Slog;
import com.android.server.SystemService;

public class DataShapingService extends SystemService {
    private final String TAG;
    private DataShapingServiceImpl mImpl;

    public DataShapingService(Context context) {
        super(context);
        this.TAG = "DataShapingService";
        this.mImpl = new DataShapingServiceImpl(context);
    }

    @Override
    public void onStart() {
        Slog.d("DataShapingService", "Start DataShaping Service.");
        publishBinderService("data_shaping", this.mImpl);
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase != 500) {
            return;
        }
        this.mImpl.start();
    }
}
