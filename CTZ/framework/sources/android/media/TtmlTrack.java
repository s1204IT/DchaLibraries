package android.media;

import android.media.SubtitleTrack;
import android.util.Log;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;
import org.xmlpull.v1.XmlPullParserException;

class TtmlTrack extends SubtitleTrack implements TtmlNodeListener {
    private static final String TAG = "TtmlTrack";
    private Long mCurrentRunID;
    private final TtmlParser mParser;
    private String mParsingData;
    private final TtmlRenderingWidget mRenderingWidget;
    private TtmlNode mRootNode;
    private final TreeSet<Long> mTimeEvents;
    private final LinkedList<TtmlNode> mTtmlNodes;

    TtmlTrack(TtmlRenderingWidget ttmlRenderingWidget, MediaFormat mediaFormat) {
        super(mediaFormat);
        this.mParser = new TtmlParser(this);
        this.mTtmlNodes = new LinkedList<>();
        this.mTimeEvents = new TreeSet<>();
        this.mRenderingWidget = ttmlRenderingWidget;
        this.mParsingData = "";
    }

    @Override
    public TtmlRenderingWidget getRenderingWidget() {
        return this.mRenderingWidget;
    }

    @Override
    public void onData(byte[] bArr, boolean z, long j) {
        try {
            String str = new String(bArr, "UTF-8");
            synchronized (this.mParser) {
                if (this.mCurrentRunID != null && j != this.mCurrentRunID.longValue()) {
                    throw new IllegalStateException("Run #" + this.mCurrentRunID + " in progress.  Cannot process run #" + j);
                }
                this.mCurrentRunID = Long.valueOf(j);
                this.mParsingData += str;
                if (z) {
                    try {
                        this.mParser.parse(this.mParsingData, this.mCurrentRunID.longValue());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (XmlPullParserException e2) {
                        e2.printStackTrace();
                    }
                    finishedRun(j);
                    this.mParsingData = "";
                    this.mCurrentRunID = null;
                }
            }
        } catch (UnsupportedEncodingException e3) {
            Log.w(TAG, "subtitle data is not UTF-8 encoded: " + e3);
        }
    }

    @Override
    public void onTtmlNodeParsed(TtmlNode ttmlNode) {
        this.mTtmlNodes.addLast(ttmlNode);
        addTimeEvents(ttmlNode);
    }

    @Override
    public void onRootNodeParsed(TtmlNode ttmlNode) {
        this.mRootNode = ttmlNode;
        while (true) {
            TtmlCue nextResult = getNextResult();
            if (nextResult != null) {
                addCue(nextResult);
            } else {
                this.mRootNode = null;
                this.mTtmlNodes.clear();
                this.mTimeEvents.clear();
                return;
            }
        }
    }

    @Override
    public void updateView(Vector<SubtitleTrack.Cue> vector) {
        if (!this.mVisible) {
            return;
        }
        if (this.DEBUG && this.mTimeProvider != null) {
            try {
                Log.d(TAG, "at " + (this.mTimeProvider.getCurrentTimeUs(false, true) / 1000) + " ms the active cues are:");
            } catch (IllegalStateException e) {
                Log.d(TAG, "at (illegal state) the active cues are:");
            }
        }
        this.mRenderingWidget.setActiveCues(vector);
    }

    public TtmlCue getNextResult() {
        while (this.mTimeEvents.size() >= 2) {
            long jLongValue = this.mTimeEvents.pollFirst().longValue();
            long jLongValue2 = this.mTimeEvents.first().longValue();
            if (!getActiveNodes(jLongValue, jLongValue2).isEmpty()) {
                return new TtmlCue(jLongValue, jLongValue2, TtmlUtils.applySpacePolicy(TtmlUtils.extractText(this.mRootNode, jLongValue, jLongValue2), false), TtmlUtils.extractTtmlFragment(this.mRootNode, jLongValue, jLongValue2));
            }
        }
        return null;
    }

    private void addTimeEvents(TtmlNode ttmlNode) {
        this.mTimeEvents.add(Long.valueOf(ttmlNode.mStartTimeMs));
        this.mTimeEvents.add(Long.valueOf(ttmlNode.mEndTimeMs));
        for (int i = 0; i < ttmlNode.mChildren.size(); i++) {
            addTimeEvents(ttmlNode.mChildren.get(i));
        }
    }

    private List<TtmlNode> getActiveNodes(long j, long j2) {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < this.mTtmlNodes.size(); i++) {
            TtmlNode ttmlNode = this.mTtmlNodes.get(i);
            if (ttmlNode.isActive(j, j2)) {
                arrayList.add(ttmlNode);
            }
        }
        return arrayList;
    }
}
