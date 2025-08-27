package com.android.launcher3.model;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherProvider;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.model.ModelWriter;
import com.android.launcher3.util.ContentWriter;
import com.android.launcher3.util.ItemInfoMatcher;
import com.android.launcher3.util.LooperExecutor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.Executor;

/* loaded from: classes.dex */
public class ModelWriter {
    private static final String TAG = "ModelWriter";
    private final BgDataModel mBgDataModel;
    private final Context mContext;
    private final boolean mHasVerticalHotseat;
    private final LauncherModel mModel;
    private final boolean mVerifyChanges;
    private final Executor mWorkerExecutor = new LooperExecutor(LauncherModel.getWorkerLooper());
    private final Handler mUiHandler = new Handler(Looper.getMainLooper());

    public ModelWriter(Context context, LauncherModel launcherModel, BgDataModel bgDataModel, boolean z, boolean z2) {
        this.mContext = context;
        this.mModel = launcherModel;
        this.mBgDataModel = bgDataModel;
        this.mHasVerticalHotseat = z;
        this.mVerifyChanges = z2;
    }

    private void updateItemInfoProps(ItemInfo itemInfo, long j, long j2, int i, int i2) {
        itemInfo.container = j;
        itemInfo.cellX = i;
        itemInfo.cellY = i2;
        if (j == -101) {
            itemInfo.screenId = this.mHasVerticalHotseat ? (LauncherAppState.getIDP(this.mContext).numHotseatIcons - i2) - 1 : i;
        } else {
            itemInfo.screenId = j2;
        }
    }

    public void addOrMoveItemInDatabase(ItemInfo itemInfo, long j, long j2, int i, int i2) {
        if (itemInfo.container == -1) {
            addItemToDatabase(itemInfo, j, j2, i, i2);
        } else {
            moveItemInDatabase(itemInfo, j, j2, i, i2);
        }
    }

