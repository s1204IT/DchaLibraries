package com.android.storagemanager.deletionhelper;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.text.format.DateUtils;
import com.android.storagemanager.utils.IconProvider;
import java.io.File;

public class DownloadsFilePreference extends NestedDeletionPreference {
    private File mFile;

    public DownloadsFilePreference(Context context, File file, IconProvider iconProvider) {
        super(context);
        this.mFile = file;
        setKey(this.mFile.getPath());
        setTitle(file.getName());
        setItemSize(file.length());
        setSummary(DateUtils.formatDateTime(context, this.mFile.lastModified(), 16));
        setIcon(iconProvider.loadMimeIcon(IconProvider.getMimeType(this.mFile)));
        setPersistent(false);
    }

    public File getFile() {
        return this.mFile;
    }

    @Override
    public int compareTo(Preference preference) {
        if (preference == null || !(preference instanceof DownloadsFilePreference)) {
            return 1;
        }
        File file = ((DownloadsFilePreference) preference).getFile();
        File file2 = getFile();
        int iCompare = Long.compare(file.length(), file2.length());
        if (iCompare == 0) {
            return file2.compareTo(file);
        }
        return iCompare;
    }
}
