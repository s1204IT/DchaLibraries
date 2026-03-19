package com.android.documentsui.files;

import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.SystemProperties;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.util.Log;
import android.util.Range;
import com.android.documentsui.Model;
import com.android.documentsui.R;
import com.android.documentsui.base.DebugFlags;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.SharedMinimal;
import java.util.ArrayList;
import java.util.List;

public final class QuickViewIntentBuilder {
    static final boolean $assertionsDisabled = false;
    private final DocumentInfo mDocument;
    private final Model mModel;
    private final PackageManager mPackageMgr;
    private final Resources mResources;
    private static final String[] IN_ARCHIVE_FEATURES = new String[0];
    private static final String[] FULL_FEATURES = {"android:view", "android:edit", "android:delete", "android:send", "android:download", "android:print"};

    public QuickViewIntentBuilder(PackageManager packageManager, Resources resources, DocumentInfo documentInfo, Model model) {
        this.mPackageMgr = packageManager;
        this.mResources = resources;
        this.mDocument = documentInfo;
        this.mModel = model;
    }

    Intent build() {
        if (SharedMinimal.DEBUG) {
            Log.d("QuickViewIntentBuilder", "Preparing intent for doc:" + this.mDocument.documentId);
        }
        String quickViewPackage = getQuickViewPackage();
        ClipData clipData = null;
        if (!TextUtils.isEmpty(quickViewPackage)) {
            Intent intent = new Intent("android.intent.action.QUICK_VIEW");
            intent.setDataAndType(this.mDocument.derivedUri, this.mDocument.mimeType);
            intent.setFlags(3);
            intent.setPackage(quickViewPackage);
            if (hasRegisteredHandler(intent)) {
                includeQuickViewFeaturesFlag(intent, this.mDocument);
                ArrayList<Uri> arrayList = new ArrayList<>();
                int iCollectViewableUris = collectViewableUris(arrayList);
                Range<Integer> rangeComputeSiblingsRange = computeSiblingsRange(arrayList, iCollectViewableUris);
                for (int iIntValue = ((Integer) rangeComputeSiblingsRange.getLower()).intValue(); iIntValue <= ((Integer) rangeComputeSiblingsRange.getUpper()).intValue(); iIntValue++) {
                    Uri uri = arrayList.get(iIntValue);
                    ClipData.Item item = new ClipData.Item(uri);
                    if (SharedMinimal.DEBUG) {
                        Log.d("QuickViewIntentBuilder", "Including file: " + uri);
                    }
                    if (clipData == null) {
                        clipData = new ClipData("URIs", new String[]{"text/uri-list"}, item);
                    } else {
                        clipData.addItem(item);
                    }
                }
                intent.putExtra("android.intent.extra.INDEX", iCollectViewableUris - ((Integer) rangeComputeSiblingsRange.getLower()).intValue());
                intent.setClipData(clipData);
                return intent;
            }
            Log.e("QuickViewIntentBuilder", "Can't resolve trusted quick view package: " + quickViewPackage);
        }
        return null;
    }

    private String getQuickViewPackage() {
        String string = this.mResources.getString(R.string.trusted_quick_viewer_package);
        if ("*disabled*".equals(string)) {
            return "";
        }
        if (Build.IS_DEBUGGABLE) {
            String quickViewer = DebugFlags.getQuickViewer();
            if (quickViewer != null) {
                return quickViewer;
            }
            return SystemProperties.get("debug.quick_viewer", string);
        }
        return string;
    }

    private int collectViewableUris(ArrayList<Uri> arrayList) {
        String[] modelIds = this.mModel.getModelIds();
        arrayList.ensureCapacity(modelIds.length);
        int size = 0;
        for (int i = 0; i < modelIds.length; i++) {
            Cursor item = this.mModel.getItem(modelIds[i]);
            if (item == null) {
                if (SharedMinimal.DEBUG) {
                    Log.d("QuickViewIntentBuilder", "Unable to obtain cursor for sibling document, modelId: " + modelIds[i]);
                }
            } else if ("vnd.android.document/directory".equals(DocumentInfo.getCursorString(item, "mime_type"))) {
                if (SharedMinimal.DEBUG) {
                    Log.d("QuickViewIntentBuilder", "Skipping directory, not supported by quick view. modelId: " + modelIds[i]);
                }
            } else {
                String cursorString = DocumentInfo.getCursorString(item, "document_id");
                arrayList.add(DocumentsContract.buildDocumentUri(DocumentInfo.getCursorString(item, "android:authority"), cursorString));
                if (cursorString.equals(this.mDocument.documentId)) {
                    size = arrayList.size() - 1;
                    if (SharedMinimal.DEBUG) {
                        Log.d("QuickViewIntentBuilder", "Found starting point for QV. " + size);
                    }
                }
            }
        }
        return size;
    }

    private boolean hasRegisteredHandler(Intent intent) {
        return intent.resolveActivity(this.mPackageMgr) != null;
    }

    private static void includeQuickViewFeaturesFlag(Intent intent, DocumentInfo documentInfo) {
        intent.putExtra("android.intent.extra.QUICK_VIEW_FEATURES", documentInfo.isInArchive() ? IN_ARCHIVE_FEATURES : FULL_FEATURES);
    }

    private static Range<Integer> computeSiblingsRange(List<Uri> list, int i) {
        int iMin;
        int iMax;
        if (i < list.size() / 2) {
            iMax = Math.max(0, i - 250);
            iMin = Math.min(list.size() - 1, (iMax + 500) - 1);
        } else {
            iMin = Math.min(list.size() - 1, i + 250);
            iMax = Math.max(0, (iMin - 500) + 1);
        }
        if (SharedMinimal.DEBUG) {
            Log.d("QuickViewIntentBuilder", "Copmuted siblings from index: " + iMax + " to: " + iMin);
        }
        return new Range<>(Integer.valueOf(iMax), Integer.valueOf(iMin));
    }
}
