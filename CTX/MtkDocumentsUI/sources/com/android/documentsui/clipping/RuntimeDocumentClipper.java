package com.android.documentsui.clipping;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.PersistableBundle;
import android.provider.DocumentsContract;
import android.util.Log;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.Features;
import com.android.documentsui.selection.Selection;
import com.android.documentsui.services.FileOperation;
import com.android.documentsui.services.FileOperations;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

final class RuntimeDocumentClipper implements DocumentClipper {
    static final boolean $assertionsDisabled = false;
    private final ClipStore mClipStore;
    private final ClipboardManager mClipboard;
    private final Context mContext;

    RuntimeDocumentClipper(Context context, ClipStore clipStore) {
        this.mContext = context;
        this.mClipStore = clipStore;
        this.mClipboard = (ClipboardManager) context.getSystemService(ClipboardManager.class);
    }

    @Override
    public boolean hasItemsToPaste() {
        ClipData primaryClip;
        int itemCount;
        if (this.mClipboard.hasPrimaryClip() && (itemCount = (primaryClip = this.mClipboard.getPrimaryClip()).getItemCount()) > 0) {
            for (int i = 0; i < itemCount; i++) {
                if (isDocumentUri(primaryClip.getItemAt(i).getUri())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isDocumentUri(Uri uri) {
        return uri != null && DocumentsContract.isDocumentUri(this.mContext, uri);
    }

    public ClipData getClipDataForDocuments(Function<String, Uri> function, Selection selection, int i) {
        if (selection.isEmpty()) {
            Log.w("DocumentClipper", "Attempting to clip empty selection. Ignoring.");
            return null;
        }
        ArrayList arrayList = new ArrayList(selection.size());
        Iterator<String> it = selection.iterator();
        while (it.hasNext()) {
            arrayList.add(function.apply(it.next()));
        }
        return getClipDataForDocuments(arrayList, i);
    }

    @Override
    public ClipData getClipDataForDocuments(List<Uri> list, int i, DocumentInfo documentInfo) {
        ClipData clipDataForDocuments = getClipDataForDocuments(list, i);
        clipDataForDocuments.getDescription().getExtras().putString("clipper:srcParent", documentInfo.derivedUri.toString());
        return clipDataForDocuments;
    }

    @Override
    public ClipData getClipDataForDocuments(List<Uri> list, int i) {
        if (list.size() > 500) {
            return createJumboClipData(list, i);
        }
        return createStandardClipData(list, i);
    }

    private ClipData createStandardClipData(List<Uri> list, int i) {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        ArrayList arrayList = new ArrayList();
        HashSet hashSet = new HashSet();
        PersistableBundle persistableBundle = new PersistableBundle();
        persistableBundle.putInt("clipper:opType", i);
        for (Uri uri : list) {
            DocumentInfo.addMimeTypes(contentResolver, uri, hashSet);
            arrayList.add(new ClipData.Item(uri));
        }
        ClipDescription clipDescription = new ClipDescription("", (String[]) hashSet.toArray(new String[0]));
        clipDescription.setExtras(persistableBundle);
        return createClipData(clipDescription, arrayList);
    }

    private ClipData createJumboClipData(List<Uri> list, int i) {
        ArrayList arrayList = new ArrayList(Math.min(list.size(), 500));
        ContentResolver contentResolver = this.mContext.getContentResolver();
        HashSet hashSet = new HashSet();
        int i2 = 0;
        for (Uri uri : list) {
            int i3 = i2 + 1;
            if (i2 < 500) {
                DocumentInfo.addMimeTypes(contentResolver, uri, hashSet);
                arrayList.add(new ClipData.Item(uri));
            }
            i2 = i3;
        }
        PersistableBundle persistableBundle = new PersistableBundle();
        persistableBundle.putInt("clipper:opType", i);
        persistableBundle.putInt("jumboSelection-size", list.size());
        persistableBundle.putInt("jumboSelection-tag", this.mClipStore.persistUris(list));
        ClipDescription clipDescription = new ClipDescription("", (String[]) hashSet.toArray(new String[0]));
        clipDescription.setExtras(persistableBundle);
        return createClipData(clipDescription, arrayList);
    }

    @Override
    public void clipDocumentsForCopy(Function<String, Uri> function, Selection selection) {
        this.mClipboard.setPrimaryClip(getClipDataForDocuments(function, selection, 1));
    }

    @Override
    public void clipDocumentsForCut(Function<String, Uri> function, Selection selection, DocumentInfo documentInfo) {
        ClipData clipDataForDocuments = getClipDataForDocuments(function, selection, 4);
        clipDataForDocuments.getDescription().getExtras().putString("clipper:srcParent", documentInfo.derivedUri.toString());
        this.mClipboard.setPrimaryClip(clipDataForDocuments);
    }

    @Override
    public void copyFromClipboard(DocumentInfo documentInfo, DocumentStack documentStack, FileOperations.Callback callback) {
        copyFromClipData(documentInfo, documentStack, this.mClipboard.getPrimaryClip(), callback);
    }

    @Override
    public void copyFromClipboard(DocumentStack documentStack, FileOperations.Callback callback) {
        copyFromClipData(documentStack, this.mClipboard.getPrimaryClip(), callback);
    }

    public void copyFromClipData(DocumentInfo documentInfo, DocumentStack documentStack, ClipData clipData, FileOperations.Callback callback) {
        copyFromClipData(new DocumentStack(documentStack, documentInfo), clipData, callback);
    }

    @Override
    public void copyFromClipData(DocumentStack documentStack, ClipData clipData, int i, FileOperations.Callback callback) {
        clipData.getDescription().getExtras().putInt("clipper:opType", i);
        copyFromClipData(documentStack, clipData, callback);
    }

    public void copyFromClipData(DocumentStack documentStack, ClipData clipData, FileOperations.Callback callback) {
        if (clipData == null) {
            Log.i("DocumentClipper", "Received null clipData. Ignoring.");
            return;
        }
        PersistableBundle extras = clipData.getDescription().getExtras();
        int opType = getOpType(extras);
        try {
            if (!canCopy(documentStack.peek())) {
                callback.onOperationResult(1, getOpType(clipData), 0);
                return;
            }
            UrisSupplier urisSupplierCreate = UrisSupplier.create(clipData, this.mClipStore);
            if (urisSupplierCreate.getItemCount() == 0) {
                callback.onOperationResult(0, opType, 0);
            } else {
                String string = extras.getString("clipper:srcParent");
                FileOperations.start(this.mContext, new FileOperation.Builder().withOpType(opType).withSrcParent(string == null ? null : Uri.parse(string)).withDestination(documentStack).withSrcs(urisSupplierCreate).build(), callback, FileOperations.createJobId());
            }
        } catch (IOException e) {
            Log.e("DocumentClipper", "Cannot create uris supplier.", e);
            callback.onOperationResult(1, opType, 0);
        }
    }

    private static boolean canCopy(DocumentInfo documentInfo) {
        return documentInfo != null && documentInfo.isDirectory() && documentInfo.isCreateSupported();
    }

    private int getOpType(ClipData clipData) {
        PersistableBundle extras = clipData.getDescription().getExtras();
        if (extras == null) {
            return -1;
        }
        return getOpType(extras);
    }

    private int getOpType(PersistableBundle persistableBundle) {
        if (persistableBundle == null) {
            return -1;
        }
        return persistableBundle.getInt("clipper:opType");
    }

    private static ClipData createClipData(ClipDescription clipDescription, ArrayList<ClipData.Item> arrayList) {
        if (Features.OMC_RUNTIME) {
            return new ClipData(clipDescription, arrayList);
        }
        ClipData clipData = new ClipData(clipDescription, arrayList.get(0));
        for (int i = 1; i < arrayList.size(); i++) {
            clipData.addItem(arrayList.get(i));
        }
        return clipData;
    }
}
