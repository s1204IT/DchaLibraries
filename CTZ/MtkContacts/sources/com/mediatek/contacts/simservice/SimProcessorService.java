package com.mediatek.contacts.simservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.android.contacts.util.PermissionsUtil;
import com.android.contacts.vcard.ProcessorBase;
import com.mediatek.contacts.simservice.SimProcessorManager;
import com.mediatek.contacts.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SimProcessorService extends Service {
    private SimProcessorManager mProcessorManager;
    private AtomicInteger mNumber = new AtomicInteger();
    private final ExecutorService mExecutorService = createThreadPool(2);
    private SimProcessorManager.ProcessorManagerListener mListener = new SimProcessorManager.ProcessorManagerListener() {
        @Override
        public void addProcessor(long j, ProcessorBase processorBase) {
            if (processorBase != null) {
                try {
                    SimProcessorService.this.mExecutorService.execute(processorBase);
                } catch (RejectedExecutionException e) {
                    Log.w("SIMProcessorService", "[addProcessor] RejectedExecutionException: " + e.toString());
                    if (processorBase instanceof SimProcessorBase) {
                        SimProcessorService.this.mProcessorManager.onAddProcessorFail((SimProcessorBase) processorBase);
                    }
                }
            }
        }

        @Override
        public void onAllProcessorsFinished() {
            Log.d("SIMProcessorService", "[onAllProcessorsFinished]...");
            SimProcessorService.this.stopSelf();
            SimProcessorService.this.mExecutorService.shutdown();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("SIMProcessorService", "[onCreate]...");
        this.mProcessorManager = new SimProcessorManager(this, this.mListener);
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        super.onStartCommand(intent, i, i2);
        processIntent(intent);
        return 3;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("SIMProcessorService", "[onDestroy]...");
    }

    private void processIntent(Intent intent) {
        if (intent == null) {
            Log.w("SIMProcessorService", "[processIntent] intent is null.");
            return;
        }
        if (!PermissionsUtil.hasPermission(this, "android.permission.WRITE_CONTACTS")) {
            Log.w("SIMProcessorService", "No WRITE_CONTACTS permission, unable to handle intent:" + intent);
            return;
        }
        this.mProcessorManager.handleProcessor(getApplicationContext(), intent.getIntExtra("subscription_key", 0), intent.getIntExtra("work_type", -1), intent);
    }

    private ExecutorService createThreadPool(int i) {
        return new ThreadPoolExecutor(i, 10, 10L, TimeUnit.SECONDS, new SynchronousQueue(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                String str = "SIM Service - " + SimProcessorService.this.mNumber.getAndIncrement();
                Log.d("SIMProcessorService", "[createThreadPool]thread name:" + str);
                return new Thread(runnable, str);
            }
        });
    }
}
