package com.android.printspooler.model;

import android.print.PrintJobInfo;
import java.util.function.BiConsumer;

public final class $$Lambda$PrintSpoolerService$gRIXcCK9lL2zxK9JFntdYvHjEBI implements BiConsumer {
    public static final $$Lambda$PrintSpoolerService$gRIXcCK9lL2zxK9JFntdYvHjEBI INSTANCE = new $$Lambda$PrintSpoolerService$gRIXcCK9lL2zxK9JFntdYvHjEBI();

    private $$Lambda$PrintSpoolerService$gRIXcCK9lL2zxK9JFntdYvHjEBI() {
    }

    @Override
    public final void accept(Object obj, Object obj2) {
        ((PrintSpoolerService) obj).onPrintJobStateChanged((PrintJobInfo) obj2);
    }
}
