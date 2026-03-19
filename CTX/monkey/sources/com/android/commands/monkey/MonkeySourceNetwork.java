package com.android.commands.monkey;

import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import com.android.commands.monkey.MonkeySourceNetworkVars;
import com.android.commands.monkey.MonkeySourceNetworkViews;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.StringTokenizer;

public class MonkeySourceNetwork implements MonkeyEventSource {
    private static final String DONE = "done";
    private static final String ERROR_STR = "ERROR";
    public static final int MONKEY_NETWORK_VERSION = 2;
    private static final String OK_STR = "OK";
    private static final String QUIT = "quit";
    private static final String TAG = "MonkeyStub";
    private static DeferredReturn deferredReturn;
    private Socket clientSocket;
    private BufferedReader input;
    private PrintWriter output;
    private ServerSocket serverSocket;
    public static final MonkeyCommandReturn OK = new MonkeyCommandReturn(true);
    public static final MonkeyCommandReturn ERROR = new MonkeyCommandReturn(false);
    public static final MonkeyCommandReturn EARG = new MonkeyCommandReturn(false, "Invalid Argument");
    private static final Map<String, MonkeyCommand> COMMAND_MAP = new HashMap();
    private final CommandQueueImpl commandQueue = new CommandQueueImpl();
    private boolean started = false;

    public interface CommandQueue {
        void enqueueEvent(MonkeyEvent monkeyEvent);
    }

    public interface MonkeyCommand {
        MonkeyCommandReturn translateCommand(List<String> list, CommandQueue commandQueue);
    }

    public static class MonkeyCommandReturn {
        private final String message;
        private final boolean success;

        public MonkeyCommandReturn(boolean z) {
            this.success = z;
            this.message = null;
        }

        public MonkeyCommandReturn(boolean z, String str) {
            this.success = z;
            this.message = str;
        }

        boolean hasMessage() {
            return this.message != null;
        }

        String getMessage() {
            return this.message;
        }

        boolean wasSuccessful() {
            return this.success;
        }
    }

    static {
        COMMAND_MAP.put("flip", new FlipCommand());
        COMMAND_MAP.put("touch", new TouchCommand());
        COMMAND_MAP.put("trackball", new TrackballCommand());
        COMMAND_MAP.put("key", new KeyCommand());
        COMMAND_MAP.put("sleep", new SleepCommand());
        COMMAND_MAP.put("wake", new WakeCommand());
        COMMAND_MAP.put("tap", new TapCommand());
        COMMAND_MAP.put("press", new PressCommand());
        COMMAND_MAP.put("type", new TypeCommand());
        COMMAND_MAP.put("listvar", new MonkeySourceNetworkVars.ListVarCommand());
        COMMAND_MAP.put("getvar", new MonkeySourceNetworkVars.GetVarCommand());
        COMMAND_MAP.put("listviews", new MonkeySourceNetworkViews.ListViewsCommand());
        COMMAND_MAP.put("queryview", new MonkeySourceNetworkViews.QueryViewCommand());
        COMMAND_MAP.put("getrootview", new MonkeySourceNetworkViews.GetRootViewCommand());
        COMMAND_MAP.put("getviewswithtext", new MonkeySourceNetworkViews.GetViewsWithTextCommand());
        COMMAND_MAP.put("deferreturn", new DeferReturnCommand());
    }

    private static class FlipCommand implements MonkeyCommand {
        private FlipCommand() {
        }

