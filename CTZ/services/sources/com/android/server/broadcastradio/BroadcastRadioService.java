package com.android.server.broadcastradio;

import android.content.Context;
import android.hardware.radio.IAnnouncementListener;
import android.hardware.radio.ICloseHandle;
import android.hardware.radio.IRadioService;
import android.hardware.radio.ITuner;
import android.hardware.radio.ITunerCallback;
import android.hardware.radio.RadioManager;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.SystemService;
import com.android.server.broadcastradio.hal2.AnnouncementAggregator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.ToIntFunction;

public class BroadcastRadioService extends SystemService {
    private static final boolean DEBUG = false;
    private static final String TAG = "BcRadioSrv";
    private final com.android.server.broadcastradio.hal1.BroadcastRadioService mHal1;
    private final com.android.server.broadcastradio.hal2.BroadcastRadioService mHal2;
    private final Object mLock;
    private List<RadioManager.ModuleProperties> mModules;
    private final ServiceImpl mServiceImpl;

    public BroadcastRadioService(Context context) {
        super(context);
        this.mServiceImpl = new ServiceImpl();
        this.mHal1 = new com.android.server.broadcastradio.hal1.BroadcastRadioService();
        this.mHal2 = new com.android.server.broadcastradio.hal2.BroadcastRadioService();
        this.mLock = new Object();
        this.mModules = null;
    }

    @Override
    public void onStart() {
        publishBinderService("broadcastradio", this.mServiceImpl);
    }

    private static int getNextId(List<RadioManager.ModuleProperties> list) {
        OptionalInt optionalIntMax = list.stream().mapToInt(new ToIntFunction() {
            @Override
            public final int applyAsInt(Object obj) {
                return ((RadioManager.ModuleProperties) obj).getId();
            }
        }).max();
        if (optionalIntMax.isPresent()) {
            return optionalIntMax.getAsInt() + 1;
        }
        return 0;
    }

    private class ServiceImpl extends IRadioService.Stub {
        private ServiceImpl() {
        }

        private void enforcePolicyAccess() {
            if (BroadcastRadioService.this.getContext().checkCallingPermission("android.permission.ACCESS_BROADCAST_RADIO") != 0) {
                throw new SecurityException("ACCESS_BROADCAST_RADIO permission not granted");
            }
        }

        public List<RadioManager.ModuleProperties> listModules() {
            enforcePolicyAccess();
            synchronized (BroadcastRadioService.this.mLock) {
                if (BroadcastRadioService.this.mModules != null) {
                    return BroadcastRadioService.this.mModules;
                }
                BroadcastRadioService.this.mModules = BroadcastRadioService.this.mHal1.loadModules();
                BroadcastRadioService.this.mModules.addAll(BroadcastRadioService.this.mHal2.loadModules(BroadcastRadioService.getNextId(BroadcastRadioService.this.mModules)));
                return BroadcastRadioService.this.mModules;
            }
        }

        public ITuner openTuner(int i, RadioManager.BandConfig bandConfig, boolean z, ITunerCallback iTunerCallback) throws RemoteException {
            enforcePolicyAccess();
            if (iTunerCallback != null) {
                synchronized (BroadcastRadioService.this.mLock) {
                    if (BroadcastRadioService.this.mHal2.hasModule(i)) {
                        return BroadcastRadioService.this.mHal2.openSession(i, bandConfig, z, iTunerCallback);
                    }
                    return BroadcastRadioService.this.mHal1.openTuner(i, bandConfig, z, iTunerCallback);
                }
            }
            throw new IllegalArgumentException("Callback must not be empty");
        }

        public ICloseHandle addAnnouncementListener(int[] iArr, IAnnouncementListener iAnnouncementListener) {
            Objects.requireNonNull(iArr);
            Objects.requireNonNull(iAnnouncementListener);
            enforcePolicyAccess();
            synchronized (BroadcastRadioService.this.mLock) {
                if (BroadcastRadioService.this.mHal2.hasAnyModules()) {
                    return BroadcastRadioService.this.mHal2.addAnnouncementListener(iArr, iAnnouncementListener);
                }
                Slog.i(BroadcastRadioService.TAG, "There are no HAL 2.x modules registered");
                return new AnnouncementAggregator(iAnnouncementListener);
            }
        }
    }
}
