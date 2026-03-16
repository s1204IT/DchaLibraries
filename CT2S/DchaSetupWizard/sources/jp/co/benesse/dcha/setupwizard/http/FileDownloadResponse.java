package jp.co.benesse.dcha.setupwizard.http;

import java.io.File;
import jp.co.benesse.dcha.util.Logger;

public class FileDownloadResponse extends Response {
    private static final String TAG = FileDownloadResponse.class.getSimpleName();
    public File outFile;

    public FileDownloadResponse() {
        this.outFile = null;
    }

    public FileDownloadResponse(Response response) {
        super(response);
        this.outFile = null;
        Logger.d(TAG, "FileDownloadResponse 0001");
        if (response instanceof FileDownloadResponse) {
            Logger.d(TAG, "FileDownloadResponse 0002");
            this.outFile = ((FileDownloadResponse) response).outFile;
        }
    }
}
