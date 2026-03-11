package com.android.settings.development;

import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class ShowSurfaceUpdatesPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final int SURFACE_FLINGER_READ_CODE = 1010;
    static final String SURFACE_FLINGER_SERVICE_KEY = "SurfaceFlinger";
    private final IBinder mSurfaceFlinger;

    public ShowSurfaceUpdatesPreferenceController(Context context) {
        super(context);
        this.mSurfaceFlinger = ServiceManager.getService(SURFACE_FLINGER_SERVICE_KEY);
    }

    @Override
    public String getPreferenceKey() {
        return "show_screen_updates";
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        writeShowUpdatesSetting(((Boolean) obj).booleanValue());
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updateShowUpdatesSetting();
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SwitchPreference switchPreference = (SwitchPreference) this.mPreference;
        if (switchPreference.isChecked()) {
            writeShowUpdatesSetting(false);
            switchPreference.setChecked(false);
        }
    }

    void updateShowUpdatesSetting() {
        try {
            if (this.mSurfaceFlinger != null) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                parcelObtain.writeInterfaceToken("android.ui.ISurfaceComposer");
                this.mSurfaceFlinger.transact(SURFACE_FLINGER_READ_CODE, parcelObtain, parcelObtain2, 0);
                parcelObtain2.readInt();
                parcelObtain2.readInt();
                ((SwitchPreference) this.mPreference).setChecked(parcelObtain2.readInt() != 0);
                parcelObtain2.recycle();
                parcelObtain.recycle();
            }
        } catch (RemoteException e) {
        }
    }

    void writeShowUpdatesSetting(boolean z) {
        try {
            if (this.mSurfaceFlinger != null) {
                Parcel parcelObtain = Parcel.obtain();
                parcelObtain.writeInterfaceToken("android.ui.ISurfaceComposer");
                parcelObtain.writeInt(z ? 1 : 0);
                this.mSurfaceFlinger.transact(1002, parcelObtain, null, 0);
                parcelObtain.recycle();
            }
        } catch (RemoteException e) {
        }
        updateShowUpdatesSetting();
    }
}
