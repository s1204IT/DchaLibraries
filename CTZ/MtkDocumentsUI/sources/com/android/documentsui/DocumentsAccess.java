package com.android.documentsui;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.DocumentsContract;
import android.util.Log;
import com.android.documentsui.archives.ArchivesProvider;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.RootInfo;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public interface DocumentsAccess {
    Uri createDocument(DocumentInfo documentInfo, String str, String str2);

    DocumentsContract.Path findDocumentPath(Uri uri) throws RemoteException;

    DocumentInfo getArchiveDocument(Uri uri);

    List<DocumentInfo> getDocuments(String str, List<String> list) throws RemoteException;

    DocumentInfo getRootDocument(RootInfo rootInfo);

    boolean isDocumentUri(Uri uri);

    static DocumentsAccess create(Context context) {
        return new RuntimeDocumentAccess(context);
    }

    public static final class RuntimeDocumentAccess implements DocumentsAccess {
        private final Context mContext;

        private RuntimeDocumentAccess(Context context) {
            this.mContext = context;
        }

        @Override
        public DocumentInfo getRootDocument(RootInfo rootInfo) {
            return getDocument(DocumentsContract.buildDocumentUri(rootInfo.authority, rootInfo.documentId));
        }

        public DocumentInfo getDocument(Uri uri) {
            try {
                return DocumentInfo.fromUri(this.mContext.getContentResolver(), uri);
            } catch (FileNotFoundException e) {
                Log.w("DocumentAccess", "Couldn't create DocumentInfo for uri: " + uri);
                return null;
            }
        }

        @Override
        public List<DocumentInfo> getDocuments(String str, List<String> list) throws Exception {
            Throwable th;
            Throwable th2;
            ContentProviderClient contentProviderClientAcquireUnstableProviderOrThrow = DocumentsApplication.acquireUnstableProviderOrThrow(this.mContext.getContentResolver(), str);
            try {
                ArrayList arrayList = new ArrayList(list.size());
                Iterator<String> it = list.iterator();
                while (it.hasNext()) {
                    Uri uriBuildDocumentUri = DocumentsContract.buildDocumentUri(str, it.next());
                    Cursor cursorQuery = contentProviderClientAcquireUnstableProviderOrThrow.query(uriBuildDocumentUri, null, null, null, null);
                    try {
                        if (!cursorQuery.moveToNext()) {
                            Log.e("DocumentAccess", "Couldn't create DocumentInfo for Uri: " + uriBuildDocumentUri);
                            throw new RemoteException("Failed to move cursor.");
                        }
                        arrayList.add(DocumentInfo.fromCursor(cursorQuery, str));
                        if (cursorQuery != null) {
                            $closeResource(null, cursorQuery);
                        }
                    } catch (Throwable th3) {
                        try {
                            throw th3;
                        } catch (Throwable th4) {
                            th = th3;
                            th2 = th4;
                            if (cursorQuery != null) {
                                throw th2;
                            }
                            $closeResource(th, cursorQuery);
                            throw th2;
                        }
                    }
                }
                return arrayList;
            } finally {
                if (contentProviderClientAcquireUnstableProviderOrThrow != null) {
                    $closeResource(null, contentProviderClientAcquireUnstableProviderOrThrow);
                }
            }
        }

        private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
            if (th == null) {
                autoCloseable.close();
                return;
            }
            try {
                autoCloseable.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
        }

        @Override
        public DocumentInfo getArchiveDocument(Uri uri) {
            return getDocument(ArchivesProvider.buildUriForArchive(uri, 268435456));
        }

        @Override
        public boolean isDocumentUri(Uri uri) {
            return DocumentsContract.isDocumentUri(this.mContext, uri);
        }

        @Override
        public DocumentsContract.Path findDocumentPath(Uri uri) throws Exception {
            ContentProviderClient contentProviderClientAcquireUnstableProviderOrThrow = DocumentsApplication.acquireUnstableProviderOrThrow(this.mContext.getContentResolver(), uri.getAuthority());
            Throwable th = null;
            try {
                return DocumentsContract.findDocumentPath(contentProviderClientAcquireUnstableProviderOrThrow, uri);
            } finally {
                if (contentProviderClientAcquireUnstableProviderOrThrow != null) {
                    $closeResource(th, contentProviderClientAcquireUnstableProviderOrThrow);
                }
            }
        }

        @Override
        public Uri createDocument(DocumentInfo documentInfo, String str, String str2) throws Throwable {
            Throwable th;
            try {
                ContentProviderClient contentProviderClientAcquireUnstableProviderOrThrow = DocumentsApplication.acquireUnstableProviderOrThrow(this.mContext.getContentResolver(), documentInfo.derivedUri.getAuthority());
                try {
                    Uri uriCreateDocument = DocumentsContract.createDocument(contentProviderClientAcquireUnstableProviderOrThrow, documentInfo.derivedUri, str, str2);
                    if (contentProviderClientAcquireUnstableProviderOrThrow != null) {
                        $closeResource(null, contentProviderClientAcquireUnstableProviderOrThrow);
                    }
                    return uriCreateDocument;
                } catch (Throwable th2) {
                    th = th2;
                    th = null;
                    if (contentProviderClientAcquireUnstableProviderOrThrow != null) {
                    }
                }
            } catch (Exception e) {
                Log.w("DocumentAccess", "Failed to create document", e);
                return null;
            }
        }
    }
}
