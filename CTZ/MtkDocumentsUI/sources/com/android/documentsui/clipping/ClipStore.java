package com.android.documentsui.clipping;

import android.net.Uri;
import java.io.File;
import java.io.IOException;

public interface ClipStore {
    ClipStorageReader createReader(File file) throws IOException;

    File getFile(int i) throws IOException;

    int persistUris(Iterable<Uri> iterable);
}
