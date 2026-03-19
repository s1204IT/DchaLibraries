package org.ccil.cowan.tagsoup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import org.xml.sax.SAXException;

public class PYXScanner implements Scanner {
    @Override
    public void resetDocumentLocator(String publicid, String systemid) {
    }

    @Override
    public void scan(Reader r, ScanHandler h) throws SAXException, IOException {
        BufferedReader br = new BufferedReader(r);
        char[] buff = null;
        boolean instag = false;
        while (true) {
            String s = br.readLine();
            if (s != null) {
                int size = s.length();
                if (buff == null || buff.length < size) {
                    buff = new char[size];
                }
                s.getChars(0, size, buff, 0);
                switch (buff[0]) {
                    case '(':
                        if (instag) {
                            h.stagc(buff, 0, 0);
                        }
                        h.gi(buff, 1, size - 1);
                        instag = true;
                        break;
                    case ')':
                        if (instag) {
                            h.stagc(buff, 0, 0);
                            instag = false;
                        }
                        h.etag(buff, 1, size - 1);
                        break;
                    case '-':
                        if (instag) {
                            h.stagc(buff, 0, 0);
                            instag = false;
                        }
                        if (s.equals("-\\n")) {
                            buff[0] = '\n';
                            h.pcdata(buff, 0, 1);
                        } else {
                            h.pcdata(buff, 1, size - 1);
                        }
                        break;
                    case '?':
                        if (instag) {
                            h.stagc(buff, 0, 0);
                            instag = false;
                        }
                        h.pi(buff, 1, size - 1);
                        break;
                    case 'A':
                        int sp = s.indexOf(32);
                        h.aname(buff, 1, sp - 1);
                        h.aval(buff, sp + 1, (size - sp) - 1);
                        break;
                    case 'E':
                        if (instag) {
                            h.stagc(buff, 0, 0);
                            instag = false;
                        }
                        h.entity(buff, 1, size - 1);
                        break;
                }
            } else {
                h.eof(buff, 0, 0);
                return;
            }
        }
    }

    @Override
    public void startCDATA() {
    }

    public static void main(String[] argv) throws SAXException, IOException {
        Scanner s = new PYXScanner();
        Reader r = new InputStreamReader(System.in, "UTF-8");
        Writer w = new BufferedWriter(new OutputStreamWriter(System.out, "UTF-8"));
        s.scan(r, new PYXWriter(w));
    }
}
