package network.message.payload.election;

import enums.MessageType;
import enums.AlgorithmPath;
import network.Address;
import network.message.payload.MessagePayload;
import java.util.Set;

public class OkPayload extends MessagePayload {
    private static final long serialVersionUID = 2874891435243124L;
    private final Set<Address> recentlyOKed;
    private final AlgorithmPath path;

    public OkPayload() {
        super(MessageType.OK);
        this.path = AlgorithmPath.FAST;
        this.recentlyOKed = null;
    }

    public OkPayload(AlgorithmPath path, Set<Address> recentlyOKed) {
        super(MessageType.OK);
        this.path = path;
        this.recentlyOKed = recentlyOKed;
    }

    public OkPayload(Set<Address> recentlyOKed) {
        super(MessageType.OK);
        this.path = AlgorithmPath.FAST;
        this.recentlyOKed = recentlyOKed;
    }

    public Set<Address> getRecentlyOKed() {
        return recentlyOKed;
    }
    
    public AlgorithmPath getPath() {
        return path;
    }
}
