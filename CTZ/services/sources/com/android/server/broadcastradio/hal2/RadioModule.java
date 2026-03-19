package com.android.server.broadcastradio.hal2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.broadcastradio.V2_0.AmFmRegionConfig;
import android.hardware.broadcastradio.V2_0.Announcement;
import android.hardware.broadcastradio.V2_0.IAnnouncementListener;
import android.hardware.broadcastradio.V2_0.IBroadcastRadio;
import android.hardware.broadcastradio.V2_0.ITunerSession;
import android.hardware.radio.IAnnouncementListener;
import android.hardware.radio.ICloseHandle;
import android.hardware.radio.ITunerCallback;
import android.hardware.radio.RadioManager;
import android.os.RemoteException;
import android.util.MutableInt;
import android.util.Slog;
import com.android.server.broadcastradio.hal2.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

class RadioModule {
    private static final String TAG = "BcRadio2Srv.module";
    public final RadioManager.ModuleProperties mProperties;
    private final IBroadcastRadio mService;

    private RadioModule(IBroadcastRadio iBroadcastRadio, RadioManager.ModuleProperties moduleProperties) {
        this.mProperties = (RadioManager.ModuleProperties) Objects.requireNonNull(moduleProperties);
        this.mService = (IBroadcastRadio) Objects.requireNonNull(iBroadcastRadio);
    }

    public static RadioModule tryLoadingModule(int i, String str) {
        try {
            IBroadcastRadio service = IBroadcastRadio.getService(str);
            if (service == null) {
                return null;
            }
            final Mutable mutable = new Mutable();
            service.getAmFmRegionConfig(false, new IBroadcastRadio.getAmFmRegionConfigCallback() {
                @Override
                public final void onValues(int i2, AmFmRegionConfig amFmRegionConfig) {
                    RadioModule.lambda$tryLoadingModule$0(mutable, i2, amFmRegionConfig);
                }
            });
            final Mutable mutable2 = new Mutable();
            service.getDabRegionConfig(new IBroadcastRadio.getDabRegionConfigCallback() {
                @Override
                public final void onValues(int i2, ArrayList arrayList) {
                    RadioModule.lambda$tryLoadingModule$1(mutable2, i2, arrayList);
                }
            });
            return new RadioModule(service, Convert.propertiesFromHal(i, str, service.getProperties(), (AmFmRegionConfig) mutable.value, (List) mutable2.value));
        } catch (RemoteException e) {
            Slog.e(TAG, "failed to load module " + str, e);
            return null;
        }
    }

    static void lambda$tryLoadingModule$0(Mutable mutable, int i, AmFmRegionConfig amFmRegionConfig) {
        if (i == 0) {
            mutable.value = amFmRegionConfig;
        }
    }

    static void lambda$tryLoadingModule$1(Mutable mutable, int i, ArrayList arrayList) {
        if (i == 0) {
            mutable.value = arrayList;
        }
    }

    public TunerSession openSession(ITunerCallback iTunerCallback) throws RemoteException {
        TunerCallback tunerCallback = new TunerCallback((ITunerCallback) Objects.requireNonNull(iTunerCallback));
        final Mutable mutable = new Mutable();
        final MutableInt mutableInt = new MutableInt(1);
        synchronized (this.mService) {
            this.mService.openSession(tunerCallback, new IBroadcastRadio.openSessionCallback() {
                @Override
                public final void onValues(int i, ITunerSession iTunerSession) {
                    RadioModule.lambda$openSession$2(mutable, mutableInt, i, iTunerSession);
                }
            });
        }
        Convert.throwOnError("openSession", mutableInt.value);
        Objects.requireNonNull((ITunerSession) mutable.value);
        return new TunerSession(this, (ITunerSession) mutable.value, tunerCallback);
    }

    static void lambda$openSession$2(Mutable mutable, MutableInt mutableInt, int i, ITunerSession iTunerSession) {
        mutable.value = iTunerSession;
        mutableInt.value = i;
    }

    public ICloseHandle addAnnouncementListener(int[] iArr, IAnnouncementListener iAnnouncementListener) throws RemoteException {
        ArrayList<Byte> arrayList = new ArrayList<>();
        for (int i : iArr) {
            arrayList.add(Byte.valueOf((byte) i));
        }
        final MutableInt mutableInt = new MutableInt(1);
        final Mutable mutable = new Mutable();
        AnonymousClass1 anonymousClass1 = new AnonymousClass1(iAnnouncementListener);
        synchronized (this.mService) {
            this.mService.registerAnnouncementListener(arrayList, anonymousClass1, new IBroadcastRadio.registerAnnouncementListenerCallback() {
                @Override
                public final void onValues(int i2, android.hardware.broadcastradio.V2_0.ICloseHandle iCloseHandle) {
                    RadioModule.lambda$addAnnouncementListener$3(mutableInt, mutable, i2, iCloseHandle);
                }
            });
        }
        Convert.throwOnError("addAnnouncementListener", mutableInt.value);
        return new ICloseHandle.Stub() {
            public void close() {
                try {
                    ((android.hardware.broadcastradio.V2_0.ICloseHandle) mutable.value).close();
                } catch (RemoteException e) {
                    Slog.e(RadioModule.TAG, "Failed closing announcement listener", e);
                }
            }
        };
    }

    class AnonymousClass1 extends IAnnouncementListener.Stub {
        final android.hardware.radio.IAnnouncementListener val$listener;

        AnonymousClass1(android.hardware.radio.IAnnouncementListener iAnnouncementListener) {
            this.val$listener = iAnnouncementListener;
        }

        @Override
        public void onListUpdated(ArrayList<Announcement> arrayList) throws RemoteException {
            this.val$listener.onListUpdated((List) arrayList.stream().map(new Function() {
                @Override
                public final Object apply(Object obj) {
                    return Convert.announcementFromHal((Announcement) obj);
                }
            }).collect(Collectors.toList()));
        }
    }

    static void lambda$addAnnouncementListener$3(MutableInt mutableInt, Mutable mutable, int i, android.hardware.broadcastradio.V2_0.ICloseHandle iCloseHandle) {
        mutableInt.value = i;
        mutable.value = iCloseHandle;
    }

    Bitmap getImage(final int i) {
        byte[] bArr;
        if (i == 0) {
            throw new IllegalArgumentException("Image ID is missing");
        }
        synchronized (this.mService) {
            List list = (List) Utils.maybeRethrow(new Utils.FuncThrowingRemoteException() {
                @Override
                public final Object exec() {
                    return this.f$0.mService.getImage(i);
                }
            });
            bArr = new byte[list.size()];
            for (int i2 = 0; i2 < list.size(); i2++) {
                bArr[i2] = ((Byte) list.get(i2)).byteValue();
            }
        }
        if (bArr.length == 0) {
            return null;
        }
        return BitmapFactory.decodeByteArray(bArr, 0, bArr.length);
    }
}
