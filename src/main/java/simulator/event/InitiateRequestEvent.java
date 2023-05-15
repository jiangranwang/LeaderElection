package simulator.event;
import enums.EventType;
import network.Address;
// import network.message.Message;

public class InitiateRequestEvent extends Event {
    public InitiateRequestEvent(long time, Address id) {
        super(EventType.INITATE_REQ, time, id);
    }
}
