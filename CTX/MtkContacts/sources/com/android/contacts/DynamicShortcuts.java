package com.android.contacts;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PersistableBundle;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.os.BuildCompat;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.lettertiles.LetterTileDrawable;
import com.android.contacts.model.account.BaseAccountType;
import com.android.contacts.util.BitmapUtil;
import com.android.contacts.util.ImplicitIntentsUtil;
import com.android.contacts.util.PermissionsUtil;
import com.android.contactsbind.experiments.Flags;
import com.mediatek.contacts.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@TargetApi(BaseAccountType.Weight.STRUCTURED_POSTAL)
public class DynamicShortcuts {
    private static final String EXTRA_SHORTCUT_TYPE = "extraShortcutType";
    private static final int LONG_LABEL_MAX_LENGTH = 30;
    private static final int MAX_SHORTCUTS = 3;
    static final String[] PROJECTION = {"_id", "lookup", "display_name"};
    public static final String SHORTCUT_ADD_CONTACT = "shortcut-add-contact";
    private static final int SHORTCUT_TYPE_ACTION_URI = 2;
    private static final int SHORTCUT_TYPE_CONTACT_URI = 1;
    private static final int SHORTCUT_TYPE_UNKNOWN = 0;
    private static final int SHORT_LABEL_MAX_LENGTH = 12;
    private static final String TAG = "DynamicShortcuts";
    private final int mContentChangeMaxUpdateDelay;
    private final int mContentChangeMinUpdateDelay;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private int mIconSize;
    private final JobScheduler mJobScheduler;
    private int mLongLabelMaxLength;
    private int mShortLabelMaxLength;
    private final ShortcutManager mShortcutManager;

    public DynamicShortcuts(Context context) {
        this(context, context.getContentResolver(), (ShortcutManager) context.getSystemService("shortcut"), (JobScheduler) context.getSystemService("jobscheduler"));
    }

    public DynamicShortcuts(Context context, ContentResolver contentResolver, ShortcutManager shortcutManager, JobScheduler jobScheduler) {
        this.mShortLabelMaxLength = SHORT_LABEL_MAX_LENGTH;
        this.mLongLabelMaxLength = LONG_LABEL_MAX_LENGTH;
        this.mContext = context;
        this.mContentResolver = contentResolver;
        this.mShortcutManager = shortcutManager;
        this.mJobScheduler = jobScheduler;
        this.mContentChangeMinUpdateDelay = Flags.getInstance().getInteger("Shortcuts__dynamic_min_content_change_update_delay_millis");
        this.mContentChangeMaxUpdateDelay = Flags.getInstance().getInteger("Shortcuts__dynamic_max_content_change_update_delay_millis");
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        this.mIconSize = context.getResources().getDimensionPixelSize(R.dimen.shortcut_icon_size);
        if (this.mIconSize == 0) {
            this.mIconSize = activityManager.getLauncherLargeIconSize();
        }
    }

    void setShortLabelMaxLength(int i) {
        this.mShortLabelMaxLength = i;
    }

    void setLongLabelMaxLength(int i) {
        this.mLongLabelMaxLength = i;
    }

