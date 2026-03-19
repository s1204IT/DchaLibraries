package com.android.deskclock.ringtone;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.media.RingtoneManager;
import android.net.Uri;
import com.android.deskclock.ItemAdapter;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.data.CustomRingtone;
import com.android.deskclock.data.DataModel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class RingtoneLoader extends AsyncTaskLoader<List<ItemAdapter.ItemHolder<Uri>>> {
    private List<CustomRingtone> mCustomRingtones;
    private final String mDefaultRingtoneTitle;
    private final Uri mDefaultRingtoneUri;

    RingtoneLoader(Context context, Uri uri, String str) {
        super(context);
        this.mDefaultRingtoneUri = uri;
        this.mDefaultRingtoneTitle = str;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        this.mCustomRingtones = DataModel.getDataModel().getCustomRingtones();
        forceLoad();
    }

    @Override
    public List<ItemAdapter.ItemHolder<Uri>> loadInBackground() {
        Cursor matrixCursor;
        DataModel.getDataModel().loadRingtoneTitles();
        DataModel.getDataModel().loadRingtonePermissions();
        RingtoneManager ringtoneManager = new RingtoneManager(getContext());
        ringtoneManager.setType(4);
        try {
            matrixCursor = ringtoneManager.getCursor();
        } catch (Exception e) {
            LogUtils.e("Could not get system ringtone cursor", new Object[0]);
            matrixCursor = new MatrixCursor(new String[0]);
        }
        int count = matrixCursor.getCount();
        ArrayList arrayList = new ArrayList(count + 3 + (this.mCustomRingtones == null ? 0 : this.mCustomRingtones.size()));
        arrayList.add(new HeaderHolder(R.string.your_sounds));
        if (this.mCustomRingtones != null) {
            Iterator<CustomRingtone> it = this.mCustomRingtones.iterator();
            while (it.hasNext()) {
                arrayList.add(new CustomRingtoneHolder(it.next()));
            }
        }
        arrayList.add(new AddCustomRingtoneHolder());
        arrayList.add(new HeaderHolder(R.string.device_sounds));
        arrayList.add(new SystemRingtoneHolder(Utils.RINGTONE_SILENT, null));
        arrayList.add(new SystemRingtoneHolder(this.mDefaultRingtoneUri, this.mDefaultRingtoneTitle));
        for (int i = 0; i < count; i++) {
            arrayList.add(new SystemRingtoneHolder(ringtoneManager.getRingtoneUri(i), null));
        }
        return arrayList;
    }

    @Override
    protected void onReset() {
        super.onReset();
        this.mCustomRingtones = null;
    }
}
