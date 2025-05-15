package Protocol;

import java.io.File;
import java.io.Serializable;
import java.util.List;

public class CommunicationProtocol implements Serializable {
    // Messages
    private String codeProtocol;
    private String message;
    private String senderName;
    private String targetName;

    // Request/Update resources
    private String fileForRequest;
    private File[] filesForUpdate;
    private int portForSendingFile;

    // Login
    private Client clientForLogin;

    // Active clients
    private List<Client> activeClients;

    // Multicast Group
    private String multicastGroup;
    private int multicastGroupPort;

    // Constructor with protocol code
    public CommunicationProtocol(String codeProtocol) {
        this.codeProtocol = codeProtocol;
    }

    // Empty constructor
    public CommunicationProtocol() {
    }

    // ===== Setters =====
    public void setCodeProtocol(String codeProtocol) {
        this.codeProtocol = codeProtocol;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setClientForLogin(Client clientForLogin) {
        this.clientForLogin = clientForLogin;
    }

    public void setActiveClients(List<Client> activeClients) {
        this.activeClients = activeClients;
    }

    public void setMulticastGroup(String multicastGroup) {
        this.multicastGroup = multicastGroup;
    }

    public void setMulticastGroupPort(int multicastGroupPort) {
        this.multicastGroupPort = multicastGroupPort;
    }

    public void setFileForRequest(String fileForRequest) {
        this.fileForRequest = fileForRequest;
    }

    public void setFilesForUpdate(File[] filesForUpdate) {
        this.filesForUpdate = filesForUpdate;
    }

    public void setPortForSendingFile(int portForSendingFile) {
        this.portForSendingFile = portForSendingFile;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    // ===== Getters =====
    public Client getClientForLogin() {
        return clientForLogin;
    }

    public String getCodeProtocol() {
        return codeProtocol;
    }

    public String getMessage() {
        return message;
    }

    public List<Client> getActiveClients() {
        return activeClients;
    }

    public String getMulticastGroup() {
        return multicastGroup;
    }

    public int getMulticastGroupPort() {
        return multicastGroupPort;
    }

    public String getFileForRequest() {
        return fileForRequest;
    }

    public File[] getFilesForUpdate() {
        return filesForUpdate;
    }

    public int getPortForSendingFile() {
        return portForSendingFile;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getTargetName() {
        return targetName;
    }
}
