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

    @Override
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
                httpURLConnection = null;
            } catch (IOException e2) {
                Logger.d("ExecuteHttpTask", "run 003", e2);
                httpURLConnection = null;
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
                } catch (Throwable th) {
                    Logger.d("ExecuteHttpTask", "run 009");
                    httpURLConnection.disconnect();
                    throw th;
                }
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
        }
        this.countDownLatch.countDown();
        Logger.d("ExecuteHttpTask", "run end");
    }

    private StringBuffer processResponse(HttpURLConnection httpURLConnection) throws Throwable {
        String str;
        Object[] objArr;
        BufferedReader bufferedReader;
        Logger.d("ExecuteHttpTask", "processResponse start");
        StringBuffer stringBuffer = new StringBuffer();
        ?? r2 = 0;
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
                bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), "UTF-8"));
            } catch (Throwable th) {
                th = th;
            }
        } catch (FileNotFoundException e) {
            e = e;
        } catch (UnsupportedEncodingException e2) {
            e = e2;
        } catch (UnknownHostException e3) {
            e = e3;
        } catch (IOException e4) {
            e = e4;
        }
        while (true) {
            try {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                stringBuffer.append(line);
                stringBuffer.append("\n");
            } catch (FileNotFoundException e5) {
                e = e5;
                bufferedReader2 = bufferedReader;
                Logger.d("ExecuteHttpTask", "processResponse 005", e);
                Logger.d("ExecuteHttpTask", "processResponse 006");
                r2 = bufferedReader2;
                if (bufferedReader2 != null) {
                    Logger.d("ExecuteHttpTask", "processResponse 007");
                    try {
                        bufferedReader2.close();
                        r2 = bufferedReader2;
                    } catch (IOException e6) {
                        str = "ExecuteHttpTask";
                        objArr = new Object[]{"processResponse 008", e6};
                        Logger.d(str, objArr);
                    }
                }
            } catch (UnsupportedEncodingException e7) {
                e = e7;
                bufferedReader3 = bufferedReader;
                Logger.d("ExecuteHttpTask", "processResponse 003", e);
                Logger.d("ExecuteHttpTask", "processResponse 006");
                r2 = bufferedReader3;
                if (bufferedReader3 != null) {
                    Logger.d("ExecuteHttpTask", "processResponse 007");
                    try {
                        bufferedReader3.close();
                        r2 = bufferedReader3;
                    } catch (IOException e8) {
                        str = "ExecuteHttpTask";
                        objArr = new Object[]{"processResponse 008", e8};
                        Logger.d(str, objArr);
                    }
                }
            } catch (UnknownHostException e9) {
                e = e9;
                bufferedReader4 = bufferedReader;
                Logger.d("ExecuteHttpTask", "processResponse 004", e);
                Logger.d("ExecuteHttpTask", "processResponse 006");
                r2 = bufferedReader4;
                if (bufferedReader4 != null) {
                    Logger.d("ExecuteHttpTask", "processResponse 007");
                    try {
                        bufferedReader4.close();
                        r2 = bufferedReader4;
                    } catch (IOException e10) {
                        str = "ExecuteHttpTask";
                        objArr = new Object[]{"processResponse 008", e10};
                        Logger.d(str, objArr);
                    }
                }
            } catch (IOException e11) {
                e = e11;
                bufferedReader5 = bufferedReader;
                Logger.d("ExecuteHttpTask", "processResponse 005", e);
                Logger.d("ExecuteHttpTask", "processResponse 006");
                r2 = bufferedReader5;
                if (bufferedReader5 != null) {
                    Logger.d("ExecuteHttpTask", "processResponse 007");
                    try {
                        bufferedReader5.close();
                        r2 = bufferedReader5;
                    } catch (IOException e12) {
                        str = "ExecuteHttpTask";
                        objArr = new Object[]{"processResponse 008", e12};
                        Logger.d(str, objArr);
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                r2 = bufferedReader;
                Logger.d("ExecuteHttpTask", "processResponse 006");
                if (r2 != 0) {
                    Logger.d("ExecuteHttpTask", "processResponse 007");
                    try {
                        r2.close();
                    } catch (IOException e13) {
                        Object[] objArr2 = new Object[i];
                        objArr2[0] = "processResponse 008";
                        objArr2[1] = e13;
                        Logger.d("ExecuteHttpTask", objArr2);
                    }
                }
                throw th;
            }
            Logger.d("ExecuteHttpTask", "processResponse end");
            return stringBuffer;
        }
        bufferedReader.close();
        Logger.d("ExecuteHttpTask", "processResponse 002");
        Object[] objArr3 = {"processResponse 006"};
        Logger.d("ExecuteHttpTask", objArr3);
        r2 = objArr3;
        i = "processResponse 006";
        Logger.d("ExecuteHttpTask", "processResponse end");
        return stringBuffer;
    }

    public void execute() {
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
