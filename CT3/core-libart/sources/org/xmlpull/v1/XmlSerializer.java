package org.xmlpull.v1;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

public interface XmlSerializer {
    XmlSerializer attribute(String str, String str2, String str3) throws IllegalStateException, IOException, IllegalArgumentException;

    void cdsect(String str) throws IllegalStateException, IOException, IllegalArgumentException;

    void comment(String str) throws IllegalStateException, IOException, IllegalArgumentException;

    void docdecl(String str) throws IllegalStateException, IOException, IllegalArgumentException;

    void endDocument() throws IllegalStateException, IOException, IllegalArgumentException;

    XmlSerializer endTag(String str, String str2) throws IllegalStateException, IOException, IllegalArgumentException;

    void entityRef(String str) throws IllegalStateException, IOException, IllegalArgumentException;

    void flush() throws IOException;

    int getDepth();

    boolean getFeature(String str);

    String getName();

    String getNamespace();

    String getPrefix(String str, boolean z) throws IllegalArgumentException;

    Object getProperty(String str);

    void ignorableWhitespace(String str) throws IllegalStateException, IOException, IllegalArgumentException;

    void processingInstruction(String str) throws IllegalStateException, IOException, IllegalArgumentException;

    void setFeature(String str, boolean z) throws IllegalStateException, IllegalArgumentException;

    void setOutput(OutputStream outputStream, String str) throws IllegalStateException, IOException, IllegalArgumentException;

    void setOutput(Writer writer) throws IllegalStateException, IOException, IllegalArgumentException;

    void setPrefix(String str, String str2) throws IllegalStateException, IOException, IllegalArgumentException;

    void setProperty(String str, Object obj) throws IllegalStateException, IllegalArgumentException;

    void startDocument(String str, Boolean bool) throws IllegalStateException, IOException, IllegalArgumentException;

    XmlSerializer startTag(String str, String str2) throws IllegalStateException, IOException, IllegalArgumentException;

    XmlSerializer text(String str) throws IllegalStateException, IOException, IllegalArgumentException;

    XmlSerializer text(char[] cArr, int i, int i2) throws IllegalStateException, IOException, IllegalArgumentException;
}
