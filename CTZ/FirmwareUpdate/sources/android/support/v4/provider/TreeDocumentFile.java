package android.support.v4.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;
import java.util.ArrayList;

class TreeDocumentFile extends DocumentFile {
    private Context mContext;
    private Uri mUri;

    TreeDocumentFile(DocumentFile parent, Context context, Uri uri) {
        super(parent);
        this.mContext = context;
        this.mUri = uri;
    }

    @Override
    public Uri getUri() {
        return this.mUri;
    }

    @Override
    public String getName() {
        return DocumentsContractApi19.getName(this.mContext, this.mUri);
    }

    @Override
    public long length() {
        return DocumentsContractApi19.length(this.mContext, this.mUri);
    }

    @Override
    public DocumentFile[] listFiles() {
        ContentResolver resolver = this.mContext.getContentResolver();
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(this.mUri, DocumentsContract.getDocumentId(this.mUri));
        ArrayList<Uri> results = new ArrayList<>();
        Cursor c = null;
        int i = 0;
        try {
            try {
                c = resolver.query(childrenUri, new String[]{"document_id"}, null, null, null);
                while (c.moveToNext()) {
                    String documentId = c.getString(0);
                    Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(this.mUri, documentId);
                    results.add(documentUri);
                }
            } catch (Exception e) {
                Log.w("DocumentFile", "Failed query: " + e);
            }
            Uri[] result = (Uri[]) results.toArray(new Uri[results.size()]);
            DocumentFile[] resultFiles = new DocumentFile[result.length];
            while (true) {
                int i2 = i;
                if (i2 < result.length) {
                    resultFiles[i2] = new TreeDocumentFile(this, this.mContext, result[i2]);
                    i = i2 + 1;
                } else {
                    return resultFiles;
                }
            }
        } finally {
            closeQuietly(c);
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception e) {
            }
        }
    }
}
