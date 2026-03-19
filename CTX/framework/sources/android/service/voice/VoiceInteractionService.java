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
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IVoiceInteractionManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Locale;

public class VoiceInteractionService extends Service {
    static final int MSG_LAUNCH_VOICE_ASSIST_FROM_KEYGUARD = 4;
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

        @Override
        public void launchVoiceAssistFromKeyguard() throws RemoteException {
            VoiceInteractionService.this.mHandler.sendEmptyMessage(4);
        }
    };
    private final Object mLock = new Object();

    class MyHandler extends Handler {
        MyHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    VoiceInteractionService.this.onReady();
                    break;
                case 2:
                    VoiceInteractionService.this.onShutdownInternal();
                    break;
                case 3:
                    VoiceInteractionService.this.onSoundModelsChangedInternal();
                    break;
                case 4:
                    VoiceInteractionService.this.onLaunchVoiceAssistFromKeyguard();
                    break;
                default:
                    super.handleMessage(message);
                    break;
            }
        }
    }

    public void onLaunchVoiceAssistFromKeyguard() {
    }

    public static boolean isActiveService(Context context, ComponentName componentName) {
        ComponentName componentNameUnflattenFromString;
        String string = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.VOICE_INTERACTION_SERVICE);
        if (string == null || string.isEmpty() || (componentNameUnflattenFromString = ComponentName.unflattenFromString(string)) == null) {
            return false;
        }
        return componentNameUnflattenFromString.equals(componentName);
    }

    public void setDisabledShowContext(int i) {
        try {
            this.mSystemService.setDisabledShowContext(i);
        } catch (RemoteException e) {
        }
    }

    public int getDisabledShowContext() {
        try {
            return this.mSystemService.getDisabledShowContext();
        } catch (RemoteException e) {
            return 0;
        }
    }

    public void showSession(Bundle bundle, int i) {
        if (this.mSystemService == null) {
            throw new IllegalStateException("Not available until onReady() is called");
        }
        try {
            this.mSystemService.showSession(this.mInterface, bundle, i);
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

    public final AlwaysOnHotwordDetector createAlwaysOnHotwordDetector(String str, Locale locale, AlwaysOnHotwordDetector.Callback callback) {
        if (this.mSystemService == null) {
            throw new IllegalStateException("Not available until onReady() is called");
        }
        synchronized (this.mLock) {
            safelyShutdownHotwordDetector();
            this.mHotwordDetector = new AlwaysOnHotwordDetector(str, locale, callback, this.mKeyphraseEnrollmentInfo, this.mInterface, this.mSystemService);
        }
        return this.mHotwordDetector;
    }

    @VisibleForTesting
    protected final KeyphraseEnrollmentInfo getKeyphraseEnrollmentInfo() {
        return this.mKeyphraseEnrollmentInfo;
    }

    public final boolean isKeyphraseAndLocaleSupportedForHotword(String str, Locale locale) {
        return (this.mKeyphraseEnrollmentInfo == null || this.mKeyphraseEnrollmentInfo.getKeyphraseMetadata(str, locale) == null) ? false : true;
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
    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("VOICE INTERACTION");
        synchronized (this.mLock) {
            printWriter.println("  AlwaysOnHotwordDetector");
            if (this.mHotwordDetector == null) {
                printWriter.println("    NULL");
            } else {
                this.mHotwordDetector.dump("    ", printWriter);
            }
        }
    }
}
