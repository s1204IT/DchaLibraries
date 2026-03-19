package com.android.documentsui.clipping;

import android.content.ClipData;
import android.content.Context;
import android.net.Uri;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.selection.Selection;
import com.android.documentsui.services.FileOperations;
import java.util.List;
import java.util.function.Function;

public interface DocumentClipper {
    void clipDocumentsForCopy(Function<String, Uri> function, Selection selection);

    void clipDocumentsForCut(Function<String, Uri> function, Selection selection, DocumentInfo documentInfo);

    void copyFromClipData(DocumentStack documentStack, ClipData clipData, int i, FileOperations.Callback callback);

    void copyFromClipboard(DocumentInfo documentInfo, DocumentStack documentStack, FileOperations.Callback callback);

    void copyFromClipboard(DocumentStack documentStack, FileOperations.Callback callback);

    ClipData getClipDataForDocuments(List<Uri> list, int i);

    ClipData getClipDataForDocuments(List<Uri> list, int i, DocumentInfo documentInfo);

    boolean hasItemsToPaste();

    static DocumentClipper create(Context context, ClipStore clipStore) {
        return new RuntimeDocumentClipper(context, clipStore);
    }
}
