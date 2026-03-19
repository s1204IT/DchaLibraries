package com.android.statementservice.retriever;

import android.util.JsonWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

final class AssetJsonWriter {
    private StringWriter mStringWriter = new StringWriter();
    private boolean mClosed = false;
    private JsonWriter mWriter = new JsonWriter(this.mStringWriter);

    public AssetJsonWriter() {
        try {
            this.mWriter.beginObject();
        } catch (IOException e) {
            throw new AssertionError("Unreachable exception.");
        }
    }

    public void writeFieldLower(String str, String str2) {
        if (this.mClosed) {
            throw new IllegalArgumentException("Cannot write to an object that has already been closed.");
        }
        if (str2 != null) {
            try {
                this.mWriter.name(str.toLowerCase(Locale.US));
                this.mWriter.value(str2.toLowerCase(Locale.US));
            } catch (IOException e) {
                throw new AssertionError("Unreachable exception.");
            }
        }
    }

    public void writeArrayUpper(String str, List<String> list) {
        if (this.mClosed) {
            throw new IllegalArgumentException("Cannot write to an object that has already been closed.");
        }
        if (list != null) {
            try {
                this.mWriter.name(str.toLowerCase(Locale.US));
                this.mWriter.beginArray();
                Iterator<String> it = list.iterator();
                while (it.hasNext()) {
                    this.mWriter.value(it.next().toUpperCase(Locale.US));
                }
                this.mWriter.endArray();
            } catch (IOException e) {
                throw new AssertionError("Unreachable exception.");
            }
        }
    }

    public String closeAndGetString() {
        if (!this.mClosed) {
            try {
                this.mWriter.endObject();
                this.mClosed = true;
            } catch (IOException e) {
                throw new AssertionError("Unreachable exception.");
            }
        }
        return this.mStringWriter.toString();
    }
}
