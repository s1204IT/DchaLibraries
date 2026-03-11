package jp.co.benesse.dcha.setupwizard.http;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import jp.co.benesse.dcha.util.Logger;

public class FileDownloadRequest extends Request {
    private static final String TAG = FileDownloadRequest.class.getSimpleName();
    public File outPath = null;
    public boolean fileOverwrite = false;

    public FileDownloadRequest() {
        Logger.d(TAG, "FileDownloadRequest 0001");
        this.maxNumRetries = 2;
        this.retryInterval = 500L;
        Logger.d(TAG, "FileDownloadRequest 0002");
    }

    @Override
    Class<? extends Response> getResponseClass() {
        return FileDownloadResponse.class;
    }

    @Override
    void onSendData(HttpURLConnection httpURLConnection) throws IOException {
        Logger.d(TAG, "onSendData 0001");
    }
}
