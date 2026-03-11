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
        int widgetId = intent.getIntExtra("appWidgetId", -1);
        if (widgetId < 0) {
            Log.w("BookmarkThumbnailWidgetService", "Missing EXTRA_APPWIDGET_ID!");
            return null;
        }
        return new BookmarkFactory(getApplicationContext(), widgetId);
    }

    static SharedPreferences getWidgetState(Context context, int widgetId) {
        return context.getSharedPreferences(String.format("widgetState-%d", Integer.valueOf(widgetId)), 0);
    }

    static void deleteWidgetState(Context context, int widgetId) {
        File file = context.getSharedPrefsFile(String.format("widgetState-%d", Integer.valueOf(widgetId)));
        if (!file.exists() || file.delete()) {
            return;
        }
        file.deleteOnExit();
    }

    static void changeFolder(Context context, Intent intent) {
        int wid = intent.getIntExtra("appWidgetId", -1);
        long fid = intent.getLongExtra("_id", -1L);
        if (wid >= 0 && fid >= 0) {
            SharedPreferences prefs = getWidgetState(context, wid);
            prefs.edit().putLong("current_folder", fid).commit();
            AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(wid, R.id.bookmarks_list);
        }
        BookmarkThumbnailWidgetProvider.refreshWidgets(context);
    }

    static void setupWidgetState(Context context, int widgetId, long rootFolder) {
        SharedPreferences pref = getWidgetState(context, widgetId);
        pref.edit().putLong("current_folder", rootFolder).putLong("root_folder", rootFolder).apply();
    }

    static void removeOrphanedStates(Context context, int[] widgetIds) {
        File prefsDirectory = context.getSharedPrefsFile("null").getParentFile();
        File[] widgetStates = prefsDirectory.listFiles(new StateFilter(widgetIds));
        if (widgetStates == null) {
            return;
        }
        for (File f : widgetStates) {
            Log.w("BookmarkThumbnailWidgetService", "Found orphaned state: " + f.getName());
            if (!f.delete()) {
                f.deleteOnExit();
            }
        }
    }

    static class StateFilter implements FilenameFilter {
        static final Pattern sStatePattern = Pattern.compile("widgetState-(\\d+)\\.xml");
        HashSet<Integer> mWidgetIds = new HashSet<>();

        StateFilter(int[] ids) {
            for (int id : ids) {
                this.mWidgetIds.add(Integer.valueOf(id));
            }
        }

        @Override
        public boolean accept(File dir, String filename) {
            Matcher m = sStatePattern.matcher(filename);
            if (m.matches()) {
                int id = Integer.parseInt(m.group(1));
                return !this.mWidgetIds.contains(Integer.valueOf(id));
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

        public BookmarkFactory(Context context, int widgetId) {
            this.mContext = context.getApplicationContext();
            this.mWidgetId = widgetId;
        }

        void syncState() {
            if (this.mPreferences == null) {
                this.mPreferences = BookmarkThumbnailWidgetService.getWidgetState(this.mContext, this.mWidgetId);
            }
            long currentFolder = this.mPreferences.getLong("current_folder", -1L);
            this.mRootFolder = this.mPreferences.getLong("root_folder", -1L);
            if (currentFolder == this.mCurrentFolder) {
                return;
            }
            resetBookmarks();
            this.mCurrentFolder = currentFolder;
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
        public long getItemId(int position) {
            return position;
        }

        @Override
        public RemoteViews getLoadingView() {
            return new RemoteViews(this.mContext.getPackageName(), R.layout.bookmarkthumbnailwidget_item);
        }

        @Override
        public RemoteViews getViewAt(int position) {
            RemoteViews views;
            Intent fillin;
            if (!this.mBookmarks.moveToPosition(position)) {
                return null;
            }
            long id = this.mBookmarks.getLong(0);
            String title = this.mBookmarks.getString(1);
            String url = this.mBookmarks.getString(2);
            boolean isFolder = this.mBookmarks.getInt(4) != 0;
            if (isFolder) {
                views = new RemoteViews(this.mContext.getPackageName(), R.layout.bookmarkthumbnailwidget_item_folder);
            } else {
                views = new RemoteViews(this.mContext.getPackageName(), R.layout.bookmarkthumbnailwidget_item);
            }
            String displayTitle = title;
            if (TextUtils.isEmpty(title)) {
                displayTitle = url;
            }
            views.setTextViewText(R.id.label, displayTitle);
            if (isFolder) {
                if (id == this.mCurrentFolder) {
                    id = this.mBookmarks.getLong(7);
                    views.setImageViewResource(R.id.thumb, R.drawable.thumb_bookmark_widget_folder_back_holo);
                } else {
                    views.setImageViewResource(R.id.thumb, R.drawable.thumb_bookmark_widget_folder_holo);
                }
                views.setImageViewResource(R.id.favicon, R.drawable.ic_bookmark_widget_bookmark_holo_dark);
                views.setDrawableParameters(R.id.thumb, true, 0, -1, null, -1);
            } else {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                byte[] blob = this.mBookmarks.getBlob(6);
                views.setDrawableParameters(R.id.thumb, true, 255, -1, null, -1);
                if (blob != null && blob.length > 0) {
                    Bitmap thumbnail = BitmapFactory.decodeByteArray(blob, 0, blob.length, options);
                    views.setImageViewBitmap(R.id.thumb, thumbnail);
                } else {
                    views.setImageViewResource(R.id.thumb, R.drawable.browser_thumbnail);
                }
                byte[] blob2 = this.mBookmarks.getBlob(3);
                if (blob2 != null && blob2.length > 0) {
                    Bitmap favicon = BitmapFactory.decodeByteArray(blob2, 0, blob2.length, options);
                    views.setImageViewBitmap(R.id.favicon, favicon);
                } else {
                    views.setImageViewResource(R.id.favicon, R.drawable.app_web_browser_sm);
                }
            }
            if (isFolder) {
                fillin = new Intent("com.android.browser.widget.CHANGE_FOLDER").putExtra("appWidgetId", this.mWidgetId).putExtra("_id", id);
            } else if (!TextUtils.isEmpty(url)) {
                fillin = new Intent("android.intent.action.VIEW").addCategory("android.intent.category.BROWSABLE").setData(Uri.parse(url));
            } else {
                fillin = new Intent("show_browser");
            }
            views.setOnClickFillInIntent(R.id.list_item, fillin);
            return views;
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
            long token = Binder.clearCallingIdentity();
            syncState();
            if (this.mRootFolder < 0 || this.mCurrentFolder < 0) {
                this.mRootFolder = 1L;
                this.mCurrentFolder = this.mRootFolder;
                saveState();
            }
            loadBookmarks();
            Binder.restoreCallingIdentity(token);
        }

        private void resetBookmarks() {
            if (this.mBookmarks == null) {
                return;
            }
            this.mBookmarks.close();
            this.mBookmarks = null;
        }

        void loadBookmarks() {
            resetBookmarks();
            Uri uri = ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI_DEFAULT_FOLDER, this.mCurrentFolder);
            this.mBookmarks = this.mContext.getContentResolver().query(uri, BookmarkThumbnailWidgetService.PROJECTION, null, null, null);
            if (this.mCurrentFolder == this.mRootFolder) {
                return;
            }
            Uri uri2 = ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, this.mCurrentFolder);
            Cursor c = this.mContext.getContentResolver().query(uri2, BookmarkThumbnailWidgetService.PROJECTION, null, null, null);
            this.mBookmarks = new MergeCursor(new Cursor[]{c, this.mBookmarks});
            if (this.mBookmarks.getCount() != 0) {
                return;
            }
            String[] args = {String.valueOf(this.mRootFolder)};
            Cursor cursor = this.mContext.getContentResolver().query(BrowserContract.Bookmarks.CONTENT_URI, BookmarkThumbnailWidgetService.PROJECTION, "parent=?", args, null);
            this.mBookmarks = new MergeCursor(new Cursor[]{cursor, this.mBookmarks});
        }
    }
}
