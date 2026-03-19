package com.mediatek.media.mediascanner;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.media.MediaScanner;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class MediaScannerExImpl extends MediaScanner {
    private static final boolean DEBUG;
    private static final boolean LOGD = "eng".equals(Build.TYPE);
    private static final String TAG = "MediaScannerExImpl";
    private MediaInserterExImpl mMediaInserterExImpl;

    static {
        DEBUG = Log.isLoggable(TAG, 3) || LOGD;
    }

    public MediaScannerExImpl(Context context, String str) {
        super(context, str);
    }

    public void preScanAll(String str) {
        try {
            prescan(null, true);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in MediaScanner.scan()", e);
        }
    }

    public void postScanAll(ArrayList<String> arrayList) throws Throwable {
        ?? r0;
        Throwable th;
        Cursor cursorQuery;
        RemoteException e;
        boolean zHasNext;
        try {
            boolean z = this.mProcessPlaylists;
            r0 = z;
            if (z) {
                Iterator<String> it = arrayList.iterator();
                while (true) {
                    zHasNext = it.hasNext();
                    if (!zHasNext) {
                        break;
                    }
                    String next = it.next();
                    MediaScanner.FileEntry fileEntryMakeEntryFor = makeEntryFor(next);
                    long jLastModified = new File(next).lastModified();
                    long j = fileEntryMakeEntryFor != null ? jLastModified - fileEntryMakeEntryFor.mLastModified : 0L;
                    boolean z2 = j > 1 || j < -1;
                    if (fileEntryMakeEntryFor == null || z2) {
                        if (z2) {
                            fileEntryMakeEntryFor.mLastModified = jLastModified;
                        } else {
                            fileEntryMakeEntryFor = new MediaScanner.FileEntry(0L, next, jLastModified, 0);
                        }
                        fileEntryMakeEntryFor.mLastModifiedChanged = true;
                    }
                    this.mPlayLists.add(fileEntryMakeEntryFor);
                }
                processPlayLists();
                r0 = zHasNext;
            }
        } catch (RemoteException e2) {
            String str = TAG;
            Log.e(TAG, "RemoteException in MediaScanner.postScanAll()", e2);
            r0 = str;
        }
        Cursor cursor = null;
        try {
            try {
                cursorQuery = this.mMediaProvider.query(this.mImagesUri.buildUpon().appendQueryParameter("force", "1").build(), ID_PROJECTION, null, null, null, null);
                if (cursorQuery != null) {
                    try {
                        cursorQuery.getCount();
                        cursorQuery.close();
                        cursorQuery = null;
                    } catch (RemoteException e3) {
                        e = e3;
                        Log.e(TAG, "RemoteException in MediaScanner.postScanAll()", e);
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        if (DEBUG) {
                        }
                    }
                }
                Cursor cursorQuery2 = this.mMediaProvider.query(this.mVideoUri.buildUpon().appendQueryParameter("force", "1").build(), ID_PROJECTION, null, null, null, null);
                if (cursorQuery2 != null) {
                    try {
                        cursorQuery2.getCount();
                        cursorQuery2.close();
                    } catch (RemoteException e4) {
                        e = e4;
                        cursorQuery = cursorQuery2;
                        Log.e(TAG, "RemoteException in MediaScanner.postScanAll()", e);
                        if (cursorQuery != null) {
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        r0 = cursorQuery2;
                        if (r0 != 0) {
                            r0.close();
                        }
                        throw th;
                    }
                } else {
                    cursor = cursorQuery2;
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Throwable th3) {
                th = th3;
            }
        } catch (RemoteException e5) {
            cursorQuery = null;
            e = e5;
        } catch (Throwable th4) {
            r0 = 0;
            th = th4;
        }
        if (DEBUG) {
            return;
        }
        Log.v(TAG, "postScanAll");
    }

    public ArrayList<String> scanFolders(Handler handler, String[] strArr, String str, boolean z) {
        try {
            this.mPlayLists.clear();
            this.mMediaInserterExImpl = new MediaInserterExImpl(handler, 100);
            for (String str2 : strArr) {
                if (z) {
                    File file = new File(str2);
                    this.mClient.doScanFile(str2, (String) null, file.lastModified() / 1000, file.length(), file.isDirectory(), false, isNoMediaPath(str2));
                } else {
                    processDirectory(str2, this.mClient);
                }
            }
            this.mMediaInserterExImpl.flushAll();
            this.mMediaInserterExImpl = null;
        } catch (SQLException e) {
            Log.e(TAG, "SQLException in MediaScanner.scan()", e);
        } catch (RemoteException e2) {
            Log.e(TAG, "RemoteException in MediaScanner.scan()", e2);
        } catch (UnsupportedOperationException e3) {
            Log.e(TAG, "UnsupportedOperationException in MediaScanner.scan()", e3);
        }
        return this.mPlaylistFilePathList;
    }

    public ArrayList<String> scanFolders(String[] strArr, String str, boolean z) {
        try {
            this.mPlayLists.clear();
            this.mMediaInserterExImpl = new MediaInserterExImpl(this.mMediaProvider, 500);
            for (String str2 : strArr) {
                File file = new File(str2);
                if (file.exists()) {
                    this.mClient.doScanFile(str2, (String) null, file.lastModified() / 1000, file.length(), file.isDirectory(), false, isNoMediaPath(str2));
                }
                if (!z) {
                    processDirectory(str2, this.mClient);
                }
            }
            this.mMediaInserterExImpl.flushAll();
            this.mMediaInserterExImpl = null;
        } catch (SQLException e) {
            Log.e(TAG, "SQLException in MediaScanner.scan()", e);
        } catch (RemoteException e2) {
            Log.e(TAG, "RemoteException in MediaScanner.scan()", e2);
        } catch (UnsupportedOperationException e3) {
            Log.e(TAG, "UnsupportedOperationException in MediaScanner.scan()", e3);
        }
        return this.mPlaylistFilePathList;
    }
}
