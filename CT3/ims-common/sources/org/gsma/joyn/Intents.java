package org.gsma.joyn;

public class Intents {

    public static class Client {
        public static final String ACTION_CLIENT_GET_STATUS = ".client.action.GET_STATUS";
        public static final String ACTION_VIEW_SETTINGS = "org.gsma.joyn.action.VIEW_SETTINGS";
        public static final String EXTRA_CLIENT = "client";
        public static final String EXTRA_STATUS = "status";
        public static final String SERVICE_UP = "org.gsma.joyn.action.SERVICE_UP";

        private Client() {
        }
    }

    public static class Chat {
        public static final String ACTION_INITIATE_CHAT = "org.gsma.joyn.action.INITIATE_CHAT";
        public static final String ACTION_INITIATE_GROUP_CHAT = "org.gsma.joyn.action.INITIATE_GROUP_CHAT";
        public static final String ACTION_VIEW_CHAT = "org.gsma.joyn.action.VIEW_CHAT";
        public static final String ACTION_VIEW_GROUP_CHAT = "org.gsma.joyn.action.VIEW_GROUP_CHAT";

        private Chat() {
        }
    }

    public static class FileTransfer {
        public static final String ACTION_INITIATE_FT = "org.gsma.joyn.action.INITIATE_FT";
        public static final String ACTION_VIEW_FT = "org.gsma.joyn.action.VIEW_FT";

        private FileTransfer() {
        }
    }

    public static class IPCall {
        public static final String ACTION_INITIATE_IPCALL = "org.gsma.joyn.action.INITIATE_IPCALL";
        public static final String ACTION_VIEW_IPCALL = "org.gsma.joyn.action.VIEW_IPCALL";

        private IPCall() {
        }
    }
}
