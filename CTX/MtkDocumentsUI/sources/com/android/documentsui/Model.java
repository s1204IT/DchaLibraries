package com.android.documentsui;

import android.app.AuthenticationRequiredException;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import com.android.documentsui.base.DocumentFilters;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.EventListener;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.selection.Selection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class Model {
    static final boolean $assertionsDisabled = false;
    public DocumentInfo doc;
    public String error;
    public String info;
    private Cursor mCursor;
    private int mCursorCount;
    private final Features mFeatures;
    private boolean mIsLoading;
    private final Object mLock = new Object();
    private final Map<String, Integer> mPositions = new HashMap();
    private final Set<String> mFileNames = new HashSet();
    private List<EventListener<Update>> mUpdateListeners = new ArrayList();
    private int mCount = 0;
    private int mDrmLevel = -1;
    private String[] mIds = new String[0];

    public Model(Features features) {
        this.mFeatures = features;
    }

    public void addUpdateListener(EventListener<Update> eventListener) {
        this.mUpdateListeners.add(eventListener);
    }

    public void removeUpdateListener(EventListener<Update> eventListener) {
        this.mUpdateListeners.remove(eventListener);
    }

    private void notifyUpdateListeners() {
        Iterator<EventListener<Update>> it = this.mUpdateListeners.iterator();
        while (it.hasNext()) {
            it.next().accept(Update.UPDATE);
        }
    }

    private void notifyUpdateListeners(Exception exc) {
        Update update = new Update(exc, this.mFeatures.isRemoteActionsEnabled());
        Iterator<EventListener<Update>> it = this.mUpdateListeners.iterator();
        while (it.hasNext()) {
            it.next().accept(update);
        }
    }

    public void reset() {
        this.mCursor = null;
        this.mCursorCount = 0;
        this.mIds = new String[0];
        this.mPositions.clear();
        this.info = null;
        this.error = null;
        this.doc = null;
        this.mIsLoading = false;
        this.mFileNames.clear();
        notifyUpdateListeners();
    }

    protected void update(DirectoryResult directoryResult) {
        if (SharedMinimal.DEBUG) {
            Log.i("Model", "Updating model with new result set.");
        }
        if (directoryResult.exception != null) {
            Log.e("Model", "Error while loading directory contents", directoryResult.exception);
            reset();
            notifyUpdateListeners(directoryResult.exception);
            return;
        }
        this.mCursor = directoryResult.cursor;
        this.mCursorCount = this.mCursor.getCount();
        this.doc = directoryResult.doc;
        updateModelData();
        Bundle extras = this.mCursor.getExtras();
        if (extras != null) {
            this.info = extras.getString("info");
            this.error = extras.getString("error");
            this.mIsLoading = extras.getBoolean("loading", false);
        }
        notifyUpdateListeners();
    }

    public int getItemCount() {
        return this.mCount;
    }

    private void updateModelData() {
        this.mIds = new String[this.mCursorCount];
        this.mFileNames.clear();
        int i = 15;
        if (DocumentsFeatureOption.IS_SUPPORT_DRM) {
            int i2 = this.mDrmLevel;
            if (i2 != 4) {
                switch (i2) {
                    case 1:
                        i = 1;
                        break;
                    case 2:
                        i = 4;
                        break;
                }
            }
        } else {
            i = 0;
        }
        this.mCursor.moveToPosition(-1);
        this.mCount = 0;
        for (int i3 = 0; i3 < this.mCursorCount; i3++) {
            if (!this.mCursor.moveToNext()) {
                Log.e("Model", "Fail to move cursor to next pos: " + i3);
                return;
            }
            if (DocumentsFeatureOption.IS_SUPPORT_DRM) {
                boolean z = DocumentInfo.getCursorInt(this.mCursor, "is_drm") > 0;
                int cursorInt = DocumentInfo.getCursorInt(this.mCursor, "drm_method");
                if (!z || ((this.mDrmLevel <= 0 || cursorInt >= 0) && (i & cursorInt) != 0)) {
                }
            } else {
                if (this.mCursor instanceof MergeCursor) {
                    this.mIds[this.mCount] = DocumentInfo.getCursorString(this.mCursor, "android:authority") + "|" + DocumentInfo.getCursorString(this.mCursor, "document_id");
                } else {
                    this.mIds[this.mCount] = DocumentInfo.getCursorString(this.mCursor, "document_id");
                }
                this.mFileNames.add(DocumentInfo.getCursorString(this.mCursor, "_display_name"));
                this.mCount++;
            }
        }
        this.mPositions.clear();
        for (int i4 = 0; i4 < this.mCount; i4++) {
            this.mPositions.put(this.mIds[i4], Integer.valueOf(i4));
        }
    }

    public boolean hasFileWithName(String str) {
        return this.mFileNames.contains(str);
    }

    public Cursor getItem(String str) {
        Integer num = this.mPositions.get(str);
        if (num == null) {
            if (SharedMinimal.DEBUG) {
                Log.d("Model", "Unabled to find cursor position for modelId: " + str);
            }
            return null;
        }
        synchronized (this.mLock) {
            if (SharedMinimal.DEBUG) {
                Log.d("Model", "getItem modelId: " + str + "pos " + num);
            }
        }
        if (this.mCursor != null && !this.mCursor.isClosed() && !this.mCursor.moveToPosition(num.intValue())) {
            if (SharedMinimal.DEBUG) {
                Log.d("Model", "Unabled to move cursor to position " + num + " for modelId: " + str);
            }
            return null;
        }
        return this.mCursor;
    }

    public boolean isLoading() {
        return this.mIsLoading;
    }

    public List<DocumentInfo> getDocuments(Selection selection) {
        return loadDocuments(selection, DocumentFilters.ANY);
    }

    public DocumentInfo getDocument(String str) {
        Cursor item = getItem(str);
        if (item == null) {
            return null;
        }
        return DocumentInfo.fromDirectoryCursor(item);
    }

    public List<DocumentInfo> loadDocuments(Selection selection, Predicate<Cursor> predicate) {
        int size = selection != null ? selection.size() : 0;
        ArrayList arrayList = new ArrayList(size);
        synchronized (this.mLock) {
            Log.d("Model", "getDocuments items.size: " + size);
            Iterator<String> it = selection.iterator();
            while (it.hasNext()) {
                DocumentInfo documentInfoLoadDocument = loadDocument(it.next(), predicate);
                if (documentInfoLoadDocument != null) {
                    arrayList.add(documentInfoLoadDocument);
                }
            }
        }
        return arrayList;
    }

    public boolean hasDocuments(Selection selection, Predicate<Cursor> predicate) {
        Iterator<String> it = selection.iterator();
        while (it.hasNext()) {
            if (loadDocument(it.next(), predicate) != null) {
                return true;
            }
        }
        return false;
    }

    private DocumentInfo loadDocument(String str, Predicate<Cursor> predicate) {
        Cursor item = getItem(str);
        if (item == null) {
            Log.w("Model", "Unable to obtain document for modelId: " + str);
            return null;
        }
        if (predicate.test(item)) {
            return DocumentInfo.fromDirectoryCursor(item);
        }
        if (SharedMinimal.VERBOSE) {
            Log.v("Model", "Filtered out document from results: " + str);
        }
        return null;
    }

    public Uri getItemUri(String str) {
        return DocumentInfo.getUri(getItem(str));
    }

    public String[] getModelIds() {
        return this.mIds;
    }

    public static class Update {
        static final boolean $assertionsDisabled = false;
        public static final Update UPDATE = new Update();
        private final Exception mException;
        private final boolean mRemoteActionEnabled;
        private final int mUpdateType;

        private Update() {
            this.mUpdateType = 0;
            this.mException = null;
            this.mRemoteActionEnabled = false;
        }

        public Update(Exception exc, boolean z) {
            this.mUpdateType = 1;
            this.mException = exc;
            this.mRemoteActionEnabled = z;
        }

        public boolean hasException() {
            return this.mUpdateType == 1;
        }

        public boolean hasAuthenticationException() {
            return this.mRemoteActionEnabled && hasException() && (this.mException instanceof AuthenticationRequiredException);
        }

        public Exception getException() {
            return this.mException;
        }
    }
}
