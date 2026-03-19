package android.view;

import com.mediatek.perfframe.PerfFrameInfoFactory;
import com.mediatek.perfframe.PerfFrameInfoManager;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class FrameInfo {
    private static final int ANIMATION_START = 6;
    private static final int DRAW_START = 8;
    private static final int FLAGS = 0;
    public static final long FLAG_WINDOW_LAYOUT_CHANGED = 1;
    private static final int HANDLE_INPUT_START = 5;
    private static final int INTENDED_VSYNC = 1;
    private static final int NEWEST_INPUT_EVENT = 4;
    private static final int OLDEST_INPUT_EVENT = 3;
    private static final int PERFORM_TRAVERSALS_START = 7;
    private static final int VSYNC = 2;
    private static PerfFrameInfoManager mPerfFrameInfoManager = PerfFrameInfoFactory.getInstance().makePerfFrameInfoManager();
    private long gl_aligned_start_time;
    long[] mFrameInfo = new long[9];

    @Retention(RetentionPolicy.SOURCE)
    public @interface FrameInfoFlags {
    }

    public void setVsync(long j, long j2) {
        this.mFrameInfo[1] = j;
        this.mFrameInfo[2] = j2;
        this.mFrameInfo[3] = Long.MAX_VALUE;
        this.mFrameInfo[4] = 0;
        this.mFrameInfo[0] = 0;
        mPerfFrameInfoManager.clearDrawStartAndMarkIntendedVsync(j);
    }

    public void updateInputEventTime(long j, long j2) {
        if (j2 < this.mFrameInfo[3]) {
            this.mFrameInfo[3] = j2;
        }
        if (j > this.mFrameInfo[4]) {
            this.mFrameInfo[4] = j;
        }
    }

    public void markInputHandlingStart() {
        this.mFrameInfo[5] = System.nanoTime();
    }

    public void markAnimationsStart() {
        this.mFrameInfo[6] = System.nanoTime();
    }

    public void markPerformTraversalsStart() {
        this.mFrameInfo[7] = System.nanoTime();
    }

    public void markDrawStart() {
        this.mFrameInfo[8] = System.nanoTime();
        mPerfFrameInfoManager.setDrawStart(this.mFrameInfo[1]);
    }

    public void markDoFrameEnd() {
        mPerfFrameInfoManager.markDoFrameEnd(((int) System.nanoTime()) - ((int) this.mFrameInfo[1]), this.mFrameInfo[1]);
    }

    public void markGLDrawStart() {
        this.gl_aligned_start_time = System.nanoTime();
        mPerfFrameInfoManager.markGLDrawStart();
    }

    public void markGLDrawEnd() {
        mPerfFrameInfoManager.markGLDrawEnd(((int) System.nanoTime()) - ((int) this.gl_aligned_start_time));
    }

    public void addFlags(long j) {
        long[] jArr = this.mFrameInfo;
        jArr[0] = j | jArr[0];
    }
}
