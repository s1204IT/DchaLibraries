package android.service.voice;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.voice.IVoiceInteractionSessionService;
import com.android.internal.app.IVoiceInteractionManagerService;
import com.android.internal.os.HandlerCaller;
import com.android.internal.os.SomeArgs;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public abstract class VoiceInteractionSessionService extends Service {
    static final int MSG_NEW_SESSION = 1;
    HandlerCaller mHandlerCaller;
    VoiceInteractionSession mSession;
    IVoiceInteractionManagerService mSystemService;
    IVoiceInteractionSessionService mInterface = new IVoiceInteractionSessionService.Stub() {
        @Override
        public void newSession(IBinder iBinder, Bundle bundle, int i) {
            VoiceInteractionSessionService.this.mHandlerCaller.sendMessage(VoiceInteractionSessionService.this.mHandlerCaller.obtainMessageIOO(1, i, iBinder, bundle));
        }
    };
    final HandlerCaller.Callback mHandlerCallerCallback = new HandlerCaller.Callback() {
        @Override
        public void executeMessage(Message message) {
            SomeArgs someArgs = (SomeArgs) message.obj;
            if (message.what == 1) {
                VoiceInteractionSessionService.this.doNewSession((IBinder) someArgs.arg1, (Bundle) someArgs.arg2, someArgs.argi1);
            }
        }
    };

    public abstract VoiceInteractionSession onNewSession(Bundle bundle);

    @Override
    public void onCreate() {
        super.onCreate();
        this.mSystemService = IVoiceInteractionManagerService.Stub.asInterface(ServiceManager.getService(Context.VOICE_INTERACTION_MANAGER_SERVICE));
        this.mHandlerCaller = new HandlerCaller(this, Looper.myLooper(), this.mHandlerCallerCallback, true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mInterface.asBinder();
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (this.mSession != null) {
            this.mSession.onConfigurationChanged(configuration);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (this.mSession != null) {
            this.mSession.onLowMemory();
        }
    }

    @Override
    public void onTrimMemory(int i) {
        super.onTrimMemory(i);
        if (this.mSession != null) {
            this.mSession.onTrimMemory(i);
        }
    }

    @Override
    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (this.mSession == null) {
            printWriter.println("(no active session)");
        } else {
            printWriter.println("VoiceInteractionSession:");
            this.mSession.dump("  ", fileDescriptor, printWriter, strArr);
        }
    }

    void doNewSession(IBinder iBinder, Bundle bundle, int i) {
        if (this.mSession != null) {
            this.mSession.doDestroy();
            this.mSession = null;
        }
        this.mSession = onNewSession(bundle);
        try {
            this.mSystemService.deliverNewSession(iBinder, this.mSession.mSession, this.mSession.mInteractor);
            this.mSession.doCreate(this.mSystemService, iBinder);
        } catch (RemoteException e) {
        }
    }
}
