package com.android.settings.development;

import android.content.Context;
import android.content.Intent;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class DigitizerVersionPreferenceController extends DeveloperOptionsPreferenceController implements PreferenceControllerMixin, OnActivityResultListener {
    private final PackageManagerWrapper mPackageManager;

    public DigitizerVersionPreferenceController(Context context) {
        super(context);
        this.mPackageManager = new PackageManagerWrapper(this.mContext.getPackageManager());
    }

    @Override
    public String getPreferenceKey() {
        return "digitizer_ver";
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        updatePreferenceSummary();
    }

    @Override
    public boolean onActivityResult(int i, int i2, Intent intent) {
        updatePreferenceSummary();
        return true;
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
    }

    Intent getActivityStartIntent() {
        return new Intent(this.mContext, (Class<?>) AppPicker.class);
    }

    private void updatePreferenceSummary() {
        String line;
        String str = "";
        Log.e("PrefControllerMixin", "updatePreferenceSummary");
        File file = new File("/sys/devices/platform/soc/11007000.i2c/i2c-0/0-0009/digi_fwver");
        File file2 = new File("/sys/devices/platform/soc/11009000.i2c/i2c-2/2-0009/digi_fwver");
        if (file.exists()) {
            try {
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                line = bufferedReader.readLine();
                try {
                    bufferedReader.close();
                    fileReader.close();
                    str = line;
                } catch (IOException e) {
                    str = line;
                }
            } catch (IOException e2) {
            }
        } else if (file2.exists()) {
            try {
                FileReader fileReader2 = new FileReader(file2);
                BufferedReader bufferedReader2 = new BufferedReader(fileReader2);
                line = bufferedReader2.readLine();
                try {
                    bufferedReader2.close();
                    fileReader2.close();
                    str = line;
                } catch (IOException e3) {
                    str = line;
                }
            } catch (IOException e4) {
            }
        }
        Log.e("PrefControllerMixin", "tmp_str : " + str);
        this.mPreference.setSummary(str);
    }
}
