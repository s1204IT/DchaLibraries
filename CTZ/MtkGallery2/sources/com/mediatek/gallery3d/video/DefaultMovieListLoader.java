package com.mediatek.gallery3d.video;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallery3d.video.IMovieListLoader;

public class DefaultMovieListLoader implements IMovieListLoader {
    private static final String TAG = "VP_MovieListLoader";
    String EXTRA_ALL_VIDEO_FOLDER = "mediatek.intent.extra.ALL_VIDEO_FOLDER";
    String EXTRA_ORDERBY = "mediatek.intent.extra.VIDEO_LIST_ORDERBY";
    private MovieListFetcherTask mListTask;

    @Override
    public void fillVideoList(Context context, Intent intent, IMovieListLoader.LoaderListener loaderListener, IMovieItem iMovieItem) {
        boolean booleanExtra;
        String stringExtra;
        if (intent.hasExtra(this.EXTRA_ALL_VIDEO_FOLDER)) {
            booleanExtra = intent.getBooleanExtra(this.EXTRA_ALL_VIDEO_FOLDER, false);
        } else {
            booleanExtra = false;
        }
        if (intent.hasExtra(this.EXTRA_ORDERBY)) {
            stringExtra = intent.getStringExtra(this.EXTRA_ORDERBY);
        } else {
            stringExtra = "datetaken DESC, _id DESC ";
        }
        cancelList();
        this.mListTask = new MovieListFetcherTask(context, booleanExtra, loaderListener, stringExtra);
        this.mListTask.execute(iMovieItem);
        Log.d(TAG, "fillVideoList() fetechAll=" + booleanExtra + ", orderBy=" + stringExtra);
    }

    @Override
    public void cancelList() {
        if (this.mListTask != null) {
            this.mListTask.cancel(true);
        }
    }

    private class MovieListFetcherTask extends AsyncTask<IMovieItem, Void, IMovieList> {
        private static final boolean LOG = true;
        private static final String TAG = "VP_MovieListFetcher";
        private final Context mContext;
        private final ContentResolver mCr;
        private final boolean mFetechAll;
        private final IMovieListLoader.LoaderListener mFetecherListener;
        private final String mOrderBy;

        public MovieListFetcherTask(Context context, boolean z, IMovieListLoader.LoaderListener loaderListener, String str) {
            this.mContext = context;
            this.mCr = context.getContentResolver();
            this.mFetecherListener = loaderListener;
            this.mFetechAll = z;
            this.mOrderBy = str;
            Log.d(TAG, "MovieListFetcherTask() fetechAll=" + z + ", orderBy=" + str);
        }

        @Override
        protected void onPostExecute(IMovieList iMovieList) {
            Log.d(TAG, "onPostExecute() isCancelled()=" + isCancelled());
            if (!isCancelled() && this.mFetecherListener != null) {
                this.mFetecherListener.onListLoaded(iMovieList);
            }
        }

        @Override
        protected IMovieList doInBackground(IMovieItem... iMovieItemArr) throws Throwable {
            Log.d(TAG, "doInBackground() begin");
            IMovieList iMovieListFillUriList = null;
            if (iMovieItemArr[0] == null) {
                return null;
            }
            Uri uri = iMovieItemArr[0].getUri();
            String mimeType = iMovieItemArr[0].getMimeType();
            if (this.mFetechAll) {
                if (MovieUtils.isLocalFile(uri, mimeType) && String.valueOf(uri).toLowerCase().startsWith("content://media")) {
                    iMovieListFillUriList = fillUriList(null, null, iMovieItemArr[0].getCurId(), iMovieItemArr[0]);
                }
            } else if (MovieUtils.isLocalFile(uri, mimeType)) {
                String strValueOf = String.valueOf(uri);
                if (strValueOf.toLowerCase().startsWith("content://media") || strValueOf.toLowerCase().startsWith("file://")) {
                    iMovieListFillUriList = fillUriList("bucket_id=? ", new String[]{String.valueOf(iMovieItemArr[0].getBuckedId())}, iMovieItemArr[0].getCurId(), iMovieItemArr[0]);
                }
            }
            Log.d(TAG, "doInBackground() done return " + iMovieListFillUriList);
            return iMovieListFillUriList;
        }

        private IMovieList fillUriList(String str, String[] strArr, long j, IMovieItem iMovieItem) throws Throwable {
            DefaultMovieList defaultMovieList;
            Cursor cursorQuery;
            Cursor cursor = null;
            try {
                try {
                    cursorQuery = this.mCr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, new String[]{BookmarkEnhance.COLUMN_ID, BookmarkEnhance.COLUMN_MEDIA_TYPE, BookmarkEnhance.COLUMN_TITLE}, str, strArr, this.mOrderBy);
                    if (cursorQuery != null) {
                        try {
                            try {
                                if (cursorQuery.getCount() > 0) {
                                    defaultMovieList = new DefaultMovieList();
                                    boolean z = false;
                                    while (cursorQuery.moveToNext()) {
                                        try {
                                            long j2 = cursorQuery.getLong(0);
                                            if (z || j2 != j) {
                                                defaultMovieList.add(new DefaultMovieItem(this.mContext, ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, j2), cursorQuery.getString(1), cursorQuery.getString(2)));
                                            } else {
                                                defaultMovieList.add(iMovieItem);
                                                z = true;
                                            }
                                        } catch (SQLiteException e) {
                                            e = e;
                                            cursor = cursorQuery;
                                            e.printStackTrace();
                                            if (cursor != null) {
                                                cursor.close();
                                            }
                                            cursorQuery = cursor;
                                        }
                                    }
                                } else {
                                    defaultMovieList = null;
                                }
                                if (cursorQuery != null) {
                                    cursorQuery.close();
                                }
                            } catch (SQLiteException e2) {
                                e = e2;
                                defaultMovieList = null;
                            }
                        } catch (Throwable th) {
                            th = th;
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    cursorQuery = cursor;
                }
            } catch (SQLiteException e3) {
                e = e3;
                defaultMovieList = null;
            }
            Log.d(TAG, "fillUriList() cursor=" + cursorQuery + ", return " + defaultMovieList);
            return defaultMovieList;
        }
    }
}
