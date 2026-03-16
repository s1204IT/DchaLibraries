package org.ksoap2.transport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.List;
import java.util.zip.GZIPInputStream;
import org.ksoap2.HeaderProperty;
import org.ksoap2.SoapEnvelope;
import org.xmlpull.v1.XmlPullParserException;

public class HttpTransportSE extends Transport {
    private ServiceConnection serviceConnection;

    public HttpTransportSE(String url) {
        super((Proxy) null, url);
    }

    public HttpTransportSE(Proxy proxy, String url) {
        super(proxy, url);
    }

    public HttpTransportSE(String url, int timeout) {
        super(url, timeout);
    }

    public HttpTransportSE(Proxy proxy, String url, int timeout) {
        super(proxy, url, timeout);
    }

    public HttpTransportSE(String url, int timeout, int contentLength) {
        super(url, timeout);
    }

    public HttpTransportSE(Proxy proxy, String url, int timeout, int contentLength) {
        super(proxy, url, timeout);
    }

    @Override
    public void call(String soapAction, SoapEnvelope envelope) throws XmlPullParserException, IOException {
        call(soapAction, envelope, null);
    }

    @Override
    public List call(String soapAction, SoapEnvelope envelope, List headers) throws XmlPullParserException, IOException {
        InputStream is;
        if (soapAction == null) {
            soapAction = "\"\"";
        }
        System.out.println("call action:" + soapAction);
        byte[] requestData = createRequestData(envelope, "UTF-8");
        if (requestData != null) {
            this.requestDump = this.debug ? new String(requestData) : null;
        } else {
            this.requestDump = null;
        }
        this.responseDump = null;
        System.out.println("requestDump:" + this.requestDump);
        ServiceConnection connection = getServiceConnection();
        System.out.println("connection:" + connection);
        connection.setRequestProperty("User-Agent", "ksoap2-android/2.6.0+");
        System.out.println("envelope:" + envelope);
        if (envelope != null) {
            if (envelope.version != 120) {
                connection.setRequestProperty("SOAPAction", soapAction);
            }
            if (envelope.version == 120) {
                connection.setRequestProperty("Content-Type", "application/soap+xml;charset=utf-8");
            } else {
                connection.setRequestProperty("Content-Type", "text/xml;charset=utf-8");
            }
            connection.setRequestProperty("Connection", "close");
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setRequestProperty("Content-Length", "" + requestData.length);
            if (headers != null) {
                for (int i = 0; i < headers.size(); i++) {
                    HeaderProperty hp = (HeaderProperty) headers.get(i);
                    connection.setRequestProperty(hp.getKey(), hp.getValue());
                }
            }
            connection.setRequestMethod("POST");
        } else {
            connection.setRequestProperty("Connection", "close");
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setRequestMethod("GET");
        }
        if (requestData != null) {
            OutputStream os = connection.openOutputStream();
            os.write(requestData, 0, requestData.length);
            os.flush();
            os.close();
        }
        List retHeaders = null;
        boolean gZippedContent = false;
        try {
            retHeaders = connection.getResponseProperties();
            System.out.println("[HttpTransportSE] retHeaders = " + retHeaders);
            for (int i2 = 0; i2 < retHeaders.size(); i2++) {
                HeaderProperty hp2 = (HeaderProperty) retHeaders.get(i2);
                if (hp2.getKey() != null && hp2.getKey().equalsIgnoreCase("Content-Encoding") && hp2.getValue().equalsIgnoreCase("gzip")) {
                    gZippedContent = true;
                }
            }
            if (gZippedContent) {
                is = getUnZippedInputStream(connection.openInputStream());
            } else {
                is = connection.openInputStream();
            }
        } catch (IOException e) {
            if (0 != 0) {
                is = getUnZippedInputStream(connection.getErrorStream());
            } else {
                is = connection.getErrorStream();
            }
            if (is == null) {
                connection.disconnect();
                throw e;
            }
        }
        if (this.debug) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            while (true) {
                int rd = is.read(buf, 0, 8192);
                if (rd == -1) {
                    break;
                }
                bos.write(buf, 0, rd);
            }
            bos.flush();
            byte[] buf2 = bos.toByteArray();
            this.responseDump = new String(buf2);
            System.out.println("responseDump:" + this.responseDump);
            is.close();
            is = new ByteArrayInputStream(buf2);
        }
        if (envelope != null) {
            parseResponse(envelope, is);
        }
        return retHeaders;
    }

    private InputStream getUnZippedInputStream(InputStream inputStream) throws IOException {
        try {
            return (GZIPInputStream) inputStream;
        } catch (ClassCastException e) {
            return new GZIPInputStream(inputStream);
        }
    }

    @Override
    public ServiceConnection getServiceConnection() throws IOException {
        if (this.serviceConnection == null) {
            System.out.println("new ServiceConnectionSE:" + this.proxy + " " + this.url + " " + this.timeout);
            this.serviceConnection = new ServiceConnectionSE(this.proxy, this.url, this.timeout);
        }
        return this.serviceConnection;
    }

    @Override
    public String getHost() {
        try {
            String retVal = new URL(this.url).getHost();
            return retVal;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public int getPort() {
        try {
            int retVal = new URL(this.url).getPort();
            return retVal;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public String getPath() {
        try {
            String retVal = new URL(this.url).getPath();
            return retVal;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getQuery() {
        try {
            String retVal = new URL(this.url).getQuery();
            return retVal;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] getRequestData(SoapEnvelope envelope, String encoding) {
        try {
            return createRequestData(envelope, encoding);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
