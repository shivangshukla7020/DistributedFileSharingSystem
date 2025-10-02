package Server;


import Protocol.CommunicationProtocol;
import Protocol.Client;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientConnectionHandler implements Runnable{
    private Socket clientSocket;
    private List<Client> connectedClients;
    private InetAddress multicastGroup;
    private int multicastGroupPort;
    private CommunicationProtocol response;
    private ObjectOutputStream objectOutput = null;
    private ObjectInputStream objectInput = null;
    
    public ClientConnectionHandler(Socket clientSocket, List<Client> connectedClients, InetAddress multicastGroup, int multicastGroupPort){
        this.clientSocket = clientSocket;
        this.connectedClients = connectedClients;
        this.response = new CommunicationProtocol();
        this.multicastGroup = multicastGroup;
        this.multicastGroupPort = multicastGroupPort;
    }

    @Override
    public void run() {
        try {
            //Create the streams
            objectOutput = new ObjectOutputStream(clientSocket.getOutputStream());
            objectOutput.flush();
            objectInput = new ObjectInputStream(clientSocket.getInputStream());
            
            // Get the client request
            CommunicationProtocol request = (CommunicationProtocol) objectInput.readObject();
            
            // Respond to the request
            switch(request.getCodeProtocol()){
                case "LOGIN":
                    loginRequest(request);
                    break;
                case "UPDATE_FILES":
                    updateFilesRequest(request);
                    break;
                case "LOGOUT":
                    logoutRequest(request);
                    break;
                case "TRANSFER_REPORT":
                    LocalTime currentTime = LocalTime.now();
                    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                    String currentTimeStr = currentTime.format(dateTimeFormatter);
                    
                    // Respond to request
                    response.setCodeProtocol("OK");
                    response.setMessage("Report received");
                    sendResponse();

                    //Inform the users of the transfer
                    CommunicationProtocol loginMessage = new CommunicationProtocol("NOTIFICATION");
                    loginMessage.setMessage(currentTimeStr + "  -  " + "User " + request.getSenderName() + " transferred a file from user " + request.getTargetName());
                    notifyMulticastGroup(loginMessage);
                    break;
                    
                default:
                    response.setCodeProtocol("ERROR");
                    response.setMessage("Invalid request");
                    sendResponse();
                    break;
            }
            
        } 
        catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(ClientConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
        } 
        finally {
            try {
                objectInput.close();
            } catch (IOException ex) {
                Logger.getLogger(ClientConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void loginRequest(CommunicationProtocol request){
        Client clientForLogin = request.getClientForLogin();
        boolean duplicateFlag = false;
        
        //Check if the username is already taken
        synchronized(connectedClients){
            for(Client client : connectedClients){
                if(client.getName().equalsIgnoreCase(clientForLogin.getName())){
                    response.setCodeProtocol("ERROR");
                    response.setMessage("Duplicate name");
                    duplicateFlag = true;
                }
            }
        }
        
        
        //If it is a duplicate, we send an error message, otherwise, we register the user
        if(duplicateFlag){
            //Send error response
            sendResponse();
            
            //Close the open resources
            try {
                objectInput.close();
            } catch (IOException ex) {
                Logger.getLogger(ClientConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else{
            //Add the new client to the list
            connectedClients.add(clientForLogin);

            //Send login success response
            response.setCodeProtocol("OK");
            response.setMessage("Login successful");
            response.setActiveClients(connectedClients);
            response.setMulticastGroup(multicastGroup.getHostAddress());
            response.setMulticastGroupPort(multicastGroupPort);
            sendResponse();
            
            //Notify all clients about the updated active client list
            CommunicationProtocol messageNotification = new CommunicationProtocol("ACTIVE_CLIENTS_UPDATE");
            messageNotification.setActiveClients(connectedClients);
            notifyMulticastGroup(messageNotification);
            
            try {
                objectInput.close();
            } catch (IOException ex) {
                Logger.getLogger(ClientConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            //Notify all the users about the new login
            LocalTime currentTime = LocalTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String currentTimeStr = currentTime.format(dateTimeFormatter);
            
            CommunicationProtocol loginMessage = new CommunicationProtocol("NOTIFICATION");
            loginMessage.setMessage(currentTimeStr + "  -  " + "User " + clientForLogin.getName() + " logged in");
            notifyMulticastGroup(loginMessage);
        } 
    }
    
    private void updateFilesRequest(CommunicationProtocol request){
        //Get the new files 
        File[] filesForUpdate = request.getFilesForUpdate();
        
        //Get the client who made the request
        synchronized(connectedClients){
            for(Client client : connectedClients){
                if(client.getName().equals(request.getSenderName())){
                    client.setFiles(filesForUpdate);
                }
            }
        }
        
        
        //Inform the client that the operation was successful
        response.setCodeProtocol("OK");
        response.setMessage("Files updated successfully");
        sendResponse();
        
        //Inform all clients about the update
        CommunicationProtocol message = new CommunicationProtocol("CLIENT_FILES_UPDATED");
        message.setActiveClients(connectedClients);
        notifyMulticastGroup(message);
    }
    
    private void logoutRequest(CommunicationProtocol request){
        //Get the name of the client who made the request
        String senderName = request.getSenderName();
        
        //Remove the client from the list of connected clients
        synchronized(connectedClients){
            Iterator<Client> iterator = connectedClients.iterator();
            while(iterator.hasNext()){
                Client client = iterator.next();
                if(client.getName().equals(senderName)){
                    iterator.remove();
                }
            }
        }
        
        
        //Inform the client that the operation was successful
        response.setCodeProtocol("OK");
        response.setMessage("Logout realizado com sucesso");
        sendResponse();
        
        //Inform all clients about the updated client list
        CommunicationProtocol message = new CommunicationProtocol("CLIENT_LIST_ALTERED");
        message.setActiveClients(connectedClients);
        notifyMulticastGroup(message);
        
        //Notify all clients in the network about the logout
        LocalTime currentTime = LocalTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String currentTimeStr = currentTime.format(dateTimeFormatter);

        CommunicationProtocol logoutMessage = new CommunicationProtocol("NOTIFICATION");
        logoutMessage.setMessage(currentTimeStr + "  -  " + "User " + senderName + " logged out");
        notifyMulticastGroup(logoutMessage);
    }
    
    private void sendResponse(){
        try {
            objectOutput.writeObject(response);
            objectOutput.flush();
            objectOutput.close();
        } catch (IOException ex) {
            Logger.getLogger(ClientConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void notifyMulticastGroup(CommunicationProtocol messageNotification){
        MulticastSocket multicastSocket = null;
        DatagramPacket dp = null;
        try {
            multicastSocket = new MulticastSocket();
        } catch (IOException ex) {
            Logger.getLogger(ClientConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //Serialize message and send
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream);
            objectOutputStream.writeObject(messageNotification);
            objectOutputStream.flush();
            
            byte[] data = byteStream.toByteArray();
            dp = new DatagramPacket(data, data.length, multicastGroup, multicastGroupPort);
            
            multicastSocket.send(dp);
            
            //Close resources
            multicastSocket.close();
            objectOutputStream.close();
            
        } catch (IOException ex) {
            Logger.getLogger(ClientConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }
    
}
