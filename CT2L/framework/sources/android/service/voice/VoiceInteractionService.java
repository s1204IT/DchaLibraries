package android.service.voice;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.soundtrigger.KeyphraseEnrollmentInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.service.voice.AlwaysOnHotwordDetector;
import android.service.voice.IVoiceInteractionService;
import com.android.internal.app.IVoiceInteractionManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Locale;

public class VoiceInteractionService extends Service {
    static final int MSG_READY = 1;
    static final int MSG_SHUTDOWN = 2;
    static final int MSG_SOUND_MODELS_CHANGED = 3;
    public static final String SERVICE_INTERFACE = "android.service.voice.VoiceInteractionService";
    public static final String SERVICE_META_DATA = "android.voice_interaction";
    MyHandler mHandler;
    private AlwaysOnHotwordDetector mHotwordDetector;
    private KeyphraseEnrollmentInfo mKeyphraseEnrollmentInfo;
    IVoiceInteractionManagerService mSystemService;
    IVoiceInteractionService mInterface = new IVoiceInteractionService.Stub() {
        @Override
        public void ready() {
            VoiceInteractionService.this.mHandler.sendEmptyMessage(1);
        }

        @Override
        public void shutdown() {
            VoiceInteractionService.this.mHandler.sendEmptyMessage(2);
        }

        @Override
        public void soundModelsChanged() {
            VoiceInteractionService.this.mHandler.sendEmptyMessage(3);
        }
    };
    private final Object mLock = new Object();

    class MyHandler extends Handler {
        MyHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    VoiceInteractionService.this.onReady();
                    break;
                case 2:
                    VoiceInteractionService.this.onShutdownInternal();
                    break;
                case 3:
                    VoiceInteractionService.this.onSoundModelsChangedInternal();
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    public static boolean isActiveService(Context context, ComponentName service) {
        ComponentName curComp;
        String cur = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.VOICE_INTERACTION_SERVICE);
        if (cur == null || cur.isEmpty() || (curComp = ComponentName.unflattenFromString(cur)) == null) {
            return false;
        }
        return curComp.equals(service);
    }

    public void startSession(Bundle args) {
        if (this.mSystemService == null) {
            throw new IllegalStateException("Not available until onReady() is called");
        }
        try {
            this.mSystemService.startSession(this.mInterface, args);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mHandler = new MyHandler();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (SERVICE_INTERFACE.equals(intent.getAction())) {
            return this.mInterface.asBinder();
        }
        return null;
    }

    public void onReady() {
        this.mSystemService = IVoiceInteractionManagerService.Stub.asInterface(ServiceManager.getService(Context.VOICE_INTERACTION_MANAGER_SERVICE));
        this.mKeyphraseEnrollmentInfo = new KeyphraseEnrollmentInfo(getPackageManager());
    }

    private void onShutdownInternal() {
        onShutdown();
        safelyShutdownHotwordDetector();
    }

    public void onShutdown() {
    }

    private void onSoundModelsChangedInternal() {
        synchronized (this) {
            if (this.mHotwordDetector != null) {
                this.mHotwordDetector.onSoundModelsChanged();
            }
        }
    }

    public final AlwaysOnHotwordDetector createAlwaysOnHotwordDetector(String keyphrase, Locale locale, AlwaysOnHotwordDetector.Callback callback) {
        if (this.mSystemService == null) {
            throw new IllegalStateException("Not available until onReady() is called");
        }
        synchronized (this.mLock) {
            safelyShutdownHotwordDetector();
            this.mHotwordDetector = new AlwaysOnHotwordDetector(keyphrase, locale, callback, this.mKeyphraseEnrollmentInfo, this.mInterface, this.mSystemService);
        }
        return this.mHotwordDetector;
    }

    protected final KeyphraseEnrollmentInfo getKeyphraseEnrollmentInfo() {
        return this.mKeyphraseEnrollmentInfo;
    }

    private void safelyShutdownHotwordDetector() {
        try {
            synchronized (this.mLock) {
                if (this.mHotwordDetector != null) {
                    this.mHotwordDetector.stopRecognition();
                    this.mHotwordDetector.invalidate();
                    this.mHotwordDetector = null;
                }
            }
        } catch (Exception e) {
        }
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("VOICE INTERACTION");
        synchronized (this.mLock) {
            pw.println("  AlwaysOnHotwordDetector");
            if (this.mHotwordDetector == null) {
                pw.println("    NULL");
            } else {
                this.mHotwordDetector.dump("    ", pw);
            }
        }
    }
}
