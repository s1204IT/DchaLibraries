package com.coremedia.iso.boxes.apple;

import java.util.HashMap;
import java.util.Map;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class AppleMediaTypeBox extends AbstractAppleMetaDataBox {
    private static Map<String, String> mediaTypes = new HashMap();

    static {
        mediaTypes.put(SchemaSymbols.ATTVAL_FALSE_0, "Movie (is now 9)");
        mediaTypes.put(SchemaSymbols.ATTVAL_TRUE_1, "Normal (Music)");
        mediaTypes.put("2", "Audiobook");
        mediaTypes.put("6", "Music Video");
        mediaTypes.put("9", "Movie");
        mediaTypes.put("10", "TV Show");
        mediaTypes.put("11", "Booklet");
        mediaTypes.put("14", "Ringtone");
    }

    public AppleMediaTypeBox() {
        super("stik");
        this.appleDataBox = AppleDataBox.getUint8AppleDataBox();
    }
}