    void refresh() {
        if (hasRequiredPermissions()) {
            List<ShortcutInfo> strequentShortcuts = getStrequentShortcuts();
            this.mShortcutManager.setDynamicShortcuts(strequentShortcuts);
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "set dynamic shortcuts " + strequentShortcuts);
            }
            updatePinned();
        }
    }

    void updatePinned() {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        for (ShortcutInfo shortcutInfo : this.mShortcutManager.getPinnedShortcuts()) {
            PersistableBundle extras = shortcutInfo.getExtras();
            if (extras != null && extras.getInt(EXTRA_SHORTCUT_TYPE, 0) == 1) {
                ShortcutInfo shortcutInfoCreateShortcutForUri = createShortcutForUri(ContactsContract.Contacts.getLookupUri(extras.getLong("_id"), shortcutInfo.getId()));
                if (shortcutInfoCreateShortcutForUri != null) {
                    arrayList.add(shortcutInfoCreateShortcutForUri);
                    if (!shortcutInfo.isEnabled()) {
                        arrayList3.add(shortcutInfoCreateShortcutForUri.getId());
                    }
                } else if (shortcutInfo.isEnabled()) {
                    arrayList2.add(shortcutInfo.getId());
                }
            }
        }
        if (Log.isLoggable(TAG, 3)) {
            Log.d(TAG, "updating " + arrayList);
            Log.d(TAG, "enabling " + arrayList3);
            Log.d(TAG, "disabling " + arrayList2);
        }
        this.mShortcutManager.updateShortcuts(arrayList);
        this.mShortcutManager.enableShortcuts(arrayList3);
        this.mShortcutManager.disableShortcuts((List) arrayList2, this.mContext.getString(R.string.dynamic_shortcut_contact_removed_message));
    }

    private ShortcutInfo createShortcutForUri(Uri uri) {
        Cursor cursorQuery = this.mContentResolver.query(uri, PROJECTION, null, null, null);
        if (cursorQuery == null) {
            return null;
        }
        try {
            if (!cursorQuery.moveToFirst()) {
                return null;
            }
            return createShortcutFromRow(cursorQuery);
        } finally {
            cursorQuery.close();
        }
    }

    public List<ShortcutInfo> getStrequentShortcuts() {
        Cursor cursorQuery = this.mContentResolver.query(ContactsContract.Contacts.CONTENT_STREQUENT_URI.buildUpon().appendQueryParameter("limit", String.valueOf(3)).build(), PROJECTION, null, null, null);
        if (cursorQuery == null) {
            return Collections.emptyList();
        }
        ArrayList arrayList = new ArrayList();
        int i = 0;
        while (i < 3) {
            try {
                if (!cursorQuery.moveToNext()) {
                    break;
                }
                ShortcutInfo shortcutInfoCreateShortcutFromRow = createShortcutFromRow(cursorQuery);
                if (shortcutInfoCreateShortcutFromRow != null) {
                    arrayList.add(shortcutInfoCreateShortcutFromRow);
                    i++;
                }
            } finally {
                cursorQuery.close();
            }
        }
        return arrayList;
    }

    ShortcutInfo createShortcutFromRow(Cursor cursor) {
        ShortcutInfo.Builder builderBuilderForContactShortcut = builderForContactShortcut(cursor);
        if (builderBuilderForContactShortcut == null) {
            return null;
        }
        addIconForContact(cursor, builderBuilderForContactShortcut);
        return builderBuilderForContactShortcut.build();
    }

    ShortcutInfo.Builder builderForContactShortcut(Cursor cursor) {
        return builderForContactShortcut(cursor.getLong(0), cursor.getString(1), cursor.getString(2));
    }

    ShortcutInfo.Builder builderForContactShortcut(long j, String str, String str2) {
        if (str == null) {
            Log.e(TAG, "[builderForContactShortcut] lookupkey is null !!!");
            return null;
        }
        PersistableBundle persistableBundle = new PersistableBundle();
        persistableBundle.putLong("_id", j);
        persistableBundle.putInt(EXTRA_SHORTCUT_TYPE, 1);
        ShortcutInfo.Builder extras = new ShortcutInfo.Builder(this.mContext, str).setIntent(ImplicitIntentsUtil.getIntentForQuickContactLauncherShortcut(this.mContext, ContactsContract.Contacts.getLookupUri(j, str))).setDisabledMessage(this.mContext.getString(R.string.dynamic_shortcut_disabled_message)).setExtras(persistableBundle);
        if (str2 == null) {
            str2 = this.mContext.getResources().getString(R.string.missing_name);
        }
        setLabel(extras, str2);
        return extras;
    }

    ShortcutInfo getActionShortcutInfo(String str, String str2, Intent intent, Icon icon) {
        if (str == null || str2 == null) {
            return null;
        }
        PersistableBundle persistableBundle = new PersistableBundle();
        persistableBundle.putInt(EXTRA_SHORTCUT_TYPE, 2);
        ShortcutInfo.Builder disabledMessage = new ShortcutInfo.Builder(this.mContext, str).setIntent(intent).setIcon(icon).setExtras(persistableBundle).setDisabledMessage(this.mContext.getString(R.string.dynamic_shortcut_disabled_message));
        setLabel(disabledMessage, str2);
        return disabledMessage.build();
    }

    public ShortcutInfo getQuickContactShortcutInfo(long j, String str, String str2) {
        ShortcutInfo.Builder builderBuilderForContactShortcut = builderForContactShortcut(j, str, str2);
        if (builderBuilderForContactShortcut == null) {
            return null;
        }
        addIconForContact(j, str, str2, builderBuilderForContactShortcut);
        return builderBuilderForContactShortcut.build();
    }

    private void setLabel(ShortcutInfo.Builder builder, String str) {
        if (str.length() < this.mLongLabelMaxLength) {
            builder.setLongLabel(str);
        } else {
            builder.setLongLabel(str.substring(0, this.mLongLabelMaxLength - 1).trim() + "…");
        }
        if (str.length() < this.mShortLabelMaxLength) {
            builder.setShortLabel(str);
            return;
        }
        builder.setShortLabel(str.substring(0, this.mShortLabelMaxLength - 1).trim() + "…");
    }

    private void addIconForContact(Cursor cursor, ShortcutInfo.Builder builder) {
        addIconForContact(cursor.getLong(0), cursor.getString(1), cursor.getString(2), builder);
    }

    private void addIconForContact(long j, String str, String str2, ShortcutInfo.Builder builder) {
        Icon iconCreateWithBitmap;
        Bitmap contactPhoto = getContactPhoto(j);
        if (contactPhoto == null) {
            contactPhoto = getFallbackAvatar(str2, str);
        }
        if (BuildCompat.isAtLeastO()) {
            iconCreateWithBitmap = Icon.createWithAdaptiveBitmap(contactPhoto);
        } else {
            iconCreateWithBitmap = Icon.createWithBitmap(contactPhoto);
        }
        builder.setIcon(iconCreateWithBitmap);
    }

    private Bitmap getContactPhoto(long j) {
        InputStream inputStreamOpenContactPhotoInputStream = ContactsContract.Contacts.openContactPhotoInputStream(this.mContext.getContentResolver(), ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, j), true);
        try {
            if (inputStreamOpenContactPhotoInputStream == null) {
                return null;
            }
            try {
                Bitmap bitmapDecodeStreamForShortcut = decodeStreamForShortcut(inputStreamOpenContactPhotoInputStream);
                inputStreamOpenContactPhotoInputStream.close();
                return bitmapDecodeStreamForShortcut;
            } catch (IOException e) {
                Log.e(TAG, "Failed to decode contact photo for shortcut. ID=" + j, e);
                try {
                    inputStreamOpenContactPhotoInputStream.close();
                } catch (IOException e2) {
                }
                return null;
            }
        } finally {
            try {
                inputStreamOpenContactPhotoInputStream.close();
            } catch (IOException e3) {
            }
        }
    }

    private Bitmap decodeStreamForShortcut(InputStream inputStream) throws IOException {
        BitmapRegionDecoder bitmapRegionDecoderNewInstance = BitmapRegionDecoder.newInstance(inputStream, false);
        int width = bitmapRegionDecoderNewInstance.getWidth();
        int height = bitmapRegionDecoderNewInstance.getHeight();
        int iconMaxWidth = this.mShortcutManager.getIconMaxWidth();
        int iconMaxHeight = this.mShortcutManager.getIconMaxHeight();
        int iMin = Math.min(BitmapUtil.findOptimalSampleSize(width, this.mIconSize), BitmapUtil.findOptimalSampleSize(height, this.mIconSize));
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = iMin;
        int i = width / options.inSampleSize;
        int i2 = height / options.inSampleSize;
        int iMin2 = Math.min(Math.min(i, iconMaxWidth), Math.min(i2, iconMaxHeight));
        int i3 = ((i - iMin2) * options.inSampleSize) / 2;
        int i4 = ((i2 - iMin2) * options.inSampleSize) / 2;
        Bitmap bitmapDecodeRegion = bitmapRegionDecoderNewInstance.decodeRegion(new Rect(i3, i4, width - i3, height - i4), options);
        bitmapRegionDecoderNewInstance.recycle();
        if (!BuildCompat.isAtLeastO()) {
            return BitmapUtil.getRoundedBitmap(bitmapDecodeRegion, iMin2, iMin2);
        }
        return bitmapDecodeRegion;
    }

    private Bitmap getFallbackAvatar(String str, String str2) {
        ContactPhotoManager.DefaultImageRequest defaultImageRequest = new ContactPhotoManager.DefaultImageRequest(str, str2, !BuildCompat.isAtLeastO());
        if (BuildCompat.isAtLeastO()) {
            defaultImageRequest.scale = LetterTileDrawable.getAdaptiveIconScale();
        }
        Drawable defaultAvatarDrawableForContact = ContactPhotoManager.getDefaultAvatarDrawableForContact(this.mContext.getResources(), true, defaultImageRequest);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(this.mIconSize, this.mIconSize, Bitmap.Config.ARGB_8888);
        defaultAvatarDrawableForContact.setVisible(true, true);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        defaultAvatarDrawableForContact.setBounds(0, 0, this.mIconSize, this.mIconSize);
        defaultAvatarDrawableForContact.draw(canvas);
        return bitmapCreateBitmap;
    }

    void handleFlagDisabled() {
        removeAllShortcuts();
        this.mJobScheduler.cancel(1);
    }

    private void removeAllShortcuts() {
        this.mShortcutManager.removeAllDynamicShortcuts();
        List<ShortcutInfo> pinnedShortcuts = this.mShortcutManager.getPinnedShortcuts();
        ArrayList arrayList = new ArrayList(pinnedShortcuts.size());
        Iterator<ShortcutInfo> it = pinnedShortcuts.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().getId());
        }
        this.mShortcutManager.disableShortcuts((List) arrayList, this.mContext.getString(R.string.dynamic_shortcut_disabled_message));
        if (Log.isLoggable(TAG, 3)) {
            Log.d(TAG, "DynamicShortcuts have been removed.");
        }
    }

    void scheduleUpdateJob() {
        this.mJobScheduler.schedule(new JobInfo.Builder(1, new ComponentName(this.mContext, (Class<?>) ContactsJobService.class)).addTriggerContentUri(new JobInfo.TriggerContentUri(ContactsContract.AUTHORITY_URI, 1)).setTriggerContentUpdateDelay(this.mContentChangeMinUpdateDelay).setTriggerContentMaxDelay(this.mContentChangeMaxUpdateDelay).build());
    }

    void updateInBackground() {
        new ShortcutUpdateTask(this).execute(new Void[0]);
    }

    public static synchronized void initialize(Context context) {
        if (Log.isLoggable(TAG, 3)) {
            Flags flags = Flags.getInstance();
            StringBuilder sb = new StringBuilder();
            sb.append("DyanmicShortcuts.initialize\nVERSION >= N_MR1? ");
            boolean z = true;
            sb.append(Build.VERSION.SDK_INT >= 25);
            sb.append("\nisJobScheduled? ");
            if (!CompatUtils.isLauncherShortcutCompatible() || !isJobScheduled(context)) {
                z = false;
            }
            sb.append(z);
            sb.append("\nminDelay=");
            sb.append(flags.getInteger("Shortcuts__dynamic_min_content_change_update_delay_millis"));
            sb.append("\nmaxDelay=");
            sb.append(flags.getInteger("Shortcuts__dynamic_max_content_change_update_delay_millis"));
            Log.d(TAG, sb.toString());
        }
        if (CompatUtils.isLauncherShortcutCompatible()) {
            DynamicShortcuts dynamicShortcuts = new DynamicShortcuts(context);
            if (!dynamicShortcuts.hasRequiredPermissions()) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("broadcastPermissionsGranted");
                LocalBroadcastManager.getInstance(dynamicShortcuts.mContext).registerReceiver(new PermissionsGrantedReceiver(), intentFilter);
            } else if (!isJobScheduled(context)) {
                new ShortcutUpdateTask(dynamicShortcuts).execute(new Void[0]);
            }
        }
    }

    public static void reset(Context context) {
        ((JobScheduler) context.getSystemService("jobscheduler")).cancel(1);
        if (!CompatUtils.isLauncherShortcutCompatible()) {
            return;
        }
        new DynamicShortcuts(context).removeAllShortcuts();
    }

    boolean hasRequiredPermissions() {
        return PermissionsUtil.hasContactsPermissions(this.mContext);
    }

    public static void updateFromJob(final JobService jobService, final JobParameters jobParameters) {
        new ShortcutUpdateTask(new DynamicShortcuts(jobService)) {
            {
                super(dynamicShortcuts);
            }

            @Override
            protected void onPostExecute(Void r3) {
                super.onPostExecute(r3);
                jobService.jobFinished(jobParameters, false);
            }
        }.execute(new Void[0]);
    }

    public static boolean isJobScheduled(Context context) {
        return ((JobScheduler) context.getSystemService("jobscheduler")).getPendingJob(1) != null;
    }

    public static void reportShortcutUsed(Context context, String str) {
        if (!CompatUtils.isLauncherShortcutCompatible() || str == null) {
            return;
        }
        ((ShortcutManager) context.getSystemService("shortcut")).reportShortcutUsed(str);
    }

    private static class ShortcutUpdateTask extends AsyncTask<Void, Void, Void> {
        private DynamicShortcuts mDynamicShortcuts;

        public ShortcutUpdateTask(DynamicShortcuts dynamicShortcuts) {
            this.mDynamicShortcuts = dynamicShortcuts;
        }

        @Override
        protected Void doInBackground(Void... voidArr) {
            this.mDynamicShortcuts.refresh();
            return null;
        }

        @Override
        protected void onPostExecute(Void r2) {
            if (Log.isLoggable(DynamicShortcuts.TAG, 3)) {
                Log.d(DynamicShortcuts.TAG, "ShorcutUpdateTask.onPostExecute");
            }
            this.mDynamicShortcuts.scheduleUpdateJob();
        }
    }

    private static class PermissionsGrantedReceiver extends BroadcastReceiver {
        private PermissionsGrantedReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
            DynamicShortcuts.initialize(context);
        }
    }
}
