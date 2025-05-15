package Protocol;

import java.io.File;
import java.io.Serializable;

public class Client implements Serializable {
    private String name;
    private String ipv4;
    private int networkPort;
    private File[] files;

    // Constructor
    public Client(String name, String ipv4, int networkPort, File[] files) {
        this.name = name;             // assign parameter to instance variable
        this.ipv4 = ipv4;
        this.networkPort = networkPort;
        this.files = files;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getIpv4() {
        return ipv4;
    }

    public int getNetworkPort() {
        return networkPort;
    }

    public File[] getFiles() {
        return files;
    }

    // Setter
    public void setFiles(File[] files) {
        this.files = files;
    }
}
