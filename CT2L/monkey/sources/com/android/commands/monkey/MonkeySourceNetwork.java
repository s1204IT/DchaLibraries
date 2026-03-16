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

        public MonkeyCommandReturn(boolean success) {
            this.success = success;
            this.message = null;
        }

        public MonkeyCommandReturn(boolean success, String message) {
            this.success = success;
            this.message = message;
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
        public MonkeyCommandReturn translateCommand(List<String> command, CommandQueue queue) {
            if (command.size() > 1) {
                String direction = command.get(1);
                if ("open".equals(direction)) {
                    queue.enqueueEvent(new MonkeyFlipEvent(true));
                    return MonkeySourceNetwork.OK;
                }
                if ("close".equals(direction)) {
                    queue.enqueueEvent(new MonkeyFlipEvent(false));
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
        public MonkeyCommandReturn translateCommand(List<String> command, CommandQueue queue) {
            if (command.size() == 4) {
                String actionName = command.get(1);
                try {
                    int x = Integer.parseInt(command.get(2));
                    int y = Integer.parseInt(command.get(3));
                    int action = -1;
                    if ("down".equals(actionName)) {
                        action = 0;
                    } else if ("up".equals(actionName)) {
                        action = 1;
                    } else if ("move".equals(actionName)) {
                        action = 2;
                    }
                    if (action == -1) {
                        Log.e(MonkeySourceNetwork.TAG, "Got a bad action: " + actionName);
                        return MonkeySourceNetwork.EARG;
                    }
                    queue.enqueueEvent(new MonkeyTouchEvent(action).addPointer(0, x, y));
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
        public MonkeyCommandReturn translateCommand(List<String> command, CommandQueue queue) {
            if (command.size() == 3) {
                try {
                    int dx = Integer.parseInt(command.get(1));
                    int dy = Integer.parseInt(command.get(2));
                    queue.enqueueEvent(new MonkeyTrackballEvent(2).addPointer(0, dx, dy));
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
        public MonkeyCommandReturn translateCommand(List<String> command, CommandQueue queue) {
            if (command.size() == 3) {
                int keyCode = MonkeySourceNetwork.getKeyCode(command.get(2));
                if (keyCode < 0) {
                    Log.e(MonkeySourceNetwork.TAG, "Can't find keyname: " + command.get(2));
                    return MonkeySourceNetwork.EARG;
                }
                Log.d(MonkeySourceNetwork.TAG, "keycode: " + keyCode);
                int action = -1;
                if ("down".equals(command.get(1))) {
                    action = 0;
                } else if ("up".equals(command.get(1))) {
                    action = 1;
                }
                if (action == -1) {
                    Log.e(MonkeySourceNetwork.TAG, "got unknown action.");
                    return MonkeySourceNetwork.EARG;
                }
                queue.enqueueEvent(new MonkeyKeyEvent(action, keyCode));
                return MonkeySourceNetwork.OK;
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    private static int getKeyCode(String keyName) {
        int keyCode;
        try {
            keyCode = Integer.parseInt(keyName);
        } catch (NumberFormatException e) {
            keyCode = MonkeySourceRandom.getKeyCode(keyName);
            if (keyCode == 0 && (keyCode = MonkeySourceRandom.getKeyCode("KEYCODE_" + keyName.toUpperCase())) == 0) {
                return -1;
            }
        }
        return keyCode;
    }

    private static class SleepCommand implements MonkeyCommand {
        private SleepCommand() {
        }

        @Override
        public MonkeyCommandReturn translateCommand(List<String> command, CommandQueue queue) {
            if (command.size() == 2) {
                String sleepStr = command.get(1);
                try {
                    int sleep = Integer.parseInt(sleepStr);
                    queue.enqueueEvent(new MonkeyThrottleEvent(sleep));
                    return MonkeySourceNetwork.OK;
                } catch (NumberFormatException e) {
                    Log.e(MonkeySourceNetwork.TAG, "Not a number: " + sleepStr, e);
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
        public MonkeyCommandReturn translateCommand(List<String> command, CommandQueue queue) {
            if (command.size() != 2) {
                return MonkeySourceNetwork.EARG;
            }
            String str = command.get(1);
            char[] chars = str.toString().toCharArray();
            KeyCharacterMap keyCharacterMap = KeyCharacterMap.load(-1);
            KeyEvent[] events = keyCharacterMap.getEvents(chars);
            for (KeyEvent event : events) {
                queue.enqueueEvent(new MonkeyKeyEvent(event));
            }
            return MonkeySourceNetwork.OK;
        }
    }

    private static class WakeCommand implements MonkeyCommand {
        private WakeCommand() {
        }

        @Override
        public MonkeyCommandReturn translateCommand(List<String> command, CommandQueue queue) {
            return !MonkeySourceNetwork.wake() ? MonkeySourceNetwork.ERROR : MonkeySourceNetwork.OK;
        }
    }

    private static class TapCommand implements MonkeyCommand {
        private TapCommand() {
        }

        @Override
        public MonkeyCommandReturn translateCommand(List<String> command, CommandQueue queue) {
            if (command.size() == 3) {
                try {
                    int x = Integer.parseInt(command.get(1));
                    int y = Integer.parseInt(command.get(2));
                    queue.enqueueEvent(new MonkeyTouchEvent(0).addPointer(0, x, y));
                    queue.enqueueEvent(new MonkeyTouchEvent(1).addPointer(0, x, y));
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
        public MonkeyCommandReturn translateCommand(List<String> command, CommandQueue queue) {
            if (command.size() == 2) {
                int keyCode = MonkeySourceNetwork.getKeyCode(command.get(1));
                if (keyCode < 0) {
                    Log.e(MonkeySourceNetwork.TAG, "Can't find keyname: " + command.get(1));
                    return MonkeySourceNetwork.EARG;
                }
                queue.enqueueEvent(new MonkeyKeyEvent(0, keyCode));
                queue.enqueueEvent(new MonkeyKeyEvent(1, keyCode));
                return MonkeySourceNetwork.OK;
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    private static class DeferReturnCommand implements MonkeyCommand {
        private DeferReturnCommand() {
        }

        @Override
        public MonkeyCommandReturn translateCommand(List<String> command, CommandQueue queue) {
            if (command.size() > 3) {
                String event = command.get(1);
                if (!event.equals("screenchange")) {
                    return MonkeySourceNetwork.EARG;
                }
                long timeout = Long.parseLong(command.get(2));
                MonkeyCommand deferredCommand = (MonkeyCommand) MonkeySourceNetwork.COMMAND_MAP.get(command.get(3));
                if (deferredCommand != null) {
                    List<String> parts = command.subList(3, command.size());
                    MonkeyCommandReturn ret = deferredCommand.translateCommand(parts, queue);
                    DeferredReturn unused = MonkeySourceNetwork.deferredReturn = new DeferredReturn(1, ret, timeout);
                    return MonkeySourceNetwork.OK;
                }
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    private static final boolean wake() {
        IPowerManager pm = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
        try {
            pm.wakeUp(SystemClock.uptimeMillis());
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
        public void enqueueEvent(MonkeyEvent e) {
            this.queuedEvents.offer(e);
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

        public DeferredReturn(int event, MonkeyCommandReturn deferredReturn, long timeout) {
            this.event = event;
            this.deferredReturn = deferredReturn;
            this.timeout = timeout;
        }

        public MonkeyCommandReturn waitForEvent() {
            switch (this.event) {
                case 1:
                    try {
                        synchronized (MonkeySourceNetworkViews.class) {
                            MonkeySourceNetworkViews.class.wait(this.timeout);
                            break;
                        }
                    } catch (InterruptedException e) {
                        Log.d(MonkeySourceNetwork.TAG, "Deferral interrupted: " + e.getMessage());
                    }
                    break;
            }
            return this.deferredReturn;
        }
    }

    public MonkeySourceNetwork(int port) throws IOException {
        this.serverSocket = new ServerSocket(port, 0, InetAddress.getLocalHost());
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

    private static String replaceQuotedChars(String input) {
        return input.replace("\\\"", "\"");
    }

    private static List<String> commandLineSplit(String line) {
        ArrayList<String> result = new ArrayList<>();
        StringTokenizer tok = new StringTokenizer(line);
        boolean insideQuote = false;
        StringBuffer quotedWord = new StringBuffer();
        while (tok.hasMoreTokens()) {
            String cur = tok.nextToken();
            if (!insideQuote && cur.startsWith("\"")) {
                quotedWord.append(replaceQuotedChars(cur));
                insideQuote = true;
            } else if (insideQuote) {
                if (cur.endsWith("\"")) {
                    insideQuote = false;
                    quotedWord.append(" ").append(replaceQuotedChars(cur));
                    String word = quotedWord.toString();
                    result.add(word.substring(1, word.length() - 1));
                } else {
                    quotedWord.append(" ").append(replaceQuotedChars(cur));
                }
            } else {
                result.add(replaceQuotedChars(cur));
            }
        }
        return result;
    }

    private void translateCommand(String commandLine) {
        MonkeyCommand command;
        Log.d(TAG, "translateCommand: " + commandLine);
        List<String> parts = commandLineSplit(commandLine);
        if (parts.size() > 0 && (command = COMMAND_MAP.get(parts.get(0))) != null) {
            MonkeyCommandReturn ret = command.translateCommand(parts, this.commandQueue);
            handleReturn(ret);
        }
    }

    private void handleReturn(MonkeyCommandReturn ret) {
        if (ret.wasSuccessful()) {
            if (ret.hasMessage()) {
                returnOk(ret.getMessage());
                return;
            } else {
                returnOk();
                return;
            }
        }
        if (ret.hasMessage()) {
            returnError(ret.getMessage());
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
                MonkeyEvent queuedEvent = this.commandQueue.getNextQueuedEvent();
                if (queuedEvent == null) {
                    if (deferredReturn != null) {
                        Log.d(TAG, "Waiting for event");
                        MonkeyCommandReturn ret = deferredReturn.waitForEvent();
                        deferredReturn = null;
                        handleReturn(ret);
                    }
                    String command = this.input.readLine();
                    if (command == null) {
                        Log.d(TAG, "Connection dropped.");
                        command = DONE;
                    }
                    if (DONE.equals(command)) {
                        try {
                            stopServer();
                            return new MonkeyNoopEvent();
                        } catch (IOException e2) {
                            Log.e(TAG, "Got IOException shutting down!", e2);
                            return null;
                        }
                    }
                    if (QUIT.equals(command)) {
                        Log.d(TAG, "Quit requested");
                        returnOk();
                        return null;
                    }
                    if (!command.startsWith("#")) {
                        translateCommand(command);
                    }
                } else {
                    return queuedEvent;
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

    private void returnError(String msg) {
        this.output.print(ERROR_STR);
        this.output.print(":");
        this.output.println(msg);
    }

    private void returnOk() {
        this.output.println(OK_STR);
    }

    private void returnOk(String returnValue) {
        this.output.print(OK_STR);
        this.output.print(":");
        this.output.println(returnValue);
    }

    @Override
    public void setVerbose(int verbose) {
    }

    @Override
    public boolean validate() {
        return true;
    }
}
