package android.os;

public class Broadcaster {
    private Registration mReg;

    public void request(int i, Handler handler, int i2) {
        int length;
        Registration registration;
        synchronized (this) {
            if (this.mReg == null) {
                Registration registration2 = new Registration();
                registration2.senderWhat = i;
                registration2.targets = new Handler[1];
                registration2.targetWhats = new int[1];
                registration2.targets[0] = handler;
                registration2.targetWhats[0] = i2;
                this.mReg = registration2;
                registration2.next = registration2;
                registration2.prev = registration2;
            } else {
                Registration registration3 = this.mReg;
                Registration registration4 = registration3;
                while (registration4.senderWhat < i && (registration4 = registration4.next) != registration3) {
                }
                if (registration4.senderWhat != i) {
                    registration = new Registration();
                    registration.senderWhat = i;
                    registration.targets = new Handler[1];
                    registration.targetWhats = new int[1];
                    registration.next = registration4;
                    registration.prev = registration4.prev;
                    registration4.prev.next = registration;
                    registration4.prev = registration;
                    if (registration4 == this.mReg && registration4.senderWhat > registration.senderWhat) {
                        this.mReg = registration;
                    }
                    length = 0;
                } else {
                    length = registration4.targets.length;
                    Handler[] handlerArr = registration4.targets;
                    int[] iArr = registration4.targetWhats;
                    for (int i3 = 0; i3 < length; i3++) {
                        if (handlerArr[i3] == handler && iArr[i3] == i2) {
                            return;
                        }
                    }
                    int i4 = length + 1;
                    registration4.targets = new Handler[i4];
                    System.arraycopy(handlerArr, 0, registration4.targets, 0, length);
                    registration4.targetWhats = new int[i4];
                    System.arraycopy(iArr, 0, registration4.targetWhats, 0, length);
                    registration = registration4;
                }
                registration.targets[length] = handler;
                registration.targetWhats[length] = i2;
            }
        }
    }

    public void cancelRequest(int i, Handler handler, int i2) {
        synchronized (this) {
            Registration registration = this.mReg;
            if (registration == null) {
                return;
            }
            Registration registration2 = registration;
            while (registration2.senderWhat < i && (registration2 = registration2.next) != registration) {
            }
            if (registration2.senderWhat == i) {
                Handler[] handlerArr = registration2.targets;
                int[] iArr = registration2.targetWhats;
                int length = handlerArr.length;
                int i3 = 0;
                while (true) {
                    if (i3 >= length) {
                        break;
                    }
                    if (handlerArr[i3] != handler || iArr[i3] != i2) {
                        i3++;
                    } else {
                        int i4 = length - 1;
                        registration2.targets = new Handler[i4];
                        registration2.targetWhats = new int[i4];
                        if (i3 > 0) {
                            System.arraycopy(handlerArr, 0, registration2.targets, 0, i3);
                            System.arraycopy(iArr, 0, registration2.targetWhats, 0, i3);
                        }
                        int i5 = (length - i3) - 1;
                        if (i5 != 0) {
                            int i6 = i3 + 1;
                            System.arraycopy(handlerArr, i6, registration2.targets, i3, i5);
                            System.arraycopy(iArr, i6, registration2.targetWhats, i3, i5);
                        }
                    }
                }
            }
        }
    }

    public void dumpRegistrations() {
        synchronized (this) {
            Registration registration = this.mReg;
            System.out.println("Broadcaster " + this + " {");
            if (registration != null) {
                Registration registration2 = registration;
                do {
                    System.out.println("    senderWhat=" + registration2.senderWhat);
                    int length = registration2.targets.length;
                    for (int i = 0; i < length; i++) {
                        System.out.println("        [" + registration2.targetWhats[i] + "] " + registration2.targets[i]);
                    }
                    registration2 = registration2.next;
                } while (registration2 != registration);
            }
            System.out.println("}");
        }
    }

    public void broadcast(Message message) {
        synchronized (this) {
            if (this.mReg == null) {
                return;
            }
            int i = message.what;
            Registration registration = this.mReg;
            Registration registration2 = registration;
            while (registration2.senderWhat < i && (registration2 = registration2.next) != registration) {
            }
            if (registration2.senderWhat == i) {
                Handler[] handlerArr = registration2.targets;
                int[] iArr = registration2.targetWhats;
                int length = handlerArr.length;
                for (int i2 = 0; i2 < length; i2++) {
                    Handler handler = handlerArr[i2];
                    Message messageObtain = Message.obtain();
                    messageObtain.copyFrom(message);
                    messageObtain.what = iArr[i2];
                    handler.sendMessage(messageObtain);
                }
            }
        }
    }

    private class Registration {
        Registration next;
        Registration prev;
        int senderWhat;
        int[] targetWhats;
        Handler[] targets;

        private Registration() {
        }
    }
}
