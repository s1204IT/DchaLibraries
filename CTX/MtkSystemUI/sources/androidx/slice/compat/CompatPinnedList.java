package androidx.slice.compat;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.SystemClock;
import android.support.v4.util.ArraySet;
import android.support.v4.util.ObjectsCompat;
import android.text.TextUtils;
import androidx.slice.SliceSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CompatPinnedList {
    private final Context mContext;
    private final String mPrefsName;

    public CompatPinnedList(Context context, String prefsName) {
        this.mContext = context;
        this.mPrefsName = prefsName;
    }

    private SharedPreferences getPrefs() {
        SharedPreferences prefs = this.mContext.getSharedPreferences(this.mPrefsName, 0);
        long lastBootTime = prefs.getLong("last_boot", 0L);
        long currentBootTime = getBootTime();
        if (Math.abs(lastBootTime - currentBootTime) > 2000) {
            prefs.edit().clear().putLong("last_boot", currentBootTime).commit();
        }
        return prefs;
    }

    public List<Uri> getPinnedSlices() {
        List<Uri> pinned = new ArrayList<>();
        for (String key : getPrefs().getAll().keySet()) {
            if (key.startsWith("pinned_")) {
                Uri uri = Uri.parse(key.substring("pinned_".length()));
                if (!getPins(uri).isEmpty()) {
                    pinned.add(uri);
                }
            }
        }
        return pinned;
    }

    private Set<String> getPins(Uri uri) {
        return getPrefs().getStringSet("pinned_" + uri.toString(), new ArraySet());
    }

    public synchronized ArraySet<SliceSpec> getSpecs(Uri uri) {
        ArraySet<SliceSpec> specs = new ArraySet<>();
        SharedPreferences prefs = getPrefs();
        String specNamesStr = prefs.getString("spec_names_" + uri.toString(), null);
        String specRevsStr = prefs.getString("spec_revs_" + uri.toString(), null);
        if (!TextUtils.isEmpty(specNamesStr) && !TextUtils.isEmpty(specRevsStr)) {
            String[] specNames = specNamesStr.split(",", -1);
            String[] specRevs = specRevsStr.split(",", -1);
            if (specNames.length != specRevs.length) {
                return new ArraySet<>();
            }
            for (int i = 0; i < specNames.length; i++) {
                specs.add(new SliceSpec(specNames[i], Integer.parseInt(specRevs[i])));
            }
            return specs;
        }
        return new ArraySet<>();
    }

    private void setPins(Uri uri, Set<String> pins) {
        getPrefs().edit().putStringSet("pinned_" + uri.toString(), pins).commit();
    }

    private void setSpecs(Uri uri, ArraySet<SliceSpec> specs) {
        String[] specNames = new String[specs.size()];
        String[] specRevs = new String[specs.size()];
        for (int i = 0; i < specs.size(); i++) {
            specNames[i] = specs.valueAt(i).getType();
            specRevs[i] = String.valueOf(specs.valueAt(i).getRevision());
        }
        getPrefs().edit().putString("spec_names_" + uri.toString(), TextUtils.join(",", specNames)).putString("spec_revs_" + uri.toString(), TextUtils.join(",", specRevs)).commit();
    }

    protected long getBootTime() {
        return System.currentTimeMillis() - SystemClock.elapsedRealtime();
    }

    public synchronized boolean addPin(Uri uri, String pkg, Set<SliceSpec> specs) {
        boolean wasNotPinned;
        Set<String> pins = getPins(uri);
        wasNotPinned = pins.isEmpty();
        pins.add(pkg);
        setPins(uri, pins);
        if (wasNotPinned) {
            setSpecs(uri, new ArraySet<>(specs));
        } else {
            setSpecs(uri, mergeSpecs(getSpecs(uri), specs));
        }
        return wasNotPinned;
    }

    public synchronized boolean removePin(Uri uri, String pkg) {
        Set<String> pins = getPins(uri);
        if (!pins.isEmpty() && pins.contains(pkg)) {
            pins.remove(pkg);
            setPins(uri, pins);
            return pins.size() == 0;
        }
        return false;
    }

    private static ArraySet<SliceSpec> mergeSpecs(ArraySet<SliceSpec> specs, Set<SliceSpec> supportedSpecs) {
        int i;
        int i2 = 0;
        while (i2 < specs.size()) {
            SliceSpec s = specs.valueAt(i2);
            SliceSpec other = findSpec(supportedSpecs, s.getType());
            if (other == null) {
                i = i2 - 1;
                specs.removeAt(i2);
            } else {
                int i3 = other.getRevision();
                if (i3 >= s.getRevision()) {
                    i = i2;
                } else {
                    i = i2 - 1;
                    specs.removeAt(i2);
                    specs.add(other);
                }
            }
            i2 = i + 1;
        }
        return specs;
    }

    private static SliceSpec findSpec(Set<SliceSpec> specs, String type) {
        for (SliceSpec spec : specs) {
            if (ObjectsCompat.equals(spec.getType(), type)) {
                return spec;
            }
        }
        return null;
    }
}
