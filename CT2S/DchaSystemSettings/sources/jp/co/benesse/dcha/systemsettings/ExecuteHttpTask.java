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

    public ExecuteHttpTask(String url, int timeout) {
        Logger.d("ExecuteHttpTask", "ExecuteHttpTask start");
        this.url = url;
        this.timeout = timeout;
        this.countDownLatch = new CountDownLatch(1);
        Logger.d("ExecuteHttpTask", "ExecuteHttpTask end");
    }

    @Override
    public void run() {
        Logger.d("ExecuteHttpTask", "run start");
        HttpURLConnection connection = null;
        if (this.url != null) {
            Logger.d("ExecuteHttpTask", "run 001");
            try {
                connection = (HttpURLConnection) new URL(this.url).openConnection();
            } catch (MalformedURLException e) {
                Logger.d("ExecuteHttpTask", "run 002", e);
            } catch (IOException e2) {
                Logger.d("ExecuteHttpTask", "run 003", e2);
            }
        }
        if (connection != null) {
            Logger.d("ExecuteHttpTask", "run 004");
            try {
                try {
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Connection", "close");
                    connection.setRequestProperty("Content-Type", "text/html; charset=UTF-8");
                    connection.setRequestProperty("Content-Type", "application/octet-stream");
                    connection.setInstanceFollowRedirects(true);
                    connection.setConnectTimeout(this.timeout * 1000);
                    connection.setReadTimeout(this.timeout * 1000);
                    connection.connect();
                    StringBuffer sb = processResponse(connection);
                    Logger.d("ExecuteHttpTask", "run 005");
                    synchronized (this.lock) {
                        Logger.d("ExecuteHttpTask", "run 006");
                        this.httpResponse = new HttpResponse(connection.getResponseCode(), sb);
                    }
                    Logger.d("ExecuteHttpTask", "run 009");
                    connection.disconnect();
                } catch (ProtocolException e3) {
                    Logger.d("ExecuteHttpTask", "run 007", e3);
                    Logger.d("ExecuteHttpTask", "run 009");
                    connection.disconnect();
                } catch (IOException e4) {
                    Logger.d("ExecuteHttpTask", "run 008", e4);
                    Logger.d("ExecuteHttpTask", "run 009");
                    connection.disconnect();
                }
            } catch (Throwable th) {
                Logger.d("ExecuteHttpTask", "run 009");
                connection.disconnect();
                throw th;
            }
        }
        this.countDownLatch.countDown();
        Logger.d("ExecuteHttpTask", "run end");
    }

    private StringBuffer processResponse(HttpURLConnection connection) throws Throwable {
        BufferedReader reader;
        Logger.d("ExecuteHttpTask", "processResponse start");
        StringBuffer sb = new StringBuffer();
        BufferedReader reader2 = null;
        try {
            try {
                Logger.d("ExecuteHttpTask", "processResponse 001");
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
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
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                sb.append(line).append("\n");
            } catch (FileNotFoundException e5) {
                e = e5;
                reader2 = reader;
                Logger.d("ExecuteHttpTask", "processResponse 005", e);
                Logger.d("ExecuteHttpTask", "processResponse 006");
                if (reader2 != null) {
                    Logger.d("ExecuteHttpTask", "processResponse 007");
                    try {
                        reader2.close();
                    } catch (IOException e6) {
                        Logger.d("ExecuteHttpTask", "processResponse 008", e6);
                    }
                    reader2 = null;
                }
            } catch (UnsupportedEncodingException e7) {
                e = e7;
                reader2 = reader;
                Logger.d("ExecuteHttpTask", "processResponse 003", e);
                Logger.d("ExecuteHttpTask", "processResponse 006");
                if (reader2 != null) {
                    Logger.d("ExecuteHttpTask", "processResponse 007");
                    try {
                        reader2.close();
                    } catch (IOException e8) {
                        Logger.d("ExecuteHttpTask", "processResponse 008", e8);
                    }
                    reader2 = null;
                }
            } catch (UnknownHostException e9) {
                e = e9;
                reader2 = reader;
                Logger.d("ExecuteHttpTask", "processResponse 004", e);
                Logger.d("ExecuteHttpTask", "processResponse 006");
                if (reader2 != null) {
                    Logger.d("ExecuteHttpTask", "processResponse 007");
                    try {
                        reader2.close();
                    } catch (IOException e10) {
                        Logger.d("ExecuteHttpTask", "processResponse 008", e10);
                    }
                    reader2 = null;
                }
            } catch (IOException e11) {
                e = e11;
                reader2 = reader;
                Logger.d("ExecuteHttpTask", "processResponse 005", e);
                Logger.d("ExecuteHttpTask", "processResponse 006");
                if (reader2 != null) {
                    Logger.d("ExecuteHttpTask", "processResponse 007");
                    try {
                        reader2.close();
                    } catch (IOException e12) {
                        Logger.d("ExecuteHttpTask", "processResponse 008", e12);
                    }
                    reader2 = null;
                }
            } catch (Throwable th2) {
                th = th2;
                reader2 = reader;
                Logger.d("ExecuteHttpTask", "processResponse 006");
                if (reader2 != null) {
                    Logger.d("ExecuteHttpTask", "processResponse 007");
                    try {
                        reader2.close();
                    } catch (IOException e13) {
                        Logger.d("ExecuteHttpTask", "processResponse 008", e13);
                    }
                }
                throw th;
            }
            Logger.d("ExecuteHttpTask", "processResponse end");
            return sb;
        }
        reader.close();
        reader2 = null;
        Logger.d("ExecuteHttpTask", "processResponse 002");
        Logger.d("ExecuteHttpTask", "processResponse 006");
        if (0 != 0) {
            Logger.d("ExecuteHttpTask", "processResponse 007");
            try {
                reader2.close();
            } catch (IOException e14) {
                Logger.d("ExecuteHttpTask", "processResponse 008", e14);
            }
            reader2 = null;
        }
        Logger.d("ExecuteHttpTask", "processResponse end");
        return sb;
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
        Logger.d("ExecuteHttpTask", "getResponse start");
        HttpResponse returnResponse = null;
        synchronized (this.lock) {
            Logger.d("ExecuteHttpTask", "getResponse 001");
            if (this.httpResponse != null && 200 == this.httpResponse.getStatusCode()) {
                Logger.d("ExecuteHttpTask", "getResponse 002");
                returnResponse = this.httpResponse;
            }
        }
        Logger.d("ExecuteHttpTask", "getResponse end");
        return returnResponse;
    }
}