        @Override
        public MonkeyCommandReturn translateCommand(List<String> list, CommandQueue commandQueue) {
            if (list.size() > 1) {
                String str = list.get(1);
                if ("open".equals(str)) {
                    commandQueue.enqueueEvent(new MonkeyFlipEvent(true));
                    return MonkeySourceNetwork.OK;
                }
                if ("close".equals(str)) {
                    commandQueue.enqueueEvent(new MonkeyFlipEvent(false));
                    return MonkeySourceNetwork.OK;
                }
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    private static class TouchCommand implements MonkeyCommand {
        private TouchCommand() {
        }

        @Override
        public MonkeyCommandReturn translateCommand(List<String> list, CommandQueue commandQueue) {
            if (list.size() == 4) {
                int i = 1;
                String str = list.get(1);
                try {
                    int i2 = Integer.parseInt(list.get(2));
                    int i3 = Integer.parseInt(list.get(3));
                    if (!"down".equals(str)) {
                        if (!"up".equals(str)) {
                            i = "move".equals(str) ? 2 : -1;
                        }
                    } else {
                        i = 0;
                    }
                    if (i == -1) {
                        Log.e(MonkeySourceNetwork.TAG, "Got a bad action: " + str);
                        return MonkeySourceNetwork.EARG;
                    }
                    commandQueue.enqueueEvent(new MonkeyTouchEvent(i).addPointer(0, i2, i3));
                    return MonkeySourceNetwork.OK;
                } catch (NumberFormatException e) {
                    Log.e(MonkeySourceNetwork.TAG, "Got something that wasn't a number", e);
                    return MonkeySourceNetwork.EARG;
                }
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    private static class TrackballCommand implements MonkeyCommand {
        private TrackballCommand() {
        }

        @Override
        public MonkeyCommandReturn translateCommand(List<String> list, CommandQueue commandQueue) {
            if (list.size() == 3) {
                try {
                    commandQueue.enqueueEvent(new MonkeyTrackballEvent(2).addPointer(0, Integer.parseInt(list.get(1)), Integer.parseInt(list.get(2))));
                    return MonkeySourceNetwork.OK;
                } catch (NumberFormatException e) {
                    Log.e(MonkeySourceNetwork.TAG, "Got something that wasn't a number", e);
                    return MonkeySourceNetwork.EARG;
                }
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    private static class KeyCommand implements MonkeyCommand {
        private KeyCommand() {
        }

        @Override
        public MonkeyCommandReturn translateCommand(List<String> list, CommandQueue commandQueue) {
            if (list.size() == 3) {
                int keyCode = MonkeySourceNetwork.getKeyCode(list.get(2));
                if (keyCode < 0) {
                    Log.e(MonkeySourceNetwork.TAG, "Can't find keyname: " + list.get(2));
                    return MonkeySourceNetwork.EARG;
                }
                Log.d(MonkeySourceNetwork.TAG, "keycode: " + keyCode);
                int i = 1;
                if ("down".equals(list.get(1))) {
                    i = 0;
                } else if (!"up".equals(list.get(1))) {
                    i = -1;
                }
                if (i == -1) {
                    Log.e(MonkeySourceNetwork.TAG, "got unknown action.");
                    return MonkeySourceNetwork.EARG;
                }
                commandQueue.enqueueEvent(new MonkeyKeyEvent(i, keyCode));
                return MonkeySourceNetwork.OK;
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    private static int getKeyCode(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            int keyCode = MonkeySourceRandom.getKeyCode(str);
            if (keyCode == 0) {
                int keyCode2 = MonkeySourceRandom.getKeyCode("KEYCODE_" + str.toUpperCase());
                if (keyCode2 == 0) {
                    return -1;
                }
                return keyCode2;
            }
            return keyCode;
        }
    }

    private static class SleepCommand implements MonkeyCommand {
        private SleepCommand() {
        }

        @Override
        public MonkeyCommandReturn translateCommand(List<String> list, CommandQueue commandQueue) {
            if (list.size() == 2) {
                String str = list.get(1);
                try {
                    commandQueue.enqueueEvent(new MonkeyThrottleEvent(Integer.parseInt(str)));
                    return MonkeySourceNetwork.OK;
                } catch (NumberFormatException e) {
                    Log.e(MonkeySourceNetwork.TAG, "Not a number: " + str, e);
                    return MonkeySourceNetwork.EARG;
                }
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    private static class TypeCommand implements MonkeyCommand {
        private TypeCommand() {
        }

        @Override
        public MonkeyCommandReturn translateCommand(List<String> list, CommandQueue commandQueue) {
            if (list.size() == 2) {
                for (KeyEvent keyEvent : KeyCharacterMap.load(-1).getEvents(list.get(1).toString().toCharArray())) {
                    commandQueue.enqueueEvent(new MonkeyKeyEvent(keyEvent));
                }
                return MonkeySourceNetwork.OK;
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    private static class WakeCommand implements MonkeyCommand {
        private WakeCommand() {
        }

        @Override
        public MonkeyCommandReturn translateCommand(List<String> list, CommandQueue commandQueue) {
            if (!MonkeySourceNetwork.wake()) {
                return MonkeySourceNetwork.ERROR;
            }
            return MonkeySourceNetwork.OK;
        }
    }

    private static class TapCommand implements MonkeyCommand {
        private TapCommand() {
        }

        @Override
        public MonkeyCommandReturn translateCommand(List<String> list, CommandQueue commandQueue) {
            if (list.size() == 3) {
                try {
                    float f = Integer.parseInt(list.get(1));
                    float f2 = Integer.parseInt(list.get(2));
                    commandQueue.enqueueEvent(new MonkeyTouchEvent(0).addPointer(0, f, f2));
                    commandQueue.enqueueEvent(new MonkeyTouchEvent(1).addPointer(0, f, f2));
                    return MonkeySourceNetwork.OK;
                } catch (NumberFormatException e) {
                    Log.e(MonkeySourceNetwork.TAG, "Got something that wasn't a number", e);
                    return MonkeySourceNetwork.EARG;
                }
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    private static class PressCommand implements MonkeyCommand {
        private PressCommand() {
        }

        @Override
        public MonkeyCommandReturn translateCommand(List<String> list, CommandQueue commandQueue) {
            if (list.size() == 2) {
                int keyCode = MonkeySourceNetwork.getKeyCode(list.get(1));
                if (keyCode < 0) {
                    Log.e(MonkeySourceNetwork.TAG, "Can't find keyname: " + list.get(1));
                    return MonkeySourceNetwork.EARG;
                }
                commandQueue.enqueueEvent(new MonkeyKeyEvent(0, keyCode));
                commandQueue.enqueueEvent(new MonkeyKeyEvent(1, keyCode));
                return MonkeySourceNetwork.OK;
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    private static class DeferReturnCommand implements MonkeyCommand {
        private DeferReturnCommand() {
        }

        @Override
        public MonkeyCommandReturn translateCommand(List<String> list, CommandQueue commandQueue) {
            if (list.size() > 3) {
                if (!list.get(1).equals("screenchange")) {
                    return MonkeySourceNetwork.EARG;
                }
                long j = Long.parseLong(list.get(2));
                MonkeyCommand monkeyCommand = (MonkeyCommand) MonkeySourceNetwork.COMMAND_MAP.get(list.get(3));
                if (monkeyCommand != null) {
                    DeferredReturn unused = MonkeySourceNetwork.deferredReturn = new DeferredReturn(1, monkeyCommand.translateCommand(list.subList(3, list.size()), commandQueue), j);
                    return MonkeySourceNetwork.OK;
                }
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    private static final boolean wake() {
        try {
            IPowerManager.Stub.asInterface(ServiceManager.getService("power")).wakeUp(SystemClock.uptimeMillis(), "Monkey", (String) null);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Got remote exception", e);
            return false;
        }
    }

    private static class CommandQueueImpl implements CommandQueue {
        private final Queue<MonkeyEvent> queuedEvents;

        private CommandQueueImpl() {
            this.queuedEvents = new LinkedList();
        }

        @Override
        public void enqueueEvent(MonkeyEvent monkeyEvent) {
            this.queuedEvents.offer(monkeyEvent);
        }

        public MonkeyEvent getNextQueuedEvent() {
            return this.queuedEvents.poll();
        }
    }

    private static class DeferredReturn {
        public static final int ON_WINDOW_STATE_CHANGE = 1;
        private MonkeyCommandReturn deferredReturn;
        private int event;
        private long timeout;

        public DeferredReturn(int i, MonkeyCommandReturn monkeyCommandReturn, long j) {
            this.event = i;
            this.deferredReturn = monkeyCommandReturn;
            this.timeout = j;
        }

        public MonkeyCommandReturn waitForEvent() {
            if (this.event == 1) {
                try {
                    synchronized (MonkeySourceNetworkViews.class) {
                        MonkeySourceNetworkViews.class.wait(this.timeout);
                    }
                } catch (InterruptedException e) {
                    Log.d(MonkeySourceNetwork.TAG, "Deferral interrupted: " + e.getMessage());
                }
            }
            return this.deferredReturn;
        }
    }

    public MonkeySourceNetwork(int i) throws IOException {
        this.serverSocket = new ServerSocket(i, 0, InetAddress.getLocalHost());
    }

    private void startServer() throws IOException {
        this.clientSocket = this.serverSocket.accept();
        MonkeySourceNetworkViews.setup();
        wake();
        this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
        this.output = new PrintWriter(this.clientSocket.getOutputStream(), true);
    }

    private void stopServer() throws IOException {
        this.clientSocket.close();
        MonkeySourceNetworkViews.teardown();
        this.input.close();
        this.output.close();
        this.started = false;
    }

    private static String replaceQuotedChars(String str) {
        return str.replace("\\\"", "\"");
    }

    private static List<String> commandLineSplit(String str) {
        ArrayList arrayList = new ArrayList();
        StringTokenizer stringTokenizer = new StringTokenizer(str);
        StringBuffer stringBuffer = new StringBuffer();
        boolean z = false;
        while (stringTokenizer.hasMoreTokens()) {
            String strNextToken = stringTokenizer.nextToken();
            if (!z && strNextToken.startsWith("\"")) {
                stringBuffer.append(replaceQuotedChars(strNextToken));
                z = true;
            } else if (z) {
                if (strNextToken.endsWith("\"")) {
                    stringBuffer.append(" ");
                    stringBuffer.append(replaceQuotedChars(strNextToken));
                    String string = stringBuffer.toString();
                    arrayList.add(string.substring(1, string.length() - 1));
                    z = false;
                } else {
                    stringBuffer.append(" ");
                    stringBuffer.append(replaceQuotedChars(strNextToken));
                }
            } else {
                arrayList.add(replaceQuotedChars(strNextToken));
            }
        }
        return arrayList;
    }

    private void translateCommand(String str) {
        MonkeyCommand monkeyCommand;
        Log.d(TAG, "translateCommand: " + str);
        List<String> listCommandLineSplit = commandLineSplit(str);
        if (listCommandLineSplit.size() > 0 && (monkeyCommand = COMMAND_MAP.get(listCommandLineSplit.get(0))) != null) {
            handleReturn(monkeyCommand.translateCommand(listCommandLineSplit, this.commandQueue));
        }
    }

    private void handleReturn(MonkeyCommandReturn monkeyCommandReturn) {
        if (monkeyCommandReturn.wasSuccessful()) {
            if (monkeyCommandReturn.hasMessage()) {
                returnOk(monkeyCommandReturn.getMessage());
                return;
            } else {
                returnOk();
                return;
            }
        }
        if (monkeyCommandReturn.hasMessage()) {
            returnError(monkeyCommandReturn.getMessage());
        } else {
            returnError();
        }
    }

    @Override
    public MonkeyEvent getNextEvent() {
        if (!this.started) {
            try {
                startServer();
                this.started = true;
            } catch (IOException e) {
                Log.e(TAG, "Got IOException from server", e);
                return null;
            }
        }
        while (true) {
            try {
                MonkeyEvent nextQueuedEvent = this.commandQueue.getNextQueuedEvent();
                if (nextQueuedEvent != null) {
                    return nextQueuedEvent;
                }
                if (deferredReturn != null) {
                    Log.d(TAG, "Waiting for event");
                    MonkeyCommandReturn monkeyCommandReturnWaitForEvent = deferredReturn.waitForEvent();
                    deferredReturn = null;
                    handleReturn(monkeyCommandReturnWaitForEvent);
                }
                String line = this.input.readLine();
                if (line == null) {
                    Log.d(TAG, "Connection dropped.");
                    line = DONE;
                }
                if (DONE.equals(line)) {
                    try {
                        stopServer();
                        return new MonkeyNoopEvent();
                    } catch (IOException e2) {
                        Log.e(TAG, "Got IOException shutting down!", e2);
                        return null;
                    }
                }
                if (QUIT.equals(line)) {
                    Log.d(TAG, "Quit requested");
                    returnOk();
                    return null;
                }
                if (!line.startsWith("#")) {
                    translateCommand(line);
                }
            } catch (IOException e3) {
                Log.e(TAG, "Exception: ", e3);
                return null;
            }
        }
    }

    private void returnError() {
        this.output.println(ERROR_STR);
    }

    private void returnError(String str) {
        this.output.print(ERROR_STR);
        this.output.print(":");
        this.output.println(str);
    }

    private void returnOk() {
        this.output.println(OK_STR);
    }

    private void returnOk(String str) {
        this.output.print(OK_STR);
        this.output.print(":");
        this.output.println(str);
    }

    @Override
    public void setVerbose(int i) {
    }

    @Override
    public boolean validate() {
        return true;
    }
}
