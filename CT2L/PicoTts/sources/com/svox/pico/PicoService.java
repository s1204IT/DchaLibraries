package com.svox.pico;

import com.android.tts.compat.CompatTtsService;

public class PicoService extends CompatTtsService {
    @Override
    protected String getSoFilename() {
        return "libttspico.so";
    }
}
