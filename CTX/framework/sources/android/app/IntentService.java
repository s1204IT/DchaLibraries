package android.app;

import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

public abstract class IntentService extends Service {
    private String mName;
    private boolean mRedelivery;
    private volatile ServiceHandler mServiceHandler;
    private volatile Looper mServiceLooper;

    protected abstract void onHandleIntent(Intent intent);

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            IntentService.this.onHandleIntent((Intent) message.obj);
            IntentService.this.stopSelf(message.arg1);
        }
    }

    public IntentService(String str) {
        this.mName = str;
    }

    public void setIntentRedelivery(boolean z) {
        this.mRedelivery = z;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread handlerThread = new HandlerThread("IntentService[" + this.mName + "]");
        handlerThread.start();
        this.mServiceLooper = handlerThread.getLooper();
        this.mServiceHandler = new ServiceHandler(this.mServiceLooper);
    }

    @Override
    public void onStart(Intent intent, int i) {
        Message messageObtainMessage = this.mServiceHandler.obtainMessage();
        messageObtainMessage.arg1 = i;
        messageObtainMessage.obj = intent;
        this.mServiceHandler.sendMessage(messageObtainMessage);
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        onStart(intent, i2);
        return this.mRedelivery ? 3 : 2;
    }

    @Override
    public void onDestroy() {
        this.mServiceLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
