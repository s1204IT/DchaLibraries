package android.media;

import android.media.SubtitleTrack;
import android.net.ProxyInfo;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.util.Log;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

class SRTTrack extends WebVttTrack {
    private static final int KEY_LOCAL_SETTING = 102;
    private static final int KEY_START_TIME = 7;
    private static final int KEY_STRUCT_TEXT = 16;
    private static final int MEDIA_TIMED_TEXT = 99;
    private final Handler mEventHandler;
    private static final String TAG = "SRTTrack";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    SRTTrack(WebVttRenderingWidget renderingWidget, MediaFormat format) {
        super(renderingWidget, format);
        this.mEventHandler = null;
    }

    SRTTrack(Handler eventHandler, MediaFormat format) {
        super(null, format);
        this.mEventHandler = eventHandler;
    }

    @Override
    protected void onData(SubtitleData data) {
        int i = 0;
        try {
            TextTrackCue cue = new TextTrackCue();
            cue.mStartTimeMs = data.getStartTimeUs() / 1000;
            cue.mEndTimeMs = (data.getStartTimeUs() + data.getDurationUs()) / 1000;
            String paragraph = new String(data.getData(), "UTF-8");
            String[] lines = paragraph.split("\\r?\\n");
            cue.mLines = new TextTrackCueSpan[lines.length][];
            int length = lines.length;
            int i2 = 0;
            while (i < length) {
                String line = lines[i];
                TextTrackCueSpan[] span = {new TextTrackCueSpan(line, -1L)};
                cue.mLines[i2] = span;
                i++;
                i2++;
            }
            addCue(cue);
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "subtitle data is not UTF-8 encoded: " + e);
        }
    }

    @Override
    public void onData(byte[] data, boolean eos, long runID) {
        String header;
        try {
            Reader r = new InputStreamReader(new ByteArrayInputStream(data), "UTF-8");
            BufferedReader br = new BufferedReader(r);
            while (br.readLine() != null && (header = br.readLine()) != null) {
                TextTrackCue cue = new TextTrackCue();
                String[] startEnd = header.split("-->");
                cue.mStartTimeMs = parseMs(startEnd[0]);
                String[] endTime = startEnd[1].trim().split(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                if (DEBUG) {
                    Log.d(TAG, "endTime: " + endTime[0]);
                }
                cue.mEndTimeMs = parseMs(endTime[0]);
                cue.mRunID = runID;
                List<String> paragraph = new ArrayList<>();
                while (true) {
                    String s = br.readLine();
                    if (s != null ? s.trim().equals(ProxyInfo.LOCAL_EXCL_LIST) : true) {
                        break;
                    } else {
                        paragraph.add(s);
                    }
                }
                cue.mLines = new TextTrackCueSpan[paragraph.size()][];
                cue.mStrings = (String[]) paragraph.toArray(new String[0]);
                int i = 0;
                for (String line : paragraph) {
                    TextTrackCueSpan[] span = {new TextTrackCueSpan(line, -1L)};
                    cue.mStrings[i] = line;
                    cue.mLines[i] = span;
                    i++;
                }
                addCue(cue);
            }
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "subtitle data is not UTF-8 encoded: " + e);
        } catch (IOException ioe) {
            Log.e(TAG, ioe.getMessage(), ioe);
        }
    }

    @Override
    public void updateView(Vector<SubtitleTrack.Cue> activeCues) {
        if (getRenderingWidget() != null) {
            super.updateView(activeCues);
            return;
        }
        if (this.mEventHandler == null) {
            return;
        }
        if (activeCues.isEmpty()) {
            if (DEBUG) {
                Log.d(TAG, "activeCues is Empty");
            }
            Message msg = this.mEventHandler.obtainMessage(99, 0, 0, null);
            this.mEventHandler.sendMessage(msg);
        }
        for (SubtitleTrack.Cue cue : activeCues) {
            TextTrackCue ttc = (TextTrackCue) cue;
            Parcel parcel = Parcel.obtain();
            parcel.writeInt(102);
            parcel.writeInt(7);
            parcel.writeInt((int) cue.mStartTimeMs);
            parcel.writeInt(16);
            StringBuilder sb = new StringBuilder();
            for (String line : ttc.mStrings) {
                sb.append(line).append('\n');
            }
            byte[] buf = sb.toString().getBytes();
            parcel.writeInt(buf.length);
            parcel.writeByteArray(buf);
            Message msg2 = this.mEventHandler.obtainMessage(99, 0, 0, parcel);
            this.mEventHandler.sendMessage(msg2);
        }
        activeCues.clear();
    }

    private static long parseMs(String in) {
        long hours = Long.parseLong(in.split(":")[0].trim());
        long minutes = Long.parseLong(in.split(":")[1].trim());
        long seconds = 0;
        long millies = 0;
        if (in.split(":").length == 3) {
            seconds = Long.parseLong(in.split(":")[2].split(",")[0].trim());
            millies = Long.parseLong(in.split(":")[2].split(",")[1].trim());
        } else if (in.split(":").length == 4) {
            seconds = Long.parseLong(in.split(":")[2].trim());
            millies = Long.parseLong(in.split(":")[3].trim());
        }
        return (60 * hours * 60 * 1000) + (60 * minutes * 1000) + (1000 * seconds) + millies;
    }
}
