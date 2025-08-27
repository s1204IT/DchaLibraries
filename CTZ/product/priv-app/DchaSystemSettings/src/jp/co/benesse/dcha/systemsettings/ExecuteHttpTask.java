package jp.co.benesse.dcha.systemsettings;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import jp.co.benesse.dcha.util.Logger;

/* loaded from: classes.dex */
public class ExecuteHttpTask extends Thread {
    protected CountDownLatch countDownLatch;
    private HttpResponse httpResponse = null;
    private final Object lock = new Object();
    private final int timeout;
    private final String url;

    public ExecuteHttpTask(String str, int i) {
        Logger.d("ExecuteHttpTask", "ExecuteHttpTask start");
        this.url = str;
        this.timeout = i;
        this.countDownLatch = new CountDownLatch(1);
        Logger.d("ExecuteHttpTask", "ExecuteHttpTask end");
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [153=5] */
    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        HttpURLConnection httpURLConnection;
        String str;
        Object[] objArr;
        Logger.d("ExecuteHttpTask", "run start");
        if (this.url != null) {
            Logger.d("ExecuteHttpTask", "run 001");
            try {
                httpURLConnection = (HttpURLConnection) new URL(this.url).openConnection();
            } catch (MalformedURLException e) {
                Logger.d("ExecuteHttpTask", "run 002", e);
            } catch (IOException e2) {
                Logger.d("ExecuteHttpTask", "run 003", e2);
            }
        } else {
            httpURLConnection = null;
        }
        if (httpURLConnection != null) {
            Logger.d("ExecuteHttpTask", "run 004");
            try {
                try {
                    httpURLConnection.setRequestMethod("GET");
                    httpURLConnection.setRequestProperty("Connection", "close");
                    httpURLConnection.setRequestProperty("Content-Type", "text/html; charset=UTF-8");
                    httpURLConnection.setRequestProperty("Content-Type", "application/octet-stream");
                    httpURLConnection.setInstanceFollowRedirects(true);
                    httpURLConnection.setConnectTimeout(this.timeout * 1000);
                    httpURLConnection.setReadTimeout(this.timeout * 1000);
                    httpURLConnection.connect();
                    StringBuffer stringBufferProcessResponse = processResponse(httpURLConnection);
                    Logger.d("ExecuteHttpTask", "run 005");
                    synchronized (this.lock) {
                        Logger.d("ExecuteHttpTask", "run 006");
                        this.httpResponse = new HttpResponse(httpURLConnection.getResponseCode(), stringBufferProcessResponse);
                    }
                    str = "ExecuteHttpTask";
                    objArr = new Object[]{"run 009"};
                } catch (ProtocolException e3) {
                    Logger.d("ExecuteHttpTask", "run 007", e3);
                    str = "ExecuteHttpTask";
                    objArr = new Object[]{"run 009"};
                } catch (IOException e4) {
                    Logger.d("ExecuteHttpTask", "run 008", e4);
                    str = "ExecuteHttpTask";
                    objArr = new Object[]{"run 009"};
                }
                Logger.d(str, objArr);
                httpURLConnection.disconnect();
            } catch (Throwable th) {
                Logger.d("ExecuteHttpTask", "run 009");
                httpURLConnection.disconnect();
                throw th;
            }
        }
        this.countDownLatch.countDown();
        Logger.d("ExecuteHttpTask", "run end");
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [195=8, 196=6, 197=5, 199=5, 200=5, 201=5] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:20:0x006e */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:22:0x0071 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:31:0x00a9 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:40:0x00df */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:49:0x0115 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:75:0x0016 */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v21, types: [java.lang.Object[]] */
    /* JADX WARN: Type inference failed for: r3v11, types: [java.lang.String] */
    private StringBuffer processResponse(HttpURLConnection httpURLConnection) throws Throwable {
        String str;
        Object[] objArr;
        Logger.d("ExecuteHttpTask", "processResponse start");
        StringBuffer stringBuffer = new StringBuffer();
        BufferedReader bufferedReader = null;
        BufferedReader bufferedReader2 = null;
        BufferedReader bufferedReader3 = null;
        BufferedReader bufferedReader4 = null;
        BufferedReader bufferedReader5 = null;
        int i = 2;
        i = 2;
        i = 2;
        i = 2;
        i = 2;
        i = 2;
        i = 2;
        i = 2;
        i = 2;
        i = 2;
        try {
            try {
                Logger.d("ExecuteHttpTask", "processResponse 001");
                BufferedReader bufferedReader6 = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), "UTF-8"));
                while (true) {
                    try {
                        String line = bufferedReader6.readLine();
                        if (line == null) {
                            break;
                        }
                        stringBuffer.append(line);
                        stringBuffer.append("\n");
                    } catch (FileNotFoundException e) {
                        e = e;
                        bufferedReader2 = bufferedReader6;
                        Logger.d("ExecuteHttpTask", "processResponse 005", e);
                        Logger.d("ExecuteHttpTask", "processResponse 006");
                        bufferedReader = bufferedReader2;
                        if (bufferedReader2 != null) {
                            Logger.d("ExecuteHttpTask", "processResponse 007");
                            try {
                                bufferedReader2.close();
                                bufferedReader = bufferedReader2;
                            } catch (IOException e2) {
                                str = "ExecuteHttpTask";
                                objArr = new Object[]{"processResponse 008", e2};
                                Logger.d(str, objArr);
                                Logger.d("ExecuteHttpTask", "processResponse end");
                                return stringBuffer;
                            }
                        }
                        Logger.d("ExecuteHttpTask", "processResponse end");
                        return stringBuffer;
                    } catch (UnsupportedEncodingException e3) {
                        e = e3;
                        bufferedReader3 = bufferedReader6;
                        Logger.d("ExecuteHttpTask", "processResponse 003", e);
                        Logger.d("ExecuteHttpTask", "processResponse 006");
                        bufferedReader = bufferedReader3;
                        if (bufferedReader3 != null) {
                            Logger.d("ExecuteHttpTask", "processResponse 007");
                            try {
                                bufferedReader3.close();
                                bufferedReader = bufferedReader3;
                            } catch (IOException e4) {
                                str = "ExecuteHttpTask";
                                objArr = new Object[]{"processResponse 008", e4};
                                Logger.d(str, objArr);
                                Logger.d("ExecuteHttpTask", "processResponse end");
                                return stringBuffer;
                            }
                        }
                        Logger.d("ExecuteHttpTask", "processResponse end");
                        return stringBuffer;
                    } catch (UnknownHostException e5) {
                        e = e5;
                        bufferedReader4 = bufferedReader6;
                        Logger.d("ExecuteHttpTask", "processResponse 004", e);
                        Logger.d("ExecuteHttpTask", "processResponse 006");
                        bufferedReader = bufferedReader4;
                        if (bufferedReader4 != null) {
                            Logger.d("ExecuteHttpTask", "processResponse 007");
                            try {
                                bufferedReader4.close();
                                bufferedReader = bufferedReader4;
                            } catch (IOException e6) {
                                str = "ExecuteHttpTask";
                                objArr = new Object[]{"processResponse 008", e6};
                                Logger.d(str, objArr);
                                Logger.d("ExecuteHttpTask", "processResponse end");
                                return stringBuffer;
                            }
                        }
                        Logger.d("ExecuteHttpTask", "processResponse end");
                        return stringBuffer;
                    } catch (IOException e7) {
                        e = e7;
                        bufferedReader5 = bufferedReader6;
                        Logger.d("ExecuteHttpTask", "processResponse 005", e);
                        Logger.d("ExecuteHttpTask", "processResponse 006");
                        bufferedReader = bufferedReader5;
                        if (bufferedReader5 != null) {
                            Logger.d("ExecuteHttpTask", "processResponse 007");
                            try {
                                bufferedReader5.close();
                                bufferedReader = bufferedReader5;
                            } catch (IOException e8) {
                                str = "ExecuteHttpTask";
                                objArr = new Object[]{"processResponse 008", e8};
                                Logger.d(str, objArr);
                                Logger.d("ExecuteHttpTask", "processResponse end");
                                return stringBuffer;
                            }
                        }
                        Logger.d("ExecuteHttpTask", "processResponse end");
                        return stringBuffer;
                    } catch (Throwable th) {
                        th = th;
                        bufferedReader = bufferedReader6;
                        Logger.d("ExecuteHttpTask", "processResponse 006");
                        if (bufferedReader != null) {
                            Logger.d("ExecuteHttpTask", "processResponse 007");
                            try {
                                bufferedReader.close();
                            } catch (IOException e9) {
                                Object[] objArr2 = new Object[i];
                                objArr2[0] = "processResponse 008";
                                objArr2[1] = e9;
                                Logger.d("ExecuteHttpTask", objArr2);
                            }
                        }
                        throw th;
                    }
                }
                bufferedReader6.close();
                Logger.d("ExecuteHttpTask", "processResponse 002");
                ?? r2 = {"processResponse 006"};
                Logger.d("ExecuteHttpTask", r2);
                bufferedReader = r2;
                i = "processResponse 006";
            } catch (FileNotFoundException e10) {
                e = e10;
            } catch (UnsupportedEncodingException e11) {
                e = e11;
            } catch (UnknownHostException e12) {
                e = e12;
            } catch (IOException e13) {
                e = e13;
            }
            Logger.d("ExecuteHttpTask", "processResponse end");
            return stringBuffer;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public void execute() throws InterruptedException {
        Logger.d("ExecuteHttpTask", "execute start");
        start();
        try {
            Logger.d("ExecuteHttpTask", "execute 001");
            this.countDownLatch.await(this.timeout, TimeUnit.SECONDS);
            Logger.d("ExecuteHttpTask", "execute 002");
        } catch (InterruptedException e) {
            Logger.d("ExecuteHttpTask", "execute 003", e);
        }
        Logger.d("ExecuteHttpTask", "execute end");
    }

    public HttpResponse getResponse() {
        HttpResponse httpResponse;
        Logger.d("ExecuteHttpTask", "getResponse start");
        synchronized (this.lock) {
            Logger.d("ExecuteHttpTask", "getResponse 001");
            if (this.httpResponse != null && 200 == this.httpResponse.getStatusCode()) {
                Logger.d("ExecuteHttpTask", "getResponse 002");
                httpResponse = this.httpResponse;
            } else {
                httpResponse = null;
            }
        }
        Logger.d("ExecuteHttpTask", "getResponse end");
        return httpResponse;
    }
}
