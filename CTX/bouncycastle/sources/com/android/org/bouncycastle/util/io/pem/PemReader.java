package com.android.org.bouncycastle.util.io.pem;

import com.android.org.bouncycastle.util.encoders.Base64;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

public class PemReader extends BufferedReader {
    private static final String BEGIN = "-----BEGIN ";
    private static final String END = "-----END ";

    public PemReader(Reader reader) {
        super(reader);
    }

    public PemObject readPemObject() throws IOException {
        String line = readLine();
        while (line != null && !line.startsWith(BEGIN)) {
            line = readLine();
        }
        if (line != null) {
            String strSubstring = line.substring(BEGIN.length());
            int iIndexOf = strSubstring.indexOf(45);
            String strSubstring2 = strSubstring.substring(0, iIndexOf);
            if (iIndexOf > 0) {
                return loadObject(strSubstring2);
            }
            return null;
        }
        return null;
    }

    private PemObject loadObject(String str) throws IOException {
        String line;
        String str2 = END + str;
        StringBuffer stringBuffer = new StringBuffer();
        ArrayList arrayList = new ArrayList();
        while (true) {
            line = readLine();
            if (line == null) {
                break;
            }
            if (line.indexOf(":") >= 0) {
                int iIndexOf = line.indexOf(58);
                arrayList.add(new PemHeader(line.substring(0, iIndexOf), line.substring(iIndexOf + 1).trim()));
            } else {
                if (line.indexOf(str2) != -1) {
                    break;
                }
                stringBuffer.append(line.trim());
            }
        }
        if (line == null) {
            throw new IOException(str2 + " not found");
        }
        return new PemObject(str, arrayList, Base64.decode(stringBuffer.toString()));
    }
}
