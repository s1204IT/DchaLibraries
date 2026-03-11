package com.android.settings.datausage;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v7.preference.Preference;
import android.util.ArraySet;
import com.android.settingslib.utils.AsyncLoader;

public class AppPrefLoader extends AsyncLoader<ArraySet<Preference>> {
    private PackageManager mPackageManager;
    private ArraySet<String> mPackages;
    private Context mPrefContext;

    public AppPrefLoader(Context context, ArraySet<String> arraySet, PackageManager packageManager) {
        super(context);
        this.mPackages = arraySet;
        this.mPackageManager = packageManager;
        this.mPrefContext = context;
    }

    @Override
    public ArraySet<Preference> loadInBackground() {
        ArraySet<Preference> arraySet = new ArraySet<>();
        int size = this.mPackages.size();
        for (int i = 1; i < size; i++) {
            try {
                ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(this.mPackages.valueAt(i), 0);
                Preference preference = new Preference(this.mPrefContext);
                preference.setIcon(applicationInfo.loadIcon(this.mPackageManager));
                preference.setTitle(applicationInfo.loadLabel(this.mPackageManager));
                preference.setSelectable(false);
                arraySet.add(preference);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return arraySet;
    }

    @Override
    public void onDiscardResult(ArraySet<Preference> arraySet) {
    }
}
