package com.android.browser.widget;

import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MergeCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.android.browser.R;
import com.android.browser.provider.BrowserContract;
import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BookmarkThumbnailWidgetService extends RemoteViewsService {
    private static final String[] PROJECTION = {"_id", "title", "url", "favicon", "folder", "position", "thumbnail", "parent"};

    @Override
    public RemoteViewsService.RemoteViewsFactory onGetViewFactory(Intent intent) {
        int intExtra = intent.getIntExtra("appWidgetId", -1);
        if (intExtra < 0) {
            Log.w("BookmarkThumbnailWidgetService", "Missing EXTRA_APPWIDGET_ID!");
            return null;
        }
        return new BookmarkFactory(getApplicationContext(), intExtra);
    }

    static SharedPreferences getWidgetState(Context context, int i) {
        return context.getSharedPreferences(String.format("widgetState-%d", Integer.valueOf(i)), 0);
    }

    static void deleteWidgetState(Context context, int i) {
        File sharedPrefsFile = context.getSharedPrefsFile(String.format("widgetState-%d", Integer.valueOf(i)));
        if (sharedPrefsFile.exists() && !sharedPrefsFile.delete()) {
            sharedPrefsFile.deleteOnExit();
        }
    }

    static void changeFolder(Context context, Intent intent) {
        int intExtra = intent.getIntExtra("appWidgetId", -1);
        long longExtra = intent.getLongExtra("_id", -1L);
        if (intExtra >= 0 && longExtra >= 0) {
            getWidgetState(context, intExtra).edit().putLong("current_folder", longExtra).commit();
            AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(intExtra, R.id.bookmarks_list);
        }
        BookmarkThumbnailWidgetProvider.refreshWidgets(context);
    }

    static void setupWidgetState(Context context, int i, long j) {
        getWidgetState(context, i).edit().putLong("current_folder", j).putLong("root_folder", j).apply();
    }

    static void removeOrphanedStates(Context context, int[] iArr) {
        File[] fileArrListFiles = context.getSharedPrefsFile("null").getParentFile().listFiles(new StateFilter(iArr));
        if (fileArrListFiles != null) {
            for (File file : fileArrListFiles) {
                Log.w("BookmarkThumbnailWidgetService", "Found orphaned state: " + file.getName());
                if (!file.delete()) {
                    file.deleteOnExit();
                }
            }
        }
    }

    static class StateFilter implements FilenameFilter {
        static final Pattern sStatePattern = Pattern.compile("widgetState-(\\d+)\\.xml");
        HashSet<Integer> mWidgetIds = new HashSet<>();

        StateFilter(int[] iArr) {
            for (int i : iArr) {
                this.mWidgetIds.add(Integer.valueOf(i));
            }
        }

        @Override
        public boolean accept(File file, String str) {
            Matcher matcher = sStatePattern.matcher(str);
            if (matcher.matches()) {
                if (!this.mWidgetIds.contains(Integer.valueOf(Integer.parseInt(matcher.group(1))))) {
                    return true;
                }
                return false;
            }
            return false;
        }
    }

    static class BookmarkFactory implements RemoteViewsService.RemoteViewsFactory {
        private Cursor mBookmarks;
        private Context mContext;
        private int mWidgetId;
        private long mCurrentFolder = -1;
        private long mRootFolder = -1;
        private SharedPreferences mPreferences = null;

        public BookmarkFactory(Context context, int i) {
            this.mContext = context.getApplicationContext();
            this.mWidgetId = i;
        }

        void syncState() {
            if (this.mPreferences == null) {
                this.mPreferences = BookmarkThumbnailWidgetService.getWidgetState(this.mContext, this.mWidgetId);
            }
            long j = this.mPreferences.getLong("current_folder", -1L);
            this.mRootFolder = this.mPreferences.getLong("root_folder", -1L);
            if (j != this.mCurrentFolder) {
                resetBookmarks();
                this.mCurrentFolder = j;
            }
        }

        void saveState() {
            if (this.mPreferences == null) {
                this.mPreferences = BookmarkThumbnailWidgetService.getWidgetState(this.mContext, this.mWidgetId);
            }
            this.mPreferences.edit().putLong("current_folder", this.mCurrentFolder).putLong("root_folder", this.mRootFolder).commit();
        }

        @Override
        public int getCount() {
            if (this.mBookmarks == null) {
                return 0;
            }
            return this.mBookmarks.getCount();
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public RemoteViews getLoadingView() {
            return new RemoteViews(this.mContext.getPackageName(), R.layout.bookmarkthumbnailwidget_item);
        }

        @Override
        public RemoteViews getViewAt(int i) {
            RemoteViews remoteViews;
            Intent intent;
            if (!this.mBookmarks.moveToPosition(i)) {
                return null;
            }
            long j = this.mBookmarks.getLong(0);
            String string = this.mBookmarks.getString(1);
            String string2 = this.mBookmarks.getString(2);
            boolean z = this.mBookmarks.getInt(4) != 0;
            if (z) {
                remoteViews = new RemoteViews(this.mContext.getPackageName(), R.layout.bookmarkthumbnailwidget_item_folder);
            } else {
                remoteViews = new RemoteViews(this.mContext.getPackageName(), R.layout.bookmarkthumbnailwidget_item);
            }
            if (TextUtils.isEmpty(string)) {
                string = string2;
            }
            remoteViews.setTextViewText(R.id.label, string);
            if (z) {
                if (j == this.mCurrentFolder) {
                    j = this.mBookmarks.getLong(7);
                    remoteViews.setImageViewResource(R.id.thumb, R.drawable.thumb_bookmark_widget_folder_back_holo);
                } else {
                    remoteViews.setImageViewResource(R.id.thumb, R.drawable.thumb_bookmark_widget_folder_holo);
                }
                remoteViews.setImageViewResource(R.id.favicon, R.drawable.ic_bookmark_widget_bookmark_holo_dark);
            } else {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                byte[] blob = this.mBookmarks.getBlob(6);
                if (blob != null && blob.length > 0) {
                    remoteViews.setImageViewBitmap(R.id.thumb, BitmapFactory.decodeByteArray(blob, 0, blob.length, options));
                } else {
                    remoteViews.setImageViewResource(R.id.thumb, R.drawable.browser_thumbnail);
                }
                byte[] blob2 = this.mBookmarks.getBlob(3);
                if (blob2 == null || blob2.length <= 0) {
                    remoteViews.setImageViewResource(R.id.favicon, R.drawable.app_web_browser_sm);
                } else {
                    remoteViews.setImageViewBitmap(R.id.favicon, BitmapFactory.decodeByteArray(blob2, 0, blob2.length, options));
                }
            }
            if (z) {
                intent = new Intent("com.android.browser.widget.CHANGE_FOLDER").putExtra("appWidgetId", this.mWidgetId).putExtra("_id", j);
            } else if (!TextUtils.isEmpty(string2)) {
                intent = new Intent("android.intent.action.VIEW").addCategory("android.intent.category.BROWSABLE").setData(Uri.parse(string2));
            } else {
                intent = new Intent("show_browser");
            }
            remoteViews.setOnClickFillInIntent(R.id.list_item, intent);
            return remoteViews;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public void onCreate() {
        }

        @Override
        public void onDestroy() {
            if (this.mBookmarks != null) {
                this.mBookmarks.close();
                this.mBookmarks = null;
            }
            BookmarkThumbnailWidgetService.deleteWidgetState(this.mContext, this.mWidgetId);
        }

        @Override
        public void onDataSetChanged() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            syncState();
            if (this.mRootFolder < 0 || this.mCurrentFolder < 0) {
                this.mRootFolder = 1L;
                this.mCurrentFolder = this.mRootFolder;
                saveState();
            }
            loadBookmarks();
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }

        private void resetBookmarks() {
            if (this.mBookmarks != null) {
                this.mBookmarks.close();
                this.mBookmarks = null;
            }
        }

        void loadBookmarks() {
            resetBookmarks();
            this.mBookmarks = this.mContext.getContentResolver().query(ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI_DEFAULT_FOLDER, this.mCurrentFolder), BookmarkThumbnailWidgetService.PROJECTION, null, null, null);
            if (this.mCurrentFolder != this.mRootFolder) {
                this.mBookmarks = new MergeCursor(new Cursor[]{this.mContext.getContentResolver().query(ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, this.mCurrentFolder), BookmarkThumbnailWidgetService.PROJECTION, null, null, null), this.mBookmarks});
                if (this.mBookmarks.getCount() == 0) {
                    this.mBookmarks = new MergeCursor(new Cursor[]{this.mContext.getContentResolver().query(BrowserContract.Bookmarks.CONTENT_URI, BookmarkThumbnailWidgetService.PROJECTION, "parent=?", new String[]{String.valueOf(this.mRootFolder)}, null), this.mBookmarks});
                }
            }
        }
    }
}
