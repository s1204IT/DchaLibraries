package com.android.bips.discovery;

import android.net.Uri;
import android.print.PrinterId;
import android.printservice.PrintService;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonWriter;
import com.android.bips.jni.BackendConstants;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Objects;

public class DiscoveredPrinter {
    public final String location;
    private PrinterId mPrinterId;
    public final String name;
    public final Uri path;
    public final Uri uuid;

    public DiscoveredPrinter(Uri uri, String str, Uri uri2, String str2) {
        this.uuid = uri;
        this.name = str;
        this.path = uri2;
        this.location = str2;
    }

    public DiscoveredPrinter(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        String strNextString = null;
        Uri uri = null;
        Uri uri2 = null;
        String strNextString2 = null;
        while (jsonReader.hasNext()) {
            String strNextName = jsonReader.nextName();
            byte b = -1;
            int iHashCode = strNextName.hashCode();
            if (iHashCode != 3373707) {
                if (iHashCode != 3433509) {
                    if (iHashCode != 3601339) {
                        if (iHashCode == 1901043637 && strNextName.equals("location")) {
                            b = 3;
                        }
                    } else if (strNextName.equals("uuid")) {
                        b = 0;
                    }
                } else if (strNextName.equals("path")) {
                    b = 2;
                }
            } else if (strNextName.equals("name")) {
                b = 1;
            }
            switch (b) {
                case BackendConstants.STATUS_OK:
                    uri2 = Uri.parse(jsonReader.nextString());
                    break;
                case BackendConstants.ALIGN_CENTER_HORIZONTAL:
                    strNextString = jsonReader.nextString();
                    break;
                case 2:
                    uri = Uri.parse(jsonReader.nextString());
                    break;
                case 3:
                    strNextString2 = jsonReader.nextString();
                    break;
            }
        }
        jsonReader.endObject();
        if (strNextString == null || uri == null) {
            throw new IOException("Missing name or path");
        }
        this.uuid = uri2;
        this.name = strNextString;
        this.path = uri;
        this.location = strNextString2;
    }

    public Uri getUri() {
        return this.uuid != null ? this.uuid : this.path;
    }

    public String getHost() {
        return this.path.getHost().replaceAll(":[0-9]+", "");
    }

    public PrinterId getId(PrintService printService) {
        if (this.mPrinterId == null) {
            this.mPrinterId = printService.generatePrinterId(getUri().toString());
        }
        return this.mPrinterId;
    }

    void write(JsonWriter jsonWriter) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name("name").value(this.name);
        jsonWriter.name("path").value(this.path.toString());
        if (this.uuid != null) {
            jsonWriter.name("uuid").value(this.uuid.toString());
        }
        if (!TextUtils.isEmpty(this.location)) {
            jsonWriter.name("location").value(this.location);
        }
        jsonWriter.endObject();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DiscoveredPrinter)) {
            return false;
        }
        DiscoveredPrinter discoveredPrinter = (DiscoveredPrinter) obj;
        return Objects.equals(this.uuid, discoveredPrinter.uuid) && Objects.equals(this.name, discoveredPrinter.name) && Objects.equals(this.path, discoveredPrinter.path) && Objects.equals(this.location, discoveredPrinter.location);
    }

    public int hashCode() {
        return (31 * (((((527 + this.name.hashCode()) * 31) + (this.uuid != null ? this.uuid.hashCode() : 0)) * 31) + this.path.hashCode())) + (this.location != null ? this.location.hashCode() : 0);
    }

    public String toString() {
        StringWriter stringWriter = new StringWriter();
        try {
            write(new JsonWriter(stringWriter));
        } catch (IOException e) {
        }
        return "DiscoveredPrinter" + stringWriter.toString();
    }
}
