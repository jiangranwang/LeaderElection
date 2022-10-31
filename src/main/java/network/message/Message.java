package network.message;

import java.io.Serializable;

import network.Address;
import network.message.payload.MessagePayload;

public class Message implements Cloneable, Serializable {
    private static final long serialVersionUID = 47223974816589531L;
    private final Address src, dst;
    private final int srcSeqNo;
    private final MessagePayload payload;

    public Message(Address src, int srcSeqNo, Address dst, MessagePayload payload) {
        this.src = src;
        this.srcSeqNo = srcSeqNo;
        this.dst = dst;
        this.payload = payload;
    }

    public Message(Message other, Address dst) {
        this.src = other.src;
        this.srcSeqNo = other.srcSeqNo;
        this.dst = dst;
        this.payload = other.payload;
    }

    public Address getSrc() {
        return src;
    }

    public Address getDst() {
        return dst;
    }

    public int getSrcSeqNo() {
        return srcSeqNo;
    }

    public MessagePayload getPayload() {
        return payload;
    }

    public String toString() {
        return "from " + src + " to " + dst + " (srcSeqNo: " + srcSeqNo + ") payload: " + payload;
    }

    @Override
    public Message clone() {
        try {
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            return (Message) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}