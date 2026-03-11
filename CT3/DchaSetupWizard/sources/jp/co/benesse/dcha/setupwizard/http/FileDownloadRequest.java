package jp.co.benesse.dcha.setupwizard.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Calendar;
import java.util.Locale;
import jp.co.benesse.dcha.util.Logger;

public class FileDownloadRequest extends Request {
    public static final int BUFFER_SIZE = 2048;
    public static final int PROGRESS_INTERVAL = 10;
    private static final String TAG = FileDownloadRequest.class.getSimpleName();
    public File outPath = null;
    public boolean fileOverwrite = false;

    public FileDownloadRequest() {
        Logger.d(TAG, "FileDownloadRequest 0001");
        this.maxNumRetries = 2;
        this.retryInterval = 5000L;
        Logger.d(TAG, "FileDownloadRequest 0002");
    }

    @Override
    Class<? extends Response> getResponseClass() {
        return FileDownloadResponse.class;
    }

    @Override
    void onSendData(HttpURLConnection conn) throws IOException {
        Logger.d(TAG, "onSendData 0001");
    }

    @Override
    Response onReceiveData(HttpURLConnection conn, Response response) throws Throwable {
        Throwable th;
        int length;
        Logger.d(TAG, "onReceiveData 0001");
        FileDownloadResponse downloadResponse = new FileDownloadResponse(response);
        FileDownloadRequest request = (FileDownloadRequest) response.request;
        if (request.outPath == null) {
            Logger.d(TAG, "onReceiveData 0002");
            throw new IllegalArgumentException("Request.outPath is null");
        }
        File outPath = request.outPath;
        boolean isDirectory = outPath.isDirectory();
        InputStream istream = null;
        FileOutputStream ostream = null;
        if (isDirectory) {
            try {
                try {
                    Logger.d(TAG, "onReceiveData 0003");
                    String filename = request.url.getFile();
                    if (filename != null) {
                        Logger.d(TAG, "onReceiveData 0004");
                        String[] array = filename.split("/");
                        filename = null;
                        if (array.length > 0) {
                            Logger.d(TAG, "onReceiveData 0005");
                            filename = array[array.length - 1];
                        }
                    }
                    if (filename == null || filename.isEmpty()) {
                        Logger.d(TAG, "onReceiveData 0006");
                        filename = String.valueOf(String.valueOf(Calendar.getInstance(Locale.getDefault()).getTimeInMillis())) + ".tmp";
                    }
                    outPath = new File(outPath, filename);
                } catch (IOException e) {
                    e = e;
                    Logger.d(TAG, "onReceiveData 0013");
                    throw e;
                }
            } catch (Throwable th2) {
                th = th2;
                if (ostream != null) {
                    Logger.d(TAG, "onReceiveData 0014");
                    ostream.close();
                }
                if (istream != null) {
                    Logger.d(TAG, "onReceiveData 0015");
                    istream.close();
                }
                throw th;
            }
        }
        if (outPath.exists() && request.fileOverwrite) {
            Logger.d(TAG, "onReceiveData 0007");
            outPath.delete();
        }
        if (!outPath.exists()) {
            Logger.d(TAG, "onReceiveData 0008");
            byte[] buff = new byte[BUFFER_SIZE];
            int count = 0;
            istream = conn.getInputStream();
            FileOutputStream ostream2 = new FileOutputStream(outPath);
            try {
                downloadResponse.receiveLength = 0L;
                while (!request.isCancelled() && (length = istream.read(buff)) != -1) {
                    Logger.d(TAG, "onReceiveData 0009");
                    ostream2.write(buff, 0, length);
                    downloadResponse.receiveLength += (long) length;
                    if (request.responseListener != null && (count = count + 1) > 10) {
                        Logger.d(TAG, "onReceiveData 0010");
                        count = 0;
                        request.responseListener.onHttpProgress(downloadResponse);
                    }
                }
                ostream2.flush();
                if (request.responseListener != null) {
                    Logger.d(TAG, "onReceiveData 0011");
                    request.responseListener.onHttpProgress(downloadResponse);
                }
                ostream = ostream2;
            } catch (IOException e2) {
                e = e2;
                ostream = ostream2;
                Logger.d(TAG, "onReceiveData 0013");
                throw e;
            } catch (Throwable th3) {
                th = th3;
                ostream = ostream2;
                if (ostream != null) {
                }
                if (istream != null) {
                }
                throw th;
            }
        }
        if (isDirectory && request.isCancelled()) {
            Logger.d(TAG, "onReceiveData 0012");
            outPath.delete();
            outPath = null;
        }
        downloadResponse.outFile = outPath;
        if (ostream != null) {
            Logger.d(TAG, "onReceiveData 0014");
            ostream.close();
        }
        if (istream != null) {
            Logger.d(TAG, "onReceiveData 0015");
            istream.close();
        }
        Logger.d(TAG, "onReceiveData 0016");
        return downloadResponse;
    }
}
