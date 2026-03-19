package com.android.shell;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import com.android.internal.content.FileSystemProvider;
import java.io.File;
import java.io.FileNotFoundException;

public class BugreportStorageProvider extends FileSystemProvider {
    private File mRoot;
    private static final String[] DEFAULT_ROOT_PROJECTION = {"root_id", "flags", "icon", "title", "document_id"};
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = {"document_id", "mime_type", "_display_name", "last_modified", "flags", "_size"};

    public boolean onCreate() {
        super.onCreate(DEFAULT_DOCUMENT_PROJECTION);
        this.mRoot = new File(getContext().getFilesDir(), "bugreports");
        return true;
    }

    public Cursor queryRoots(String[] strArr) throws FileNotFoundException {
        MatrixCursor matrixCursor = new MatrixCursor(resolveRootProjection(strArr));
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("root_id", "bugreport");
        rowBuilderNewRow.add("flags", 2);
        rowBuilderNewRow.add("icon", Integer.valueOf(android.R.mipmap.sym_def_app_icon));
        rowBuilderNewRow.add("title", getContext().getString(R.string.bugreport_storage_title));
        rowBuilderNewRow.add("document_id", "bugreport");
        return matrixCursor;
    }

    public Cursor queryDocument(String str, String[] strArr) throws FileNotFoundException {
        if ("bugreport".equals(str)) {
            MatrixCursor matrixCursor = new MatrixCursor(resolveDocumentProjection(strArr));
            includeDefaultDocument(matrixCursor);
            return matrixCursor;
        }
        return super.queryDocument(str, strArr);
    }

    public ParcelFileDescriptor openDocument(String str, String str2, CancellationSignal cancellationSignal) throws FileNotFoundException {
        if (ParcelFileDescriptor.parseMode(str2) != 268435456) {
            throw new FileNotFoundException("Failed to open: " + str + ", mode = " + str2);
        }
        return ParcelFileDescriptor.open(getFileForDocId(str), 268435456);
    }

    protected Uri buildNotificationUri(String str) {
        return DocumentsContract.buildChildDocumentsUri("com.android.shell.documents", str);
    }

    private static String[] resolveRootProjection(String[] strArr) {
        return strArr != null ? strArr : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] strArr) {
        return strArr != null ? strArr : DEFAULT_DOCUMENT_PROJECTION;
    }

    protected String getDocIdForFile(File file) {
        return "bugreport:" + file.getName();
    }

    protected File getFileForDocId(String str, boolean z) throws FileNotFoundException {
        if ("bugreport".equals(str)) {
            return this.mRoot;
        }
        int iIndexOf = str.indexOf(58, 1);
        String strSubstring = str.substring(iIndexOf + 1);
        if (iIndexOf == -1 || !"bugreport".equals(str.substring(0, iIndexOf)) || !FileUtils.isValidExtFilename(strSubstring)) {
            throw new FileNotFoundException("Invalid document ID: " + str);
        }
        File file = new File(this.mRoot, strSubstring);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + str);
        }
        return file;
    }

    protected MatrixCursor.RowBuilder includeFile(MatrixCursor matrixCursor, String str, File file) throws FileNotFoundException {
        MatrixCursor.RowBuilder rowBuilderIncludeFile = super.includeFile(matrixCursor, str, file);
        rowBuilderIncludeFile.add("flags", 4);
        return rowBuilderIncludeFile;
    }

    private void includeDefaultDocument(MatrixCursor matrixCursor) {
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("document_id", "bugreport");
        rowBuilderNewRow.add("mime_type", "vnd.android.document/directory");
        rowBuilderNewRow.add("_display_name", this.mRoot.getName());
        rowBuilderNewRow.add("last_modified", Long.valueOf(this.mRoot.lastModified()));
        rowBuilderNewRow.add("flags", 32);
    }
}
