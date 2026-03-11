package jp.co.benesse.dcha.setupwizard.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Calendar;
import java.util.Locale;
import jp.co.benesse.dcha.util.Logger;

public class FileDownloadResponse extends Response {
    public static final int BUFFER_SIZE = 4096;
    public static final int PROGRESS_INTERVAL = 10;
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

    @Override
    void onReceiveData(HttpURLConnection httpURLConnection) throws Throwable {
        InputStream inputStream;
        InputStream inputStream2;
        FileOutputStream fileOutputStream;
        InputStream inputStream3;
        FileOutputStream fileOutputStream2;
        int i;
        byte[] bArr;
        Logger.d(TAG, "onReceiveData 0001");
        FileDownloadRequest fileDownloadRequest = (FileDownloadRequest) this.request;
        if (fileDownloadRequest.url == null || fileDownloadRequest.outPath == null) {
            Logger.d(TAG, "onReceiveData 0002");
            throw new IllegalArgumentException("Request.url or Request.outPath is null");
        }
        File file = fileDownloadRequest.outPath;
        boolean zIsDirectory = file.isDirectory();
        try {
            if (zIsDirectory) {
                try {
                    Logger.d(TAG, "onReceiveData 0003");
                    String file2 = fileDownloadRequest.url.getFile();
                    if (file2 != null) {
                        Logger.d(TAG, "onReceiveData 0004");
                        String[] strArrSplit = file2.split("/");
                        if (strArrSplit.length > 0) {
                            Logger.d(TAG, "onReceiveData 0005");
                            file2 = strArrSplit[strArrSplit.length - 1];
                        } else {
                            file2 = null;
                        }
                    }
                    if (file2 == null || file2.isEmpty()) {
                        Logger.d(TAG, "onReceiveData 0006");
                        file2 = String.valueOf(Calendar.getInstance(Locale.getDefault()).getTimeInMillis()) + ".tmp";
                    }
                    file = new File(file, file2);
                } catch (IOException e) {
                    e = e;
                    Logger.d(TAG, "onReceiveData 0011", e);
                    throw e;
                } catch (Throwable th) {
                    th = th;
                    fileOutputStream = null;
                    inputStream2 = null;
                    if (fileOutputStream != null) {
                    }
                    if (inputStream2 != null) {
                    }
                    throw th;
                }
            }
            if (file.exists() && fileDownloadRequest.fileOverwrite) {
                Logger.d(TAG, "onReceiveData 0007");
                deleteFile(file);
            }
            if (file.exists()) {
                inputStream3 = null;
                fileOutputStream2 = null;
            } else {
                Logger.d(TAG, "onReceiveData 0008");
                byte[] bArr2 = new byte[BUFFER_SIZE];
                inputStream2 = httpURLConnection.getInputStream();
                try {
                    fileOutputStream2 = new FileOutputStream(file);
                } catch (IOException e2) {
                    e = e2;
                    Logger.d(TAG, "onReceiveData 0011", e);
                    throw e;
                } catch (Throwable th2) {
                    th = th2;
                    fileOutputStream = null;
                    if (fileOutputStream != null) {
                        Logger.d(TAG, "onReceiveData 0012");
                        fileOutputStream.close();
                    }
                    if (inputStream2 != null) {
                        Logger.d(TAG, "onReceiveData 0013");
                        inputStream2.close();
                    }
                    throw th;
                }
                try {
                    this.receiveLength = 0L;
                    loop0: while (true) {
                        int i2 = 0;
                        while (!fileDownloadRequest.isCancelled() && (i = inputStream2.read(bArr2)) != -1) {
                            fileOutputStream2.write(bArr2, 0, i);
                            bArr = bArr2;
                            this.receiveLength += (long) i;
                            if (fileDownloadRequest.responseListener == null || (i2 = i2 + 1) <= 10) {
                                bArr2 = bArr;
                            }
                        }
                        fileDownloadRequest.responseListener.onHttpProgress(this);
                        bArr2 = bArr;
                    }
                    fileOutputStream2.flush();
                    if (fileDownloadRequest.responseListener != null) {
                        Logger.d(TAG, "onReceiveData 0009");
                        fileDownloadRequest.responseListener.onHttpProgress(this);
                    }
                    inputStream3 = inputStream2;
                } catch (IOException e3) {
                    e = e3;
                    Logger.d(TAG, "onReceiveData 0011", e);
                    throw e;
                } catch (Throwable th3) {
                    th = th3;
                    fileOutputStream = fileOutputStream2;
                    if (fileOutputStream != null) {
                    }
                    if (inputStream2 != null) {
                    }
                    throw th;
                }
            }
            if (zIsDirectory) {
                try {
                    if (fileDownloadRequest.isCancelled()) {
                        Logger.d(TAG, "onReceiveData 0010");
                        deleteFile(file);
                        file = null;
                    }
                } catch (IOException e4) {
                    e = e4;
                    Logger.d(TAG, "onReceiveData 0011", e);
                    throw e;
                }
            }
            this.outFile = file;
            if (fileOutputStream2 != null) {
                Logger.d(TAG, "onReceiveData 0012");
                fileOutputStream2.close();
            }
            if (inputStream3 != null) {
                Logger.d(TAG, "onReceiveData 0013");
                inputStream3.close();
            }
            Logger.d(TAG, "onReceiveData 0014");
        } catch (Throwable th4) {
            th = th4;
            inputStream2 = inputStream;
        }
    }

    protected boolean canWriteFile(File file) {
        if (file == null) {
            return false;
        }
        if (file.exists()) {
            return file.isFile() && file.canWrite();
        }
        return true;
    }

    protected boolean deleteFile(File file) {
        if (canWriteFile(file)) {
            return file.delete();
        }
        return false;
    }
}