    private void checkItemInfoLocked(long j, ItemInfo itemInfo, StackTraceElement[] stackTraceElementArr) {
        ItemInfo itemInfo2 = this.mBgDataModel.itemsIdMap.get(j);
        if (itemInfo2 != null && itemInfo != itemInfo2) {
            if ((itemInfo2 instanceof ShortcutInfo) && (itemInfo instanceof ShortcutInfo)) {
                ShortcutInfo shortcutInfo = (ShortcutInfo) itemInfo2;
                ShortcutInfo shortcutInfo2 = (ShortcutInfo) itemInfo;
                if (shortcutInfo.title.toString().equals(shortcutInfo2.title.toString()) && shortcutInfo.intent.filterEquals(shortcutInfo2.intent) && shortcutInfo.id == shortcutInfo2.id && shortcutInfo.itemType == shortcutInfo2.itemType && shortcutInfo.container == shortcutInfo2.container && shortcutInfo.screenId == shortcutInfo2.screenId && shortcutInfo.cellX == shortcutInfo2.cellX && shortcutInfo.cellY == shortcutInfo2.cellY && shortcutInfo.spanX == shortcutInfo2.spanX && shortcutInfo.spanY == shortcutInfo2.spanY) {
                    return;
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("item: ");
            sb.append(itemInfo != null ? itemInfo.toString() : "null");
            sb.append("modelItem: ");
            sb.append(itemInfo2 != null ? itemInfo2.toString() : "null");
            sb.append("Error: ItemInfo passed to checkItemInfo doesn't match original");
            RuntimeException runtimeException = new RuntimeException(sb.toString());
            if (stackTraceElementArr != null) {
                runtimeException.setStackTrace(stackTraceElementArr);
                throw runtimeException;
            }
            throw runtimeException;
        }
    }

    public void moveItemInDatabase(ItemInfo itemInfo, long j, long j2, int i, int i2) {
        updateItemInfoProps(itemInfo, j, j2, i, i2);
        this.mWorkerExecutor.execute(new UpdateItemRunnable(itemInfo, new ContentWriter(this.mContext).put(LauncherSettings.Favorites.CONTAINER, Long.valueOf(itemInfo.container)).put(LauncherSettings.Favorites.CELLX, Integer.valueOf(itemInfo.cellX)).put(LauncherSettings.Favorites.CELLY, Integer.valueOf(itemInfo.cellY)).put(LauncherSettings.Favorites.RANK, Integer.valueOf(itemInfo.rank)).put(LauncherSettings.Favorites.SCREEN, Long.valueOf(itemInfo.screenId))));
    }

    public void moveItemsInDatabase(ArrayList<ItemInfo> arrayList, long j, int i) {
        ArrayList arrayList2 = new ArrayList();
        int size = arrayList.size();
        for (int i2 = 0; i2 < size; i2++) {
            ItemInfo itemInfo = arrayList.get(i2);
            updateItemInfoProps(itemInfo, j, i, itemInfo.cellX, itemInfo.cellY);
            ContentValues contentValues = new ContentValues();
            contentValues.put(LauncherSettings.Favorites.CONTAINER, Long.valueOf(itemInfo.container));
            contentValues.put(LauncherSettings.Favorites.CELLX, Integer.valueOf(itemInfo.cellX));
            contentValues.put(LauncherSettings.Favorites.CELLY, Integer.valueOf(itemInfo.cellY));
            contentValues.put(LauncherSettings.Favorites.RANK, Integer.valueOf(itemInfo.rank));
            contentValues.put(LauncherSettings.Favorites.SCREEN, Long.valueOf(itemInfo.screenId));
            arrayList2.add(contentValues);
        }
        this.mWorkerExecutor.execute(new UpdateItemsRunnable(arrayList, arrayList2));
    }

    public void modifyItemInDatabase(ItemInfo itemInfo, long j, long j2, int i, int i2, int i3, int i4) {
        updateItemInfoProps(itemInfo, j, j2, i, i2);
        itemInfo.spanX = i3;
        itemInfo.spanY = i4;
        this.mWorkerExecutor.execute(new UpdateItemRunnable(itemInfo, new ContentWriter(this.mContext).put(LauncherSettings.Favorites.CONTAINER, Long.valueOf(itemInfo.container)).put(LauncherSettings.Favorites.CELLX, Integer.valueOf(itemInfo.cellX)).put(LauncherSettings.Favorites.CELLY, Integer.valueOf(itemInfo.cellY)).put(LauncherSettings.Favorites.RANK, Integer.valueOf(itemInfo.rank)).put(LauncherSettings.Favorites.SPANX, Integer.valueOf(itemInfo.spanX)).put(LauncherSettings.Favorites.SPANY, Integer.valueOf(itemInfo.spanY)).put(LauncherSettings.Favorites.SCREEN, Long.valueOf(itemInfo.screenId))));
    }

    public void updateItemInDatabase(ItemInfo itemInfo) {
        ContentWriter contentWriter = new ContentWriter(this.mContext);
        itemInfo.onAddToDatabase(contentWriter);
        this.mWorkerExecutor.execute(new UpdateItemRunnable(itemInfo, contentWriter));
    }

    public void addItemToDatabase(final ItemInfo itemInfo, long j, long j2, int i, int i2) {
        updateItemInfoProps(itemInfo, j, j2, i, i2);
        final ContentWriter contentWriter = new ContentWriter(this.mContext);
        final ContentResolver contentResolver = this.mContext.getContentResolver();
        itemInfo.onAddToDatabase(contentWriter);
        itemInfo.id = LauncherSettings.Settings.call(contentResolver, LauncherSettings.Settings.METHOD_NEW_ITEM_ID).getLong(LauncherSettings.Settings.EXTRA_VALUE);
        contentWriter.put("_id", Long.valueOf(itemInfo.id));
        final ModelVerifier modelVerifier = new ModelVerifier();
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        this.mWorkerExecutor.execute(new Runnable() { // from class: com.android.launcher3.model.-$$Lambda$ModelWriter$ItEGbDR_6cXLsuu8tWhEMI90Ypo
            @Override // java.lang.Runnable
            public final void run() {
                ModelWriter.lambda$addItemToDatabase$0(this.f$0, contentResolver, contentWriter, itemInfo, stackTrace, modelVerifier);
            }
        });
    }

    public static /* synthetic */ void lambda$addItemToDatabase$0(ModelWriter modelWriter, ContentResolver contentResolver, ContentWriter contentWriter, ItemInfo itemInfo, StackTraceElement[] stackTraceElementArr, ModelVerifier modelVerifier) {
        contentResolver.insert(LauncherSettings.Favorites.CONTENT_URI, contentWriter.getValues(modelWriter.mContext));
        synchronized (modelWriter.mBgDataModel) {
            modelWriter.checkItemInfoLocked(itemInfo.id, itemInfo, stackTraceElementArr);
            modelWriter.mBgDataModel.addItem(modelWriter.mContext, itemInfo, true);
            modelVerifier.verifyModel();
        }
    }

    public void deleteItemFromDatabase(ItemInfo itemInfo) {
        deleteItemsFromDatabase(Arrays.asList(itemInfo));
    }

    public void deleteItemsFromDatabase(ItemInfoMatcher itemInfoMatcher) {
        deleteItemsFromDatabase(itemInfoMatcher.filterItemInfos(this.mBgDataModel.itemsIdMap));
    }

    public void deleteItemsFromDatabase(final Iterable<? extends ItemInfo> iterable) {
        final ModelVerifier modelVerifier = new ModelVerifier();
        this.mWorkerExecutor.execute(new Runnable() { // from class: com.android.launcher3.model.-$$Lambda$ModelWriter$dBgTNmWSiHJipdaOvZxnLBfkuno
            @Override // java.lang.Runnable
            public final void run() {
                ModelWriter.lambda$deleteItemsFromDatabase$1(this.f$0, iterable, modelVerifier);
            }
        });
    }

    public static /* synthetic */ void lambda$deleteItemsFromDatabase$1(ModelWriter modelWriter, Iterable iterable, ModelVerifier modelVerifier) {
        Iterator it = iterable.iterator();
        while (it.hasNext()) {
            ItemInfo itemInfo = (ItemInfo) it.next();
            modelWriter.mContext.getContentResolver().delete(LauncherSettings.Favorites.getContentUri(itemInfo.id), null, null);
            modelWriter.mBgDataModel.removeItem(modelWriter.mContext, itemInfo);
            modelVerifier.verifyModel();
        }
    }

    public void deleteFolderAndContentsFromDatabase(final FolderInfo folderInfo) {
        final ModelVerifier modelVerifier = new ModelVerifier();
        this.mWorkerExecutor.execute(new Runnable() { // from class: com.android.launcher3.model.-$$Lambda$ModelWriter$L-XuB8STDjB_-Q2myy_RxlNLmeY
            @Override // java.lang.Runnable
            public final void run() {
                ModelWriter.lambda$deleteFolderAndContentsFromDatabase$2(this.f$0, folderInfo, modelVerifier);
            }
        });
    }

    public static /* synthetic */ void lambda$deleteFolderAndContentsFromDatabase$2(ModelWriter modelWriter, FolderInfo folderInfo, ModelVerifier modelVerifier) {
        ContentResolver contentResolver = modelWriter.mContext.getContentResolver();
        contentResolver.delete(LauncherSettings.Favorites.CONTENT_URI, "container=" + folderInfo.id, null);
        modelWriter.mBgDataModel.removeItem(modelWriter.mContext, folderInfo.contents);
        folderInfo.contents.clear();
        contentResolver.delete(LauncherSettings.Favorites.getContentUri(folderInfo.id), null, null);
        modelWriter.mBgDataModel.removeItem(modelWriter.mContext, folderInfo);
        modelVerifier.verifyModel();
    }

    private class UpdateItemRunnable extends UpdateItemBaseRunnable {
        private final ItemInfo mItem;
        private final long mItemId;
        private final ContentWriter mWriter;

        UpdateItemRunnable(ItemInfo itemInfo, ContentWriter contentWriter) {
            super();
            this.mItem = itemInfo;
            this.mWriter = contentWriter;
            this.mItemId = itemInfo.id;
        }

        @Override // java.lang.Runnable
        public void run() {
            ModelWriter.this.mContext.getContentResolver().update(LauncherSettings.Favorites.getContentUri(this.mItemId), this.mWriter.getValues(ModelWriter.this.mContext), null, null);
            updateItemArrays(this.mItem, this.mItemId);
        }
    }

    private class UpdateItemsRunnable extends UpdateItemBaseRunnable {
        private final ArrayList<ItemInfo> mItems;
        private final ArrayList<ContentValues> mValues;

        UpdateItemsRunnable(ArrayList<ItemInfo> arrayList, ArrayList<ContentValues> arrayList2) {
            super();
            this.mValues = arrayList2;
            this.mItems = arrayList;
        }

        @Override // java.lang.Runnable
        public void run() throws RemoteException, OperationApplicationException {
            ArrayList<ContentProviderOperation> arrayList = new ArrayList<>();
            int size = this.mItems.size();
            for (int i = 0; i < size; i++) {
                ItemInfo itemInfo = this.mItems.get(i);
                long j = itemInfo.id;
                Uri contentUri = LauncherSettings.Favorites.getContentUri(j);
                arrayList.add(ContentProviderOperation.newUpdate(contentUri).withValues(this.mValues.get(i)).build());
                updateItemArrays(itemInfo, j);
            }
            try {
                ModelWriter.this.mContext.getContentResolver().applyBatch(LauncherProvider.AUTHORITY, arrayList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private abstract class UpdateItemBaseRunnable implements Runnable {
        private final StackTraceElement[] mStackTrace = new Throwable().getStackTrace();
        private final ModelVerifier mVerifier;

        UpdateItemBaseRunnable() {
            this.mVerifier = ModelWriter.this.new ModelVerifier();
        }

        /* JADX WARN: Removed duplicated region for block: B:54:0x0078 A[Catch: all -> 0x00a4, TryCatch #0 {, blocks: (B:37:0x0007, B:39:0x0018, B:41:0x001e, B:43:0x002e, B:44:0x0053, B:46:0x0063, B:48:0x0069, B:50:0x006f, B:52:0x0074, B:58:0x009d, B:59:0x00a2, B:54:0x0078, B:56:0x0086, B:57:0x0092), top: B:64:0x0007 }] */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        protected void updateItemArrays(ItemInfo itemInfo, long j) {
            synchronized (ModelWriter.this.mBgDataModel) {
                ModelWriter.this.checkItemInfoLocked(j, itemInfo, this.mStackTrace);
                if (itemInfo.container != -100 && itemInfo.container != -101 && !ModelWriter.this.mBgDataModel.folders.containsKey(itemInfo.container)) {
                    Log.e(ModelWriter.TAG, "item: " + itemInfo + " container being set to: " + itemInfo.container + ", not in the list of folders");
                }
                ItemInfo itemInfo2 = ModelWriter.this.mBgDataModel.itemsIdMap.get(j);
                if (itemInfo2 == null || (itemInfo2.container != -100 && itemInfo2.container != -101)) {
                    ModelWriter.this.mBgDataModel.workspaceItems.remove(itemInfo2);
                } else {
                    int i = itemInfo2.itemType;
                    if (i != 6) {
                        switch (i) {
                            case 0:
                            case 1:
                            case 2:
                                if (!ModelWriter.this.mBgDataModel.workspaceItems.contains(itemInfo2)) {
                                    ModelWriter.this.mBgDataModel.workspaceItems.add(itemInfo2);
                                    break;
                                }
                                break;
                        }
                    }
                }
                this.mVerifier.verifyModel();
            }
        }
    }

    public class ModelVerifier {
        final int startId;

        ModelVerifier() {
            this.startId = ModelWriter.this.mBgDataModel.lastBindId;
        }

        void verifyModel() {
            if (ModelWriter.this.mVerifyChanges && ModelWriter.this.mModel.getCallback() != null) {
                final int i = ModelWriter.this.mBgDataModel.lastBindId;
                ModelWriter.this.mUiHandler.post(new Runnable() { // from class: com.android.launcher3.model.-$$Lambda$ModelWriter$ModelVerifier$4mPDQaepj-58Crw1v-HLJXgf78A
                    @Override // java.lang.Runnable
                    public final void run() {
                        ModelWriter.ModelVerifier.lambda$verifyModel$0(this.f$0, i);
                    }
                });
            }
        }

        public static /* synthetic */ void lambda$verifyModel$0(ModelVerifier modelVerifier, int i) {
            LauncherModel.Callbacks callback;
            if (ModelWriter.this.mBgDataModel.lastBindId <= i && i != modelVerifier.startId && (callback = ModelWriter.this.mModel.getCallback()) != null) {
                callback.rebindModel();
            }
        }
    }
}
