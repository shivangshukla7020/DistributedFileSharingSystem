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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientConnectionHandler implements Runnable{
    private Socket clientSocket;
    private List<Client> clientesLigados;
    private InetAddress grupoMulticast;
    private int grupoMulticastPorto;
    private CommunicationProtocol response;
    private ObjectOutputStream objectOutput = null;
    private ObjectInputStream objectInput = null;
    
    public ClientConnectionHandler(Socket aClientSocket, List<Client> aClientesLigados, InetAddress aGrupoMulticast, int aGrupoMulticastPorto){
        clientSocket = aClientSocket;
        clientesLigados = aClientesLigados;
        response = new CommunicationProtocol();
        grupoMulticast = aGrupoMulticast;
        grupoMulticastPorto = aGrupoMulticastPorto;
    }

    @Override
    public void run() {
        try {
            //Criar os streams
            objectOutput = new ObjectOutputStream(clientSocket.getOutputStream());
            objectOutput.flush();
            objectInput = new ObjectInputStream(clientSocket.getInputStream());
            
            //Obter o request do client
            CommunicationProtocol request = (CommunicationProtocol) objectInput.readObject();
            
            //Responder o request
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
                    LocalTime tempoAtual = LocalTime.now();
                    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                    String tempoAtualStr = tempoAtual.format(dateFormatter);
                    
                    //Responder o request
                    response.setCodeProtocol("OK");
                    response.setMessage("Informe recebido");
                    sendResponse();

                    //Informar aos utilizadores da transferencia
                    CommunicationProtocol messageLogin = new CommunicationProtocol("NOTIFICATION");
                    messageLogin.setMessage(tempoAtualStr + "  -  " + "O utilizador " + request.getNameSender() + " transferiu um ficheiro do utilizador " + request.getNameTarget());
                    notificarGrupoMulticast(messageLogin);
                    break;
                    
                default:
                    response.setCodeProtocol("ERROR");
                    response.setMessage("Pedido invalido");
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
        //Obter o cliente
        Client clientForLogin = request.getClientForLogin();
        boolean duplicateFlag = false;
        
        //Verificar se o name é duplicado
        synchronized(clientesLigados){
            for(Client client : clientesLigados){
                if(client.getName().equalsIgnoreCase(clientForLogin.getName())){
                    response.setCodeProtocol("ERROR");
                    response.setMessage("Duplicate name");
                    duplicateFlag = true;
                }
            }
        }
        
        
        //Se for duplicado, enviamos mensagem de erro, caso contrario, registamos ao utilizador
        if(duplicateFlag){
            //Enviamos a resposta de Erro
            sendResponse();
            
            //Fechamos os recursos abertos
            try {
                objectInput.close();
            } catch (IOException ex) {
                Logger.getLogger(ClientConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else{
            //Criamos uma resposta de sucesso e enviamos
            clientesLigados.add(clientForLogin);
            response.setCodeProtocol("OK");
            response.setMessage("Login successful");
            response.setActiveClients(clientesLigados);
            response.setGrupoMulticast(grupoMulticast.getHostAddress());
            response.setGrupoMulticastPorto(grupoMulticastPorto);
            sendResponse();
            
            //Informamos aos utilizadores da alteração de clientes ativos
            CommunicationProtocol messageNotification = new CommunicationProtocol("ACTIVE_CLIENTS_UPDATE");
            messageNotification.setActiveClients(clientesLigados);
            notificarGrupoMulticast(messageNotification);
            
            //Fechamos os recursos abertos
            try {
                objectInput.close();
            } catch (IOException ex) {
                Logger.getLogger(ClientConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            //Notificamos o login a todos os utilizadores da rede
            LocalTime tempoAtual = LocalTime.now();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String tempoAtualStr = tempoAtual.format(dateFormatter);
            
            CommunicationProtocol messageLogin = new CommunicationProtocol("NOTIFICATION");
            messageLogin.setMessage(tempoAtualStr + "  -  " + "O utilizador " + clientForLogin.getName() + " fez login");
            notificarGrupoMulticast(messageLogin);
        } 
    }
    
    private void updateFilesRequest(CommunicationProtocol request){
        //Obter os ficheiros novos 
        File[] filesForUpdate = request.getFilesForUpdate();
        
        //Obter utilizador que fez o request
        synchronized(clientesLigados){
            for(Client client : clientesLigados){
                if(client.getName().equals(request.getNameSender())){
                    client.setFiles(filesForUpdate);
                }
            }
        }
        
        
        //Informar ao cliente que a operação foi bem sucedida
        response.setCodeProtocol("OK");
        response.setMessage("Files updated successfully");
        sendResponse();
        
        //Informar os clientes da alteracao
        CommunicationProtocol message = new CommunicationProtocol("CLIENT_FILES_UPDATED");
        message.setActiveClients(clientesLigados);
        notificarGrupoMulticast(message);
    }
    
    private void logoutRequest(CommunicationProtocol request){
        //Obter o name do quem fez o request
        String nameSender = request.getNameSender();
        
        //Remover o cliente da lista de clientes ligados
        synchronized(clientesLigados){
            Iterator<Client> iterator = clientesLigados.iterator();
            while(iterator.hasNext()){
                Client client = iterator.next();
                if(client.getName().equals(nameSender)){
                    iterator.remove();
                }
            }
        }
        
        
        //Informar ao cliente que a operação foi bem sucedida
        response.setCodeProtocol("OK");
        response.setMessage("Logout realizado com sucesso");
        sendResponse();
        
        //Informar alteracao dos clientes ligados
        CommunicationProtocol message = new CommunicationProtocol("CLIENT_LIST_ALTERED");
        message.setActiveClients(clientesLigados);
        notificarGrupoMulticast(message);
        
        //Notificamos o logout a todos os utilizadores da rede
        LocalTime tempoAtual = LocalTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String tempoAtualStr = tempoAtual.format(dateFormatter);

        CommunicationProtocol messageLogout = new CommunicationProtocol("NOTIFICATION");
        messageLogout.setMessage(tempoAtualStr + "  -  " + "O utilizador " + nameSender + " fez logout");
        notificarGrupoMulticast(messageLogout);
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
    
    private void notificarGrupoMulticast(CommunicationProtocol messageNotification){
        MulticastSocket multicastSocket = null;
        DatagramPacket dp = null;
        try {
            multicastSocket = new MulticastSocket();
        } catch (IOException ex) {
            Logger.getLogger(ClientConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //Serializar mensagem e enviar
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream);
            objectOutputStream.writeObject(messageNotification);
            objectOutputStream.flush();
            
            byte[] data = byteStream.toByteArray();
            dp = new DatagramPacket(data, data.length, grupoMulticast, grupoMulticastPorto);
            
            multicastSocket.send(dp);
            
            //Fechar recursos
            multicastSocket.close();
            objectOutputStream.close();
            
        } catch (IOException ex) {
            Logger.getLogger(ClientConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }
    
}
