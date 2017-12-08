import javafx.util.Pair;

import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ClientInfo implements Serializable {
    private String name;
    private Pair publicKey;
    private transient ObjectOutputStream outStream; //Maybe this is not needed in the client class so we can make it transient so it is not serialized...

    public ClientInfo(String name, int e, int n){
        this.name = name;
        this.publicKey = new Pair(e,n);
    }

    public String getName() {
        return name;
    }

    public Pair getPublicKey() {
        return publicKey;
    }

    public ObjectOutputStream getOutStream() {
        return outStream;
    }

    public void setOutStream(ObjectOutputStream outStream) {
        this.outStream = outStream;
    }

}
