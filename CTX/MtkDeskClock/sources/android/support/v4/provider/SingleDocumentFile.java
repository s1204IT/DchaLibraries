package android.support.v4.provider;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

@RequiresApi(19)
class SingleDocumentFile extends DocumentFile {
    private Context mContext;
    private Uri mUri;

    SingleDocumentFile(@Nullable DocumentFile parent, Context context, Uri uri) {
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
    @Nullable
    public String getName() {
        return DocumentsContractApi19.getName(this.mContext, this.mUri);
    }

    @Override
    @Nullable
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
    public boolean isVirtual() {
        return DocumentsContractApi19.isVirtual(this.mContext, this.mUri);
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
        try {
            return DocumentsContract.deleteDocument(this.mContext.getContentResolver(), this.mUri);
        } catch (Exception e) {
            return false;
        }
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
