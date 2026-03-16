package com.android.camera.util;

import android.support.v4.view.MotionEventCompat;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.options.SerializeOptions;
import com.android.camera.Storage;
import com.android.camera.debug.Log;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class XmpUtil {
    private static final String GOOGLE_PANO_NAMESPACE = "http://ns.google.com/photos/1.0/panorama/";
    private static final int MAX_XMP_BUFFER_SIZE = 65502;
    private static final int M_APP1 = 225;
    private static final int M_SOI = 216;
    private static final int M_SOS = 218;
    private static final String PANO_PREFIX = "GPano";
    private static final Log.Tag TAG = new Log.Tag("XmpUtil");
    private static final String XMP_HEADER = "http://ns.adobe.com/xap/1.0/\u0000";
    private static final int XMP_HEADER_SIZE = 29;

    static {
        try {
            XMPMetaFactory.getSchemaRegistry().registerNamespace("http://ns.google.com/photos/1.0/panorama/", PANO_PREFIX);
        } catch (XMPException e) {
            e.printStackTrace();
        }
    }

    private static class Section {
        public byte[] data;
        public int length;
        public int marker;

        private Section() {
        }
    }

    public static XMPMeta extractXMPMeta(String filename) {
        if (!filename.toLowerCase().endsWith(Storage.JPEG_POSTFIX) && !filename.toLowerCase().endsWith(".jpeg")) {
            Log.d(TAG, "XMP parse: only jpeg file is supported");
            return null;
        }
        try {
            return extractXMPMeta(new FileInputStream(filename));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not read file: " + filename, e);
            return null;
        }
    }

    public static XMPMeta extractXMPMeta(InputStream is) {
        List<Section> sections = parse(is, true);
        if (sections == null) {
            return null;
        }
        for (Section section : sections) {
            if (hasXMPHeader(section.data)) {
                int end = getXMPContentEnd(section.data);
                byte[] buffer = new byte[end - 29];
                System.arraycopy(section.data, XMP_HEADER_SIZE, buffer, 0, buffer.length);
                try {
                    return XMPMetaFactory.parseFromBuffer(buffer);
                } catch (XMPException e) {
                    Log.d(TAG, "XMP parse error", e);
                    return null;
                }
            }
        }
        return null;
    }

    public static XMPMeta createXMPMeta() {
        return XMPMetaFactory.create();
    }

    public static XMPMeta extractOrCreateXMPMeta(String filename) {
        XMPMeta meta = extractXMPMeta(filename);
        return meta == null ? createXMPMeta() : meta;
    }

    public static boolean writeXMPMeta(String filename, XMPMeta meta) throws Throwable {
        FileOutputStream os;
        boolean z = false;
        if (filename.toLowerCase().endsWith(Storage.JPEG_POSTFIX) || filename.toLowerCase().endsWith(".jpeg")) {
            try {
                List<Section> sections = insertXMPSection(parse(new FileInputStream(filename), false), meta);
                if (sections != null) {
                    FileOutputStream os2 = null;
                    try {
                        try {
                            os = new FileOutputStream(filename);
                        } catch (Throwable th) {
                            th = th;
                        }
                    } catch (IOException e) {
                        e = e;
                    }
                    try {
                        writeJpegFile(os, sections);
                        if (os != null) {
                            try {
                                os.close();
                            } catch (IOException e2) {
                            }
                        }
                        z = true;
                    } catch (IOException e3) {
                        e = e3;
                        os2 = os;
                        Log.d(TAG, "Write file failed:" + filename, e);
                        if (os2 != null) {
                            try {
                                os2.close();
                            } catch (IOException e4) {
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        os2 = os;
                        if (os2 != null) {
                            try {
                                os2.close();
                            } catch (IOException e5) {
                            }
                        }
                        throw th;
                    }
                }
            } catch (FileNotFoundException e6) {
                Log.e(TAG, "Could not read file: " + filename, e6);
            }
        } else {
            Log.d(TAG, "XMP parse: only jpeg file is supported");
        }
        return z;
    }

    public static boolean writeXMPMeta(InputStream inputStream, OutputStream outputStream, XMPMeta meta) {
        List<Section> sections = insertXMPSection(parse(inputStream, false), meta);
        if (sections == null) {
            return false;
        }
        try {
            try {
                writeJpegFile(outputStream, sections);
                return true;
            } catch (IOException e) {
                Log.d(TAG, "Write to stream failed", e);
                if (outputStream == null) {
                    return false;
                }
                try {
                    outputStream.close();
                    return false;
                } catch (IOException e2) {
                    return false;
                }
            }
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e3) {
                }
            }
        }
    }

    private static void writeJpegFile(OutputStream os, List<Section> sections) throws IOException {
        os.write(MotionEventCompat.ACTION_MASK);
        os.write(M_SOI);
        for (Section section : sections) {
            os.write(MotionEventCompat.ACTION_MASK);
            os.write(section.marker);
            if (section.length > 0) {
                int lh = section.length >> 8;
                int ll = section.length & MotionEventCompat.ACTION_MASK;
                os.write(lh);
                os.write(ll);
            }
            os.write(section.data);
        }
    }

    private static List<Section> insertXMPSection(List<Section> sections, XMPMeta meta) {
        if (sections == null || sections.size() <= 1) {
            return null;
        }
        try {
            SerializeOptions options = new SerializeOptions();
            options.setUseCompactFormat(true);
            options.setOmitPacketWrapper(true);
            byte[] buffer = XMPMetaFactory.serializeToBuffer(meta, options);
            if (buffer.length > MAX_XMP_BUFFER_SIZE) {
                return null;
            }
            byte[] xmpdata = new byte[buffer.length + XMP_HEADER_SIZE];
            System.arraycopy(XMP_HEADER.getBytes(), 0, xmpdata, 0, XMP_HEADER_SIZE);
            System.arraycopy(buffer, 0, xmpdata, XMP_HEADER_SIZE, buffer.length);
            Section xmpSection = new Section();
            xmpSection.marker = M_APP1;
            xmpSection.length = xmpdata.length + 2;
            xmpSection.data = xmpdata;
            for (int i = 0; i < sections.size(); i++) {
                if (sections.get(i).marker == M_APP1 && hasXMPHeader(sections.get(i).data)) {
                    sections.set(i, xmpSection);
                    return sections;
                }
            }
            List<Section> newSections = new ArrayList<>();
            int position = sections.get(0).marker != M_APP1 ? 0 : 1;
            newSections.addAll(sections.subList(0, position));
            newSections.add(xmpSection);
            newSections.addAll(sections.subList(position, sections.size()));
            return newSections;
        } catch (XMPException e) {
            Log.d(TAG, "Serialize xmp failed", e);
            return null;
        }
    }

    private static boolean hasXMPHeader(byte[] data) {
        if (data.length < XMP_HEADER_SIZE) {
            return false;
        }
        try {
            byte[] header = new byte[XMP_HEADER_SIZE];
            System.arraycopy(data, 0, header, 0, XMP_HEADER_SIZE);
            return new String(header, "UTF-8").equals(XMP_HEADER);
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }

    private static int getXMPContentEnd(byte[] data) {
        for (int i = data.length - 1; i >= 1; i--) {
            if (data[i] == 62 && data[i - 1] != 63) {
                return i + 1;
            }
        }
        return data.length;
    }

    private static List<Section> parse(InputStream is, boolean readMetaOnly) {
        int c;
        try {
            try {
                if (is.read() != 255 || is.read() != M_SOI) {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                        }
                    }
                    return null;
                }
                List<Section> sections = new ArrayList<>();
                while (true) {
                    int c2 = is.read();
                    if (c2 == -1) {
                        if (is == null) {
                            return sections;
                        }
                        try {
                            is.close();
                            return sections;
                        } catch (IOException e2) {
                            return sections;
                        }
                    }
                    if (c2 != 255) {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException e3) {
                            }
                        }
                        return null;
                    }
                    do {
                        c = is.read();
                    } while (c == 255);
                    if (c == -1) {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException e4) {
                            }
                        }
                        return null;
                    }
                    if (c == M_SOS) {
                        break;
                    }
                    int lh = is.read();
                    int ll = is.read();
                    if (lh == -1 || ll == -1) {
                        break;
                    }
                    int length = (lh << 8) | ll;
                    if (!readMetaOnly || c == M_APP1) {
                        Section section = new Section();
                        section.marker = c;
                        section.length = length;
                        section.data = new byte[length - 2];
                        is.read(section.data, 0, length - 2);
                        sections.add(section);
                    } else {
                        is.skip(length - 2);
                    }
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e5) {
                    }
                }
                return null;
            } catch (IOException e6) {
                Log.d(TAG, "Could not parse file.", e6);
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e7) {
                    }
                }
                return null;
            }
        } catch (Throwable th) {
            if (is != null) {
            }
            throw th;
        }
        if (is != null) {
            try {
                is.close();
            } catch (IOException e8) {
            }
        }
        throw th;
    }

    private XmpUtil() {
    }
}
