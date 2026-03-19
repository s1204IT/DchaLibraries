package com.android.commands.hid;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

public class Event {
    public static final String COMMAND_DELAY = "delay";
    public static final String COMMAND_REGISTER = "register";
    public static final String COMMAND_REPORT = "report";
    private static final String TAG = "HidEvent";
    private String mCommand;
    private byte[] mDescriptor;
    private int mDuration;
    private int mId;
    private String mName;
    private int mPid;
    private byte[] mReport;
    private int mVid;

    public int getId() {
        return this.mId;
    }

    public String getCommand() {
        return this.mCommand;
    }

    public String getName() {
        return this.mName;
    }

    public byte[] getDescriptor() {
        return this.mDescriptor;
    }

    public int getVendorId() {
        return this.mVid;
    }

    public int getProductId() {
        return this.mPid;
    }

    public byte[] getReport() {
        return this.mReport;
    }

    public int getDuration() {
        return this.mDuration;
    }

    public String toString() {
        return "Event{id=" + this.mId + ", command=" + String.valueOf(this.mCommand) + ", name=" + String.valueOf(this.mName) + ", descriptor=" + Arrays.toString(this.mDescriptor) + ", vid=" + this.mVid + ", pid=" + this.mPid + ", report=" + Arrays.toString(this.mReport) + ", duration=" + this.mDuration + "}";
    }

    private static class Builder {
        private Event mEvent = new Event();

        public void setId(int i) {
            this.mEvent.mId = i;
        }

        private void setCommand(String str) {
            this.mEvent.mCommand = str;
        }

        public void setName(String str) {
            this.mEvent.mName = str;
        }

        public void setDescriptor(byte[] bArr) {
            this.mEvent.mDescriptor = bArr;
        }

        public void setReport(byte[] bArr) {
            this.mEvent.mReport = bArr;
        }

        public void setVid(int i) {
            this.mEvent.mVid = i;
        }

        public void setPid(int i) {
            this.mEvent.mPid = i;
        }

        public void setDuration(int i) {
            this.mEvent.mDuration = i;
        }

        public Event build() {
            if (this.mEvent.mId != -1) {
                if (this.mEvent.mCommand == null) {
                    throw new IllegalStateException("Event does not contain a command");
                }
                if (Event.COMMAND_REGISTER.equals(this.mEvent.mCommand)) {
                    if (this.mEvent.mDescriptor == null) {
                        throw new IllegalStateException("Device registration is missing descriptor");
                    }
                } else if (Event.COMMAND_DELAY.equals(this.mEvent.mCommand)) {
                    if (this.mEvent.mDuration <= 0) {
                        throw new IllegalStateException("Delay has missing or invalid duration");
                    }
                } else if (Event.COMMAND_REPORT.equals(this.mEvent.mCommand) && this.mEvent.mReport == null) {
                    throw new IllegalStateException("Report command is missing report data");
                }
                return this.mEvent;
            }
            throw new IllegalStateException("No event id");
        }
    }

    public static class Reader {
        private JsonReader mReader;

        public Reader(InputStreamReader inputStreamReader) {
            this.mReader = new JsonReader(inputStreamReader);
            this.mReader.setLenient(true);
        }

        public Event getNextEvent() throws IOException {
            Event eventBuild = null;
            while (eventBuild == null && this.mReader.peek() != JsonToken.END_DOCUMENT) {
                Builder builder = new Builder();
                try {
                    this.mReader.beginObject();
                    while (this.mReader.hasNext()) {
                        switch (this.mReader.nextName()) {
                            case "id":
                                builder.setId(readInt());
                                break;
                            case "command":
                                builder.setCommand(this.mReader.nextString());
                                break;
                            case "descriptor":
                                builder.setDescriptor(readData());
                                break;
                            case "name":
                                builder.setName(this.mReader.nextString());
                                break;
                            case "vid":
                                builder.setVid(readInt());
                                break;
                            case "pid":
                                builder.setPid(readInt());
                                break;
                            case "report":
                                builder.setReport(readData());
                                break;
                            case "duration":
                                builder.setDuration(readInt());
                                break;
                            default:
                                this.mReader.skipValue();
                                break;
                        }
                    }
                    this.mReader.endObject();
                    eventBuild = builder.build();
                } catch (IllegalStateException e) {
                    Event.error("Error reading in object, ignoring.", e);
                    consumeRemainingElements();
                    this.mReader.endObject();
                }
            }
            return eventBuild;
        }

        private byte[] readData() throws IOException {
            ArrayList arrayList = new ArrayList();
            try {
                this.mReader.beginArray();
                while (this.mReader.hasNext()) {
                    arrayList.add(Integer.decode(this.mReader.nextString()));
                }
                this.mReader.endArray();
                byte[] bArr = new byte[arrayList.size()];
                for (int i = 0; i < arrayList.size(); i++) {
                    int iIntValue = ((Integer) arrayList.get(i)).intValue();
                    if ((iIntValue & 255) != iIntValue) {
                        throw new IllegalStateException("Invalid data, all values must be byte-sized");
                    }
                    bArr[i] = (byte) iIntValue;
                }
                return bArr;
            } catch (IllegalStateException | NumberFormatException e) {
                consumeRemainingElements();
                this.mReader.endArray();
                throw new IllegalStateException("Encountered malformed data.", e);
            }
        }

        private int readInt() throws IOException {
            return Integer.decode(this.mReader.nextString()).intValue();
        }

        private void consumeRemainingElements() throws IOException {
            while (this.mReader.hasNext()) {
                this.mReader.skipValue();
            }
        }
    }

    private static void error(String str) {
        error(str, null);
    }

    private static void error(String str, Exception exc) {
        System.out.println(str);
        Log.e(TAG, str);
        if (exc != null) {
            Log.e(TAG, Log.getStackTraceString(exc));
        }
    }
}
