package com.android.settings.development;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.storage.IStorageManager;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/* loaded from: classes.dex */
public class FileEncryptionPreferenceController extends DeveloperOptionsPreferenceController implements PreferenceControllerMixin {
    static final String FILE_ENCRYPTION_PROPERTY_KEY = "ro.crypto.type";
    private final IStorageManager mStorageManager;

    public FileEncryptionPreferenceController(Context context) {
        super(context);
        this.mStorageManager = getStorageManager();
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        if (this.mStorageManager == null) {
            return false;
        }
        try {
            return this.mStorageManager.isConvertibleToFBE();
        } catch (RemoteException e) {
            return false;
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "convert_to_file_encryption";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        if (!TextUtils.equals("file", SystemProperties.get(FILE_ENCRYPTION_PROPERTY_KEY, "none"))) {
            return;
        }
        this.mPreference.setEnabled(false);
        this.mPreference.setSummary(this.mContext.getResources().getString(R.string.convert_to_file_encryption_done));
    }

    private IStorageManager getStorageManager() {
        try {
            return IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
        } catch (VerifyError e) {
            return null;
        }
    }
}
