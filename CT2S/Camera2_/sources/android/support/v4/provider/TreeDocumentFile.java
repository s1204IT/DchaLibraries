package android.support.v4.provider;

import android.content.Context;
import android.net.Uri;

class TreeDocumentFile extends DocumentFile {
    private Context mContext;
    private Uri mUri;

    TreeDocumentFile(DocumentFile parent, Context context, Uri uri) {
        super(parent);
        this.mContext = context;
        this.mUri = uri;
    }

    @Override
    public DocumentFile createFile(String mimeType, String displayName) {
        Uri result = DocumentsContractApi21.createFile(this.mContext, this.mUri, mimeType, displayName);
        if (result != null) {
            return new TreeDocumentFile(this, this.mContext, result);
        }
        return null;
    }

    @Override
    public DocumentFile createDirectory(String displayName) {
        Uri result = DocumentsContractApi21.createDirectory(this.mContext, this.mUri, displayName);
        if (result != null) {
            return new TreeDocumentFile(this, this.mContext, result);
        }
        return null;
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
    public String getType() {
        return DocumentsContractApi19.getType(this.mContext, this.mUri);
    }

    @Override
    public boolean isDirectory() {
        return DocumentsContractApi19.isDirectory(this.mContext, this.mUri);
    }

    @Override
    public boolean isFile() {
        return DocumentsContractApi19.isFile(this.mContext, this.mUri);
    }

    @Override
    public long lastModified() {
        return DocumentsContractApi19.lastModified(this.mContext, this.mUri);
    }

    @Override
    public long length() {
        return DocumentsContractApi19.length(this.mContext, this.mUri);
    }

    @Override
    public boolean canRead() {
        return DocumentsContractApi19.canRead(this.mContext, this.mUri);
    }

    @Override
    public boolean canWrite() {
        return DocumentsContractApi19.canWrite(this.mContext, this.mUri);
    }

    @Override
    public boolean delete() {
        return DocumentsContractApi19.delete(this.mContext, this.mUri);
    }

    @Override
    public boolean exists() {
        return DocumentsContractApi19.exists(this.mContext, this.mUri);
    }

    @Override
    public DocumentFile[] listFiles() {
        Uri[] result = DocumentsContractApi21.listFiles(this.mContext, this.mUri);
        DocumentFile[] resultFiles = new DocumentFile[result.length];
        for (int i = 0; i < result.length; i++) {
            resultFiles[i] = new TreeDocumentFile(this, this.mContext, result[i]);
        }
        return resultFiles;
    }

    @Override
    public boolean renameTo(String displayName) {
        Uri result = DocumentsContractApi21.renameTo(this.mContext, this.mUri, displayName);
        if (result == null) {
            return false;
        }
        this.mUri = result;
        return true;
    }
}
