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
    static List<Client> connectedClients;
    static InetAddress multicastGroupAddress;
    static int multicastGroupPort;
    
    public static void main(String[] args){
        initializeConfig();
    }
    
    private static void initializeConfig(){
        //Initialize variables
        connectedClients = Collections.synchronizedList(new ArrayList<Client>());
        Scanner scan = new Scanner(System.in);
        
        //Get port
        int port;
        while(true){
            System.out.print("Enter the server port:");
            try{
                port = Integer.parseInt(scan.nextLine());
            }
            catch(NumberFormatException e){
                System.out.println("Invalid server port, please try again\n");
                continue;
            }
            break;
        }
        
        // Get multicast group
        while(true){
            System.out.print("Enter the multicast group Inet address: ");
            String grupoMulticast = scan.nextLine();
            try {
                multicastGroupAddress = InetAddress.getByName(grupoMulticast);
            } catch (UnknownHostException ex) {
                System.out.println("Invalid multicast group Inet Address, please try again\n");
                continue;
            }
            break;
        }
        
       //Get multicast group port
        while(true){
            System.out.print("Enter the multicast group port: ");
            try{
                multicastGroupPort = Integer.parseInt(scan.nextLine());
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
                
                //Assign connection to a separate thread
                new Thread(new ClientConnectionHandler(clientSocket, connectedClients, multicastGroupAddress, multicastGroupPort)).start();
                
                /*for(Client client : connectedClients){
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
