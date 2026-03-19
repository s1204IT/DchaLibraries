package mediatek.telecom;

import android.os.Bundle;
import java.util.ArrayList;

public class MtkTelecomHelper {

    public static class MtkInCallServiceHelper {
        private static final String ACTION_BLIND_OR_ASSURED_ECT = "blindAssuredEct";
        private static final String ACTION_CANCEL_DEVICE_SWITCH = "canceldeviceswitch";
        private static final String ACTION_DEVICE_SWITCH = "deviceswitch";
        private static final String ACTION_EXPLICIT_CALL_TRANSFER = "explicitCallTransfer";
        private static final String ACTION_HANGUP_ACTIVE_AND_ANSWER_WAITING = "hangupActiveAndAnswerWaiting";
        private static final String ACTION_HANGUP_ALL = "hangupAll";
        private static final String ACTION_HANGUP_HOLD = "hangupAllHold";
        private static final String ACTION_INVITE_CONFERENCE_PARTICIPANTS = "inviteConferenceParticipants";
        private static final String ACTION_SET_SORTED_INCOMING_CALL_LIST = "setSortedIncomingCallList";
        private static final String ACTION_START_VOICE_RECORDING = "startVoiceRecording";
        private static final String ACTION_STOP_VOICE_RECORDING = "stopVoiceRecording";
        private static final String KEY_ACTION = "key_action";
        private static final String PARAM_INT_TYPE = "param_int_type";
        private static final String PARAM_STRING_ARRAY_CALL_IDS = "param_string_array_call_ids";
        private static final String PARAM_STRING_ARRAY_LIST_NUMBERS = "param_string_array_list_numbers";
        private static final String PARAM_STRING_CALL_ID = "param_string_call_id";
        private static final String PARAM_STRING_DEVICE_ID = "param_string_device_id";
        private static final String PARAM_STRING_PHONE_NUMBER = "param_string_phone_number";

        public static Bundle buildParamsForHangupHold() {
            return obtainBuilder(ACTION_HANGUP_HOLD).build();
        }

        public static Bundle buildParamsForHangupAll() {
            return obtainBuilder(ACTION_HANGUP_ALL).build();
        }

        public static Bundle buildParamsForHangupActiveAndAnswerWaiting() {
            return obtainBuilder(ACTION_HANGUP_ACTIVE_AND_ANSWER_WAITING).build();
        }

        public static Bundle buildParamsForExplicitCallTransfer(String str) {
            return obtainBuilder(ACTION_EXPLICIT_CALL_TRANSFER).putStringParam(PARAM_STRING_CALL_ID, str).build();
        }

        public static Bundle buildParamsForInviteConferenceParticipants(String str, ArrayList<String> arrayList) {
            return obtainBuilder(ACTION_INVITE_CONFERENCE_PARTICIPANTS).putStringParam(PARAM_STRING_CALL_ID, str).putStringArrayListParam(PARAM_STRING_ARRAY_LIST_NUMBERS, arrayList).build();
        }

        public static Bundle buildParamsForSetSortedIncomingCallList(ArrayList<String> arrayList) {
            return obtainBuilder(ACTION_SET_SORTED_INCOMING_CALL_LIST).putStringArrayListParam(PARAM_STRING_ARRAY_CALL_IDS, arrayList).build();
        }

        public static Bundle buildParamsForBlindOrAssuredEct(String str, String str2, int i) {
            return obtainBuilder(ACTION_BLIND_OR_ASSURED_ECT).putStringParam(PARAM_STRING_CALL_ID, str).putStringParam(PARAM_STRING_PHONE_NUMBER, str2).putIntParam(PARAM_INT_TYPE, i).build();
        }

        public static Bundle buildParamsForStartVoiceRecording() {
            return obtainBuilder(ACTION_START_VOICE_RECORDING).build();
        }

        public static Bundle buildParamsForStopVoiceRecording() {
            return obtainBuilder(ACTION_STOP_VOICE_RECORDING).build();
        }

        public static Bundle buildParamsForDeviceSwitch(String str, String str2, String str3) {
            return obtainBuilder(ACTION_DEVICE_SWITCH).putStringParam(PARAM_STRING_CALL_ID, str).putStringParam(PARAM_STRING_PHONE_NUMBER, str2).putStringParam(PARAM_STRING_DEVICE_ID, str3).build();
        }

        public static Bundle buildParamsForCancelDeviceSwitch(String str) {
            return obtainBuilder(ACTION_CANCEL_DEVICE_SWITCH).putStringParam(PARAM_STRING_CALL_ID, str).build();
        }

        private static Builder obtainBuilder(String str) {
            return new Builder(str);
        }

        private static class Builder {
            Bundle mBundle = new Bundle();

            Builder(String str) {
                this.mBundle.putString(MtkInCallServiceHelper.KEY_ACTION, str);
            }

            Builder putStringParam(String str, String str2) {
                this.mBundle.putString(str, str2);
                return this;
            }

            Builder putStringArrayListParam(String str, ArrayList<String> arrayList) {
                this.mBundle.putStringArrayList(str, arrayList);
                return this;
            }

            Builder putIntParam(String str, int i) {
                this.mBundle.putInt(str, i);
                return this;
            }

            Bundle build() {
                return this.mBundle;
            }
        }
    }

    public static class MtkInCallAdapterHelper {

        public interface ICommandProcessor {
            void blindOrAssuredEct(String str, String str2, int i);

            void cancelDeviceSwitch(String str);

            void deviceSwitch(String str, String str2, String str3);

            void explicitCallTransfer(String str);

            void hangupActiveAndAnswerWaiting();

            void hangupAll();

            void hangupHold();

            void inviteConferenceParticipants(String str, ArrayList<String> arrayList);

            void setSortedIncomingCallList(ArrayList<String> arrayList);

            void startVoiceRecording();

            void stopVoiceRecording();
        }

        public static void handleExtCommand(Bundle bundle, ICommandProcessor iCommandProcessor) {
            switch (bundle.getString("key_action", "")) {
                case "hangupAll":
                    iCommandProcessor.hangupAll();
                    break;
                case "explicitCallTransfer":
                    iCommandProcessor.explicitCallTransfer(bundle.getString("param_string_call_id"));
                    break;
                case "inviteConferenceParticipants":
                    iCommandProcessor.inviteConferenceParticipants(bundle.getString("param_string_call_id"), bundle.getStringArrayList("param_string_array_list_numbers"));
                    break;
                case "blindAssuredEct":
                    iCommandProcessor.blindOrAssuredEct(bundle.getString("param_string_call_id"), bundle.getString("param_string_phone_number"), bundle.getInt("param_int_type"));
                    break;
                case "hangupActiveAndAnswerWaiting":
                    iCommandProcessor.hangupActiveAndAnswerWaiting();
                    break;
                case "hangupAllHold":
                    iCommandProcessor.hangupHold();
                    break;
                case "setSortedIncomingCallList":
                    iCommandProcessor.setSortedIncomingCallList(bundle.getStringArrayList("param_string_array_call_ids"));
                    break;
                case "startVoiceRecording":
                    iCommandProcessor.startVoiceRecording();
                    break;
                case "stopVoiceRecording":
                    iCommandProcessor.stopVoiceRecording();
                    break;
                case "deviceswitch":
                    iCommandProcessor.deviceSwitch(bundle.getString("param_string_call_id"), bundle.getString("param_string_phone_number"), bundle.getString("param_string_device_id"));
                    break;
                case "canceldeviceswitch":
                    iCommandProcessor.cancelDeviceSwitch(bundle.getString("param_string_call_id"));
                    break;
            }
        }
    }
}
