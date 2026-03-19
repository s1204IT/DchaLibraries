package com.android.server.broadcastradio.hal2;

import android.hardware.broadcastradio.V2_0.IBroadcastRadio;
import android.hardware.radio.IAnnouncementListener;
import android.hardware.radio.ICloseHandle;
import android.hardware.radio.ITuner;
import android.hardware.radio.ITunerCallback;
import android.hardware.radio.RadioManager;
import android.hidl.manager.V1_0.IServiceManager;
import android.os.RemoteException;
import android.util.Slog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BroadcastRadioService {
    private static final String TAG = "BcRadio2Srv";
    private final Map<Integer, RadioModule> mModules = new HashMap();

    private static List<String> listByInterface(String str) {
        try {
            IServiceManager service = IServiceManager.getService();
            if (service == null) {
                Slog.e(TAG, "Failed to get HIDL Service Manager");
                return Collections.emptyList();
            }
            ArrayList arrayListListByInterface = service.listByInterface(str);
            if (arrayListListByInterface == null) {
                Slog.e(TAG, "Didn't get interface list from HIDL Service Manager");
                return Collections.emptyList();
            }
            return arrayListListByInterface;
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed fetching interface list", e);
            return Collections.emptyList();
        }
    }

    public Collection<RadioManager.ModuleProperties> loadModules(int i) {
        Slog.v(TAG, "loadModules(" + i + ")");
        for (String str : listByInterface(IBroadcastRadio.kInterfaceName)) {
            Slog.v(TAG, "checking service: " + str);
            RadioModule radioModuleTryLoadingModule = RadioModule.tryLoadingModule(i, str);
            if (radioModuleTryLoadingModule != null) {
                Slog.i(TAG, "loaded broadcast radio module " + i + ": " + str + " (HAL 2.0)");
                this.mModules.put(Integer.valueOf(i), radioModuleTryLoadingModule);
                i++;
            }
        }
        return (Collection) this.mModules.values().stream().map(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((RadioModule) obj).mProperties;
            }
        }).collect(Collectors.toList());
    }

    public boolean hasModule(int i) {
        return this.mModules.containsKey(Integer.valueOf(i));
    }

    public boolean hasAnyModules() {
        return !this.mModules.isEmpty();
    }

    public ITuner openSession(int i, RadioManager.BandConfig bandConfig, boolean z, ITunerCallback iTunerCallback) throws RemoteException {
        Objects.requireNonNull(iTunerCallback);
        if (!z) {
            throw new IllegalArgumentException("Non-audio sessions not supported with HAL 2.x");
        }
        RadioModule radioModule = this.mModules.get(Integer.valueOf(i));
        if (radioModule == null) {
            throw new IllegalArgumentException("Invalid module ID");
        }
        TunerSession tunerSessionOpenSession = radioModule.openSession(iTunerCallback);
        if (bandConfig != null) {
            tunerSessionOpenSession.setConfiguration(bandConfig);
        }
        return tunerSessionOpenSession;
    }

    public ICloseHandle addAnnouncementListener(int[] iArr, IAnnouncementListener iAnnouncementListener) {
        AnnouncementAggregator announcementAggregator = new AnnouncementAggregator(iAnnouncementListener);
        Iterator<RadioModule> it = this.mModules.values().iterator();
        boolean z = false;
        while (it.hasNext()) {
            try {
                announcementAggregator.watchModule(it.next(), iArr);
                z = true;
            } catch (UnsupportedOperationException e) {
                Slog.v(TAG, "Announcements not supported for this module", e);
            }
        }
        if (!z) {
            Slog.i(TAG, "There are no HAL modules that support announcements");
        }
        return announcementAggregator;
    }
}
