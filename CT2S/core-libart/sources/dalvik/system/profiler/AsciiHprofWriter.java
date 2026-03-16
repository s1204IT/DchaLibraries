package dalvik.system.profiler;

import dalvik.system.profiler.HprofData;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public final class AsciiHprofWriter {
    private static final Comparator<HprofData.Sample> SAMPLE_COMPARATOR = new Comparator<HprofData.Sample>() {
        @Override
        public int compare(HprofData.Sample s1, HprofData.Sample s2) {
            return s2.count - s1.count;
        }
    };
    private final HprofData data;
    private final PrintWriter out;

    public static void write(HprofData data, OutputStream outputStream) throws IOException {
        new AsciiHprofWriter(data, outputStream).write();
    }

    private AsciiHprofWriter(HprofData data, OutputStream outputStream) {
        this.data = data;
        this.out = new PrintWriter(outputStream);
    }

    private void write() throws IOException {
        for (HprofData.ThreadEvent e : this.data.getThreadHistory()) {
            this.out.println(e);
        }
        List<HprofData.Sample> samples = new ArrayList<>(this.data.getSamples());
        Collections.sort(samples, SAMPLE_COMPARATOR);
        int total = 0;
        for (HprofData.Sample sample : samples) {
            HprofData.StackTrace stackTrace = sample.stackTrace;
            total += sample.count;
            this.out.printf("TRACE %d: (thread=%d)\n", Integer.valueOf(stackTrace.stackTraceId), Integer.valueOf(stackTrace.threadId));
            StackTraceElement[] arr$ = stackTrace.stackFrames;
            for (StackTraceElement e2 : arr$) {
                this.out.printf("\t%s\n", e2);
            }
        }
        Date now = new Date(this.data.getStartMillis());
        this.out.printf("CPU SAMPLES BEGIN (total = %d) %ta %tb %td %tT %tY\n", Integer.valueOf(total), now, now, now, now, now);
        this.out.printf("rank   self  accum   count trace method\n", new Object[0]);
        int rank = 0;
        double accum = 0.0d;
        for (HprofData.Sample sample2 : samples) {
            rank++;
            HprofData.StackTrace stackTrace2 = sample2.stackTrace;
            int count = sample2.count;
            double self = ((double) count) / ((double) total);
            accum += self;
            this.out.printf("% 4d% 6.2f%%% 6.2f%% % 7d % 5d %s.%s\n", Integer.valueOf(rank), Double.valueOf(100.0d * self), Double.valueOf(100.0d * accum), Integer.valueOf(count), Integer.valueOf(stackTrace2.stackTraceId), stackTrace2.stackFrames[0].getClassName(), stackTrace2.stackFrames[0].getMethodName());
        }
        this.out.printf("CPU SAMPLES END\n", new Object[0]);
        this.out.flush();
    }
}
