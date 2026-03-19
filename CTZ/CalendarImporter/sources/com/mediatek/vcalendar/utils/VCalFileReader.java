package com.mediatek.vcalendar.utils;

import android.content.Context;
import android.net.Uri;
import com.android.common.speech.LoggingEvents;
import com.mediatek.vcalendar.ComponentParser;
import com.mediatek.vcalendar.component.Component;
import com.mediatek.vcalendar.component.VCalendar;
import com.mediatek.vcalendar.component.VEvent;
import com.mediatek.vcalendar.component.VTimezone;
import com.mediatek.vcalendar.property.Property;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

public class VCalFileReader {
    private static final boolean DEBUG = false;
    private static final int MARK_LENGTH = 8192;
    private static final int MART_LINE_LENGTH = 200;
    private static final String TAG = "VCalFileReader";
    private BufferedReader mBufferReader;
    private final Context mContext;
    private String mCurrentCompnentType;
    private String mFirstComponentString;
    private final Uri mUri;
    private int mComponentCount = -1;
    private final ArrayList<String> mComponentBeginLineList = new ArrayList<>();
    private final ArrayList<String> mComponentEndLineList = new ArrayList<>();

    public VCalFileReader(Uri uri, Context context) throws IOException, SecurityException {
        this.mUri = uri;
        this.mContext = context;
        VCalendar.TIMEZONE_LIST.clear();
        VCalendar.setVCalendarVersion(LoggingEvents.EXTRA_CALLING_APP_NAME);
        createBufferReader();
        this.mComponentBeginLineList.add(VEvent.VEVENT_BEGIN);
        this.mComponentEndLineList.add(VEvent.VEVENT_END);
        initSummaryAndTz();
        this.mBufferReader.close();
        createBufferReader();
    }

    public boolean hasNextComponent() throws IOException {
        String line;
        this.mBufferReader.mark(MARK_LENGTH);
        do {
            line = this.mBufferReader.readLine();
            if (line != null) {
            } else {
                this.mBufferReader.reset();
                return DEBUG;
            }
        } while (!this.mComponentBeginLineList.contains(line.toUpperCase(Locale.US)));
        this.mBufferReader.reset();
        return true;
    }

    private boolean locateNextComponent() throws IOException {
        this.mBufferReader.mark(MARK_LENGTH);
        while (true) {
            String line = this.mBufferReader.readLine();
            if (line != null) {
                String upperCase = line.toUpperCase(Locale.US);
                if (this.mComponentBeginLineList.contains(upperCase)) {
                    this.mCurrentCompnentType = upperCase.substring(upperCase.indexOf(":") + 1);
                    this.mBufferReader.reset();
                    return true;
                }
                this.mBufferReader.mark(MARK_LENGTH);
            } else {
                return DEBUG;
            }
        }
    }

    public String readNextComponent() throws IOException {
        LogUtil.d(TAG, "readNextComponent()");
        if (!locateNextComponent()) {
            LogUtil.i(TAG, "readNextComponent: findNextComponent = false, has no component yet.");
            return LoggingEvents.EXTRA_CALLING_APP_NAME;
        }
        StringBuffer stringBuffer = new StringBuffer();
        String line = this.mBufferReader.readLine();
        this.mBufferReader.mark(MART_LINE_LENGTH);
        stringBuffer.append(line);
        stringBuffer.append(Component.NEWLINE);
        while (true) {
            String line2 = this.mBufferReader.readLine();
            if (line2 != null) {
                String upperCase = line2.toUpperCase(Locale.US);
                if (this.mComponentEndLineList.contains(upperCase)) {
                    if (!upperCase.contains(this.mCurrentCompnentType)) {
                        throw new IllegalStateException("invalid format: begin with VEVENT, but end with VTODO etc.");
                    }
                    stringBuffer.append(line2);
                    stringBuffer.append(Component.NEWLINE);
                    return stringBuffer.toString();
                }
                if (this.mComponentBeginLineList.contains(upperCase)) {
                    throw new IllegalStateException("invalid format: embeded VEVENTS etc.");
                }
                stringBuffer.append(line2);
                stringBuffer.append(Component.NEWLINE);
            } else {
                LogUtil.i(TAG, "The vcs file is not well formatted!");
                return LoggingEvents.EXTRA_CALLING_APP_NAME;
            }
        }
    }

    public void close() throws IOException {
        this.mBufferReader.close();
        LogUtil.d(TAG, "close BufferReader succeed.");
    }

    public int getComponentsCount() {
        return this.mComponentCount;
    }

    public String getFirstComponent() {
        return this.mFirstComponentString;
    }

    private void createBufferReader() throws SecurityException, FileNotFoundException {
        this.mBufferReader = new BufferedReader(new InputStreamReader(this.mContext.getContentResolver().openInputStream(this.mUri)));
        LogUtil.d(TAG, "createBufferReader succeed.");
    }

    private void initSummaryAndTz() throws IOException {
        StringBuilder sb = null;
        boolean z = false;
        boolean z2 = false;
        boolean z3 = true;
        while (true) {
            String line = this.mBufferReader.readLine();
            if (line == null) {
                break;
            }
            String upperCase = line.toUpperCase(Locale.US);
            if (z3) {
                if (!line.equals(VCalendar.VCALENDAR_BEGIN)) {
                    break;
                }
                this.mComponentCount = 0;
                z3 = false;
            }
            if (upperCase.contains(Property.VERSION)) {
                VCalendar.setVCalendarVersion(upperCase);
            }
            if (upperCase.contains("TZ:")) {
                VCalendar.setV10TimeZone(upperCase.replace("TZ:", LoggingEvents.EXTRA_CALLING_APP_NAME));
                LogUtil.i(TAG, "initSummaryAndTz: sTz=" + VCalendar.getV10TimeZone());
            }
            if (this.mComponentBeginLineList.contains(upperCase)) {
                this.mComponentCount++;
                if (this.mComponentCount == 1) {
                    this.mCurrentCompnentType = upperCase.substring(upperCase.indexOf(":") + 1);
                    sb = new StringBuilder();
                    z2 = true;
                }
            }
            if (upperCase.equals(VTimezone.VTIMEZONE_BEGIN)) {
                sb = new StringBuilder();
                z = true;
            }
            if (z) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append(line);
                sb.append(Component.NEWLINE);
                if (upperCase.contains(VTimezone.VTIMEZONE_END)) {
                    Component component = ComponentParser.parse(sb.toString());
                    if (!(component instanceof VTimezone)) {
                        component = null;
                    }
                    VCalendar.TIMEZONE_LIST.add((VTimezone) component);
                    sb = null;
                    z = false;
                }
            }
            if (z2) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append(line);
                sb.append(Component.NEWLINE);
                if (this.mComponentEndLineList.contains(upperCase) && upperCase.contains(this.mCurrentCompnentType)) {
                    this.mFirstComponentString = sb.toString();
                    sb = null;
                    z2 = false;
                }
            }
        }
        LogUtil.i(TAG, "initSummaryAndTz(): the Events Count: " + this.mComponentCount);
        LogUtil.i(TAG, "initSummaryAndTz(): the Timezone Count: " + VCalendar.TIMEZONE_LIST.size());
    }
}
