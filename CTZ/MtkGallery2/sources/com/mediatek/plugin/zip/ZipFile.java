package com.mediatek.plugin.zip;

import android.content.Context;
import android.content.pm.Signature;
import com.mediatek.plugin.preload.SchemaValidate;
import com.mediatek.plugin.preload.SignatureParser;
import com.mediatek.plugin.preload.SoOperater;
import com.mediatek.plugin.res.IResource;
import com.mediatek.plugin.utils.Log;
import com.mediatek.plugin.utils.TraceHelper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;

public abstract class ZipFile {
    private static final String TAG = "PluginManager/ZipFile";
    protected String mFilPath;
    private java.util.jar.JarFile mJarFile;
    protected SchemaValidate mSchemaValidate;

    public interface ZipEntryConsumer {
        boolean consume(ZipEntry zipEntry);
    }

    public abstract IResource getResource(Context context);

    protected abstract String getXmlRelativePath();

    public ZipFile(String str) {
        this.mFilPath = str;
        TraceHelper.beginSection(">>>>ZipFile-new JarFile");
        try {
            this.mJarFile = new java.util.jar.JarFile(str);
        } catch (IOException e) {
            Log.e(TAG, "<ZipFile>", e);
        }
        TraceHelper.endSection();
    }

    public Signature[] getSignature() {
        TraceHelper.beginSection(">>>>ZipFile-getSignature");
        Signature[] signature = SignatureParser.parseSignature(this.mJarFile);
        TraceHelper.endSection();
        return signature;
    }

    public void copySoLib(final Context context, final String str) {
        TraceHelper.beginSection(">>>>ZipFile-copySoLib");
        final SoOperater soOperater = new SoOperater();
        ZipEntry zipEntryEnumerateZipEntry = enumerateZipEntry(new ZipEntryConsumer() {
            @Override
            public boolean consume(ZipEntry zipEntry) {
                String name = zipEntry.getName();
                if (name.endsWith(".so")) {
                    Log.d(ZipFile.TAG, "<copySoLib> So Lib Name: " + name + " || " + str);
                    return soOperater.isNewSo(context, name, zipEntry.getTime());
                }
                return false;
            }
        });
        if (zipEntryEnumerateZipEntry != null) {
            Log.d(TAG, "<copySoLib> Copying so lib: " + zipEntryEnumerateZipEntry.getName() + " to :" + str);
            soOperater.copy(context, this.mJarFile, zipEntryEnumerateZipEntry, str);
        }
        TraceHelper.endSection();
    }

    public InputStream getXmlInputStream() {
        return getInputStream(this.mFilPath, getXmlRelativePath());
    }

    public InputStream getInputStream(String str, final String str2) {
        InputStream inputStream;
        TraceHelper.beginSection(">>>>ZipFile-getInputStream");
        ZipEntry zipEntryEnumerateZipEntry = enumerateZipEntry(new ZipEntryConsumer() {
            @Override
            public boolean consume(ZipEntry zipEntry) {
                return str2.equalsIgnoreCase(zipEntry.getName());
            }
        });
        if (zipEntryEnumerateZipEntry != null) {
            Log.d(TAG, "<getInputStream> entryName = " + zipEntryEnumerateZipEntry.getName());
            try {
                inputStream = this.mJarFile.getInputStream(zipEntryEnumerateZipEntry);
            } catch (IOException e) {
                Log.e(TAG, "<getInputStream>", e);
                inputStream = null;
            }
        } else {
            inputStream = null;
        }
        TraceHelper.endSection();
        return inputStream;
    }

    public boolean validateXML(InputStream inputStream) {
        TraceHelper.beginSection(">>>>ZipFile-validateXML");
        if (this.mSchemaValidate == null) {
            this.mSchemaValidate = new SchemaValidate();
        }
        boolean zValidateXMLFile = this.mSchemaValidate.validateXMLFile(inputStream, getXmlInputStream());
        TraceHelper.endSection();
        return zValidateXMLFile;
    }

    public void recycle() {
        try {
            this.mJarFile.close();
        } catch (IOException e) {
            Log.e(TAG, "<recycle>", e);
        }
    }

    private ZipEntry enumerateZipEntry(ZipEntryConsumer zipEntryConsumer) {
        if (zipEntryConsumer == null) {
            return null;
        }
        Enumeration<JarEntry> enumerationEntries = this.mJarFile.entries();
        while (enumerationEntries.hasMoreElements()) {
            JarEntry jarEntryNextElement = enumerationEntries.nextElement();
            if (zipEntryConsumer.consume(jarEntryNextElement)) {
                return jarEntryNextElement;
            }
        }
        return null;
    }
}
