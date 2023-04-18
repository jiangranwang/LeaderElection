/*
Wrapper class for all message types
 */

package network.message;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import network.Address;
import network.message.payload.MessagePayload;

public class Message implements Cloneable, Serializable {
    private static final long serialVersionUID = 47223974816589531L;
    private final Address src, dst; // source and destination of the message
    private Address curr; // message is currently at this node (in the middle of routing)
    private final int srcSeqNo, messageNo; // numbers that uniquely identifies this message
    private final MessagePayload payload; // actual message payload

    public Message(Address src, int srcSeqNo, Address dst, MessagePayload payload) {
        this.src = src;
        this.curr = src;
        this.srcSeqNo = srcSeqNo;
        this.dst = dst;
        this.payload = payload;
        this.messageNo = -1;
    }

    public Message(Address src, int srcSeqNo, Address dst, MessagePayload payload, int messageNo) {
        this.src = src;
        this.curr = src;
        this.srcSeqNo = srcSeqNo;
        this.dst = dst;
        this.payload = payload;
        this.messageNo = messageNo;
    }

    public Message(Message other, Address dst) {
        this.src = other.src;
        this.curr = other.curr;
        this.srcSeqNo = other.srcSeqNo;
        this.dst = dst;
        this.payload = other.payload;
        this.messageNo = other.messageNo;
    }

    public Address getSrc() {
        return src;
    }

    public Address getDst() {
        return dst;
    }

    public Address getCurr() {
        return curr;
    }

    public void setCurr(Address curr) {
        this.curr = curr;
    }

    public int getSrcSeqNo() {
        return srcSeqNo;
    }

    public int getMessageNo() {
        return messageNo;
    }

    public int getByteSize() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this.clone());
            oos.flush();
            return bos.toByteArray().length;
        }
        catch (Exception ex) {
            System.out.println("Exception during msg byte size calculation with " + payload);
            ex.printStackTrace(System.out);
            return -1;
        }
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
            return (Message) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}