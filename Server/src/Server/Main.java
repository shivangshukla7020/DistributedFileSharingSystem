package Server;


import Protocol.Client;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class Main {
    static List<Client> clientesLigados;
    static InetAddress grupoMulticastInet;
    static int grupoMulticastPorto;
    
    public static void main(String[] args){
        inicializarConfig();
        
    }
    
    private static void inicializarConfig(){
        //Initialize variables
        clientesLigados = Collections.synchronizedList(new ArrayList<Client>());
        Scanner scan = new Scanner(System.in);
        
        //Get port
        int port;
        while(true){
            System.out.print("Enter the port:");
            try{
                port = Integer.parseInt(scan.nextLine());
            }
            catch(NumberFormatException e){
                System.out.println("Invalid port, please try again\n");
                continue;
            }
            break;
        }
        
        // Get multicast group
        while(true){
            System.out.print("Enter the multicast group: ");
            String grupoMulticast = scan.nextLine();
            try {
                grupoMulticastInet = InetAddress.getByName(grupoMulticast);
            } catch (UnknownHostException ex) {
                System.out.println("Invalid multicast group, please try again\n");
                continue;
            }
            break;
        }
        
       //Get multicast group port
        while(true){
            System.out.print("Enter the multicast group port: ");
            try{
                grupoMulticastPorto = Integer.parseInt(scan.nextLine());
            }
            catch(NumberFormatException e){
                System.out.println("Invalid port, please try again\n");
                continue;
            }
            break;
        }
        
       //Start server
        try{
            InetAddress inetAddress = InetAddress.getLocalHost();
            ServerSocket serverSocket = new ServerSocket(port);
            
            System.out.println("Server started at\nIPv4 Address : " + inetAddress.getHostAddress() + "\n" + "Porto : " + String.valueOf(port) + "\n");
            
            while(true){
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connection received from client at: " + clientSocket.getInetAddress().getHostAddress());
                
                //Enviar ligação a uma thread aparte
                new Thread(new ClientConnectionHandler(clientSocket, clientesLigados, grupoMulticastInet, grupoMulticastPorto)).start();
                
                /*for(Client client : clientesLigados){
                    System.out.println("------------------------CLIENTE--------------------------------");
                    System.out.println(client.getNome() + "\n");
                    System.out.println(client.getIpv4() + "\n");
                    for(File file : client.getFiles()){
                        System.out.println(file.getName());
                    }
                    System.out.println("------------------------CLIENTE--------------------------------");
                }*/
            }
            
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }
    
    
}
