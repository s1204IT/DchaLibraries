package android.filterpacks.performance;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.format.ObjectFormat;
import android.hardware.Camera;
import android.os.SystemClock;

public class ThroughputFilter extends Filter {
    private long mLastTime;
    private FrameFormat mOutputFormat;

    @GenerateFieldPort(hasDefault = true, name = "period")
    private int mPeriod;
    private int mPeriodFrameCount;
    private int mTotalFrameCount;

    public ThroughputFilter(String name) {
        super(name);
        this.mPeriod = 5;
        this.mLastTime = 0L;
        this.mTotalFrameCount = 0;
        this.mPeriodFrameCount = 0;
    }

    @Override
    public void setupPorts() {
        addInputPort(Camera.Parameters.EFFECT_FRAME);
        this.mOutputFormat = ObjectFormat.fromClass(Throughput.class, 1);
        addOutputBasedOnInput(Camera.Parameters.EFFECT_FRAME, Camera.Parameters.EFFECT_FRAME);
        addOutputPort("throughput", this.mOutputFormat);
    }

    @Override
    public FrameFormat getOutputFormat(String portName, FrameFormat inputFormat) {
        return inputFormat;
    }

    @Override
    public void open(FilterContext env) {
        this.mTotalFrameCount = 0;
        this.mPeriodFrameCount = 0;
        this.mLastTime = 0L;
    }

    @Override
    public void process(FilterContext context) {
        Frame input = pullInput(Camera.Parameters.EFFECT_FRAME);
        pushOutput(Camera.Parameters.EFFECT_FRAME, input);
        this.mTotalFrameCount++;
        this.mPeriodFrameCount++;
        if (this.mLastTime == 0) {
            this.mLastTime = SystemClock.elapsedRealtime();
        }
        long curTime = SystemClock.elapsedRealtime();
        if (curTime - this.mLastTime >= this.mPeriod * 1000) {
            FrameFormat inputFormat = input.getFormat();
            int pixelCount = inputFormat.getWidth() * inputFormat.getHeight();
            Throughput throughput = new Throughput(this.mTotalFrameCount, this.mPeriodFrameCount, this.mPeriod, pixelCount);
            Frame throughputFrame = context.getFrameManager().newFrame(this.mOutputFormat);
            throughputFrame.setObjectValue(throughput);
            pushOutput("throughput", throughputFrame);
            this.mLastTime = curTime;
            this.mPeriodFrameCount = 0;
        }
    }
}
