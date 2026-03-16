package android.support.v4.provider;

import android.content.Context;
import android.net.Uri;

class SingleDocumentFile extends DocumentFile {
    private Context mContext;
    private Uri mUri;

    SingleDocumentFile(DocumentFile parent, Context context, Uri uri) {
        super(parent);
        this.mContext = context;
        this.mUri = uri;
    }

    @Override
    public DocumentFile createFile(String mimeType, String displayName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentFile createDirectory(String displayName) {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean renameTo(String displayName) {
        throw new UnsupportedOperationException();
    }
}
