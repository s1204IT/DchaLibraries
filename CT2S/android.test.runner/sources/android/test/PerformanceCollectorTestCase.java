package android.test;

import android.os.PerformanceCollector;

public interface PerformanceCollectorTestCase {
    public static final PerformanceCollector mPerfCollector = new PerformanceCollector();

    void setPerformanceResultsWriter(PerformanceCollector.PerformanceResultsWriter performanceResultsWriter);
}
