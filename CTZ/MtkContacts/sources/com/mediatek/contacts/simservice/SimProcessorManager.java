package com.mediatek.contacts.simservice;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import com.android.contacts.vcard.ProcessorBase;
import com.mediatek.contacts.util.Log;
import java.util.concurrent.ConcurrentHashMap;

public class SimProcessorManager {
    private ConcurrentHashMap<Integer, SimProcessorBase> mEditDeleteProcessors;
    private Handler mHandler;
    private ProcessorManagerListener mListener;
    private ProcessorCompleteListener mProcessoListener = new ProcessorCompleteListener() {
        @Override
        public void onProcessorCompleted(Intent intent) {
            if (intent != null) {
                int intExtra = intent.getIntExtra("subscription_key", 0);
                Log.d("SIMProcessorManager", "[onProcessorCompleted] subId = " + intExtra + ",time=" + System.currentTimeMillis() + ", workType = " + intent.getIntExtra("work_type", -1));
                synchronized (SimProcessorManager.this.mProcessorRemoveLock) {
                    if (SimProcessorManager.this.mEditDeleteProcessors.containsKey(Integer.valueOf(intExtra))) {
                        Log.d("SIMProcessorManager", "[onProcessorCompleted] remove other processor subId=" + intExtra);
                        if (((SimProcessorBase) SimProcessorManager.this.mEditDeleteProcessors.get(Integer.valueOf(intExtra))).identifyIntent(intent)) {
                            SimProcessorManager.this.mEditDeleteProcessors.remove(Integer.valueOf(intExtra));
                            SimProcessorManager.this.checkStopService();
                        } else {
                            Log.w("SIMProcessorManager", "[onProcessorCompleted] race condition2");
                        }
                    } else {
                        Log.w("SIMProcessorManager", "[onProcessorCompleted] slotId processor not found");
                    }
                }
            }
        }
    };
    private final Object mProcessorRemoveLock = new Object();

    public interface ProcessorCompleteListener {
        void onProcessorCompleted(Intent intent);
    }

    public interface ProcessorManagerListener {
        void addProcessor(long j, ProcessorBase processorBase);

        void onAllProcessorsFinished();
    }

    public SimProcessorManager(Context context, ProcessorManagerListener processorManagerListener) {
        Log.i("SIMProcessorManager", "[SIMProcessorManager]new...");
        this.mListener = processorManagerListener;
        this.mEditDeleteProcessors = new ConcurrentHashMap<>();
        this.mHandler = new Handler(context.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 1) {
                    SimProcessorManager.this.callStopService();
                }
            }
        };
    }

    public void handleProcessor(Context context, int i, int i2, Intent intent) {
        Log.i("SIMProcessorManager", "[handleProcessor] subId=" + i + ",time=" + System.currentTimeMillis());
        SimProcessorBase simProcessorBaseCreateProcessor = createProcessor(context, i, i2, intent);
        if (simProcessorBaseCreateProcessor != null && this.mListener != null) {
            Log.d("SIMProcessorManager", "[handleProcessor]Add processor [subId=" + i + "] to threadPool.");
            this.mListener.addProcessor(0L, simProcessorBaseCreateProcessor);
        }
    }

    private SimProcessorBase createProcessor(Context context, int i, int i2, Intent intent) {
        SimProcessorBase simProcessorBaseCreateProcessor;
        Log.d("SIMProcessorManager", "[createProcessor]subId = " + i + ",workType = " + i2);
        synchronized (this.mProcessorRemoveLock) {
            simProcessorBaseCreateProcessor = createProcessor(context, i, i2, intent, this.mProcessoListener);
            this.mEditDeleteProcessors.put(Integer.valueOf(i), simProcessorBaseCreateProcessor);
        }
        return simProcessorBaseCreateProcessor;
    }

    private SimProcessorBase createProcessor(Context context, int i, int i2, Intent intent, ProcessorCompleteListener processorCompleteListener) {
        Log.d("SIMProcessorManager", "[createProcessor] create new processor for subId: " + i + ", workType: " + i2);
        if (i2 == 1) {
            return new SimEditProcessor(context, i, intent, processorCompleteListener);
        }
        if (i2 == 2) {
            return new SimDeleteProcessor(context, i, intent, processorCompleteListener);
        }
        if (i2 == 3) {
            return new SimGroupProcessor(context, i, intent, processorCompleteListener);
        }
        return null;
    }

    private void checkStopService() {
        Log.v("SIMProcessorManager", "[checkStopService]...");
        if (this.mEditDeleteProcessors.size() == 0 && this.mHandler != null) {
            Log.v("SIMProcessorManager", "[checkStopService] send stop service message.");
            this.mHandler.removeMessages(1);
            this.mHandler.sendEmptyMessageDelayed(1, 200L);
        }
    }

    private void callStopService() {
        Log.d("SIMProcessorManager", "[callStopService]...");
        if (this.mListener != null && this.mEditDeleteProcessors.size() == 0) {
            this.mListener.onAllProcessorsFinished();
        }
    }

    public void onAddProcessorFail(SimProcessorBase simProcessorBase) {
        synchronized (this.mProcessorRemoveLock) {
            this.mEditDeleteProcessors.values().remove(simProcessorBase);
        }
        if (simProcessorBase.getType() == 1) {
            ((SimEditProcessor) simProcessorBase).onAddToServiceFail();
        }
    }
}
