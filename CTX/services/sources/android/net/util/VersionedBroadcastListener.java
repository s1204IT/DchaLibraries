package android.net.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class VersionedBroadcastListener {
    private static final boolean DBG = false;
    private final Consumer<Intent> mCallback;
    private final Context mContext;
    private final IntentFilter mFilter;
    private final AtomicInteger mGenerationNumber = new AtomicInteger(0);
    private final Handler mHandler;
    private BroadcastReceiver mReceiver;
    private final String mTag;

    public interface IntentCallback {
        void run(Intent intent);
    }

    public VersionedBroadcastListener(String str, Context context, Handler handler, IntentFilter intentFilter, Consumer<Intent> consumer) {
        this.mTag = str;
        this.mContext = context;
        this.mHandler = handler;
        this.mFilter = intentFilter;
        this.mCallback = consumer;
    }

    public void startListening() {
        if (this.mReceiver != null) {
            return;
        }
        this.mReceiver = new Receiver(this.mTag, this.mGenerationNumber, this.mCallback);
        this.mContext.registerReceiver(this.mReceiver, this.mFilter, null, this.mHandler);
    }

    public void stopListening() {
        if (this.mReceiver == null) {
            return;
        }
        this.mGenerationNumber.incrementAndGet();
        this.mContext.unregisterReceiver(this.mReceiver);
        this.mReceiver = null;
    }

    private static class Receiver extends BroadcastReceiver {
        public final AtomicInteger atomicGenerationNumber;
        public final Consumer<Intent> callback;
        public final int generationNumber;
        public final String tag;

        public Receiver(String str, AtomicInteger atomicInteger, Consumer<Intent> consumer) {
            this.tag = str;
            this.atomicGenerationNumber = atomicInteger;
            this.callback = consumer;
            this.generationNumber = atomicInteger.incrementAndGet();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (this.generationNumber != this.atomicGenerationNumber.get()) {
                return;
            }
            this.callback.accept(intent);
        }
    }
}
