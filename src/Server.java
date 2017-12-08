
import java.net.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.*;

public class Server extends JFrame implements ActionListener{

    // GUI items
    JButton ssButton;
    JLabel machineInfo;
    JLabel portInfo;
    JTextArea history;
    boolean running;

    // Network Items
    boolean serverContinue;
    ServerSocket serverSocket;
    HashMap<String,ClientInfo> connectedClients = new HashMap();

    //Set up GUI and Server
    public Server()
    {
        super( "Echo Server" );

        //get content pane and set its layout
        Container container = getContentPane();
        container.setLayout( new FlowLayout() );

        //create buttons
        running = false;
        ssButton = new JButton( "Start Listening" );
        ssButton.addActionListener( this );
        container.add( ssButton );

        //Set up networking
        String machineAddress = null;
        try
        {
            InetAddress addr = InetAddress.getLocalHost();
            machineAddress = addr.getHostAddress();
        }
        catch (UnknownHostException e)
        {
            machineAddress = "127.0.0.1";
        }
        machineInfo = new JLabel (machineAddress);
        container.add( machineInfo );
        portInfo = new JLabel (" Not Listening ");
        container.add( portInfo );

        history = new JTextArea ( 10, 40 );
        history.setEditable(false);
        container.add( new JScrollPane(history) );

        setSize( 500, 250 );
        setVisible( true );

    }

    public static void main( String args[] )
    {
        Server application = new Server();
        application.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
    }

    //Handle turning on/off server gui functionality
    public void actionPerformed( ActionEvent event )
    {
        if (running == false)
        {
            running=true;
            serverContinue=true;
            new ConnectionThread (this);
        }
        else
        {
            serverContinue = false;
            running = false;
            ssButton.setText ("Start Listening");
            portInfo.setText (" Not Listening ");
        }
    }


} //End Server


//Handles listening and accepting client connections
class ConnectionThread extends Thread
{
    Server gui;

    public ConnectionThread (Server es3)
    {
        gui = es3;
        start();
    }

    public void run()
    {
        try
        {
            gui.serverSocket = new ServerSocket(0);
            gui.portInfo.setText("Listening on Port: " + gui.serverSocket.getLocalPort());
            try {
                //Continously accept connections and spawn thread for each
                while (gui.serverContinue)
                {
                    gui.ssButton.setText("Stop Listening");
                    new CommunicationThread (gui.serverSocket.accept(), gui,gui.connectedClients);
                }
            }
            catch (IOException e)
            {
                System.err.println("Accept failed.");
                System.exit(1);
            }
        }
        catch (IOException e)
        {
            System.err.println("Could not listen on port: 10008.");
            System.exit(1);
        }
        finally
        {
            try {
                gui.serverSocket.close();
            }
            catch (IOException e)
            {
                System.err.println("Could not close port: 10008.");
                System.exit(1);
            }
        }
    }
} //End ConnectionThread


//Handles subsequent requests from a client
//One instance per client will be created
class CommunicationThread extends Thread
{
    //private boolean serverContinue = true;
    private Socket clientSocket;
    private Server gui;
    private HashMap<String,ClientInfo> connectedClients;


    public CommunicationThread (Socket clientSoc, Server ec3, HashMap<String,ClientInfo> cc)
    {
        clientSocket = clientSoc;
        gui = ec3;
        connectedClients = cc;
        start();
    }

    public void run()
    {
        System.out.println ("New Communication Thread Started");

        try {
            //Obtain communication streams
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

            //What is received from client
            Object clientInput;

            //Read in what client has sent
            while((clientInput=in.readObject())!=null){

                //First time connection
                if(clientInput instanceof ClientInfo){
                    //Obtain client's information
                    ClientInfo clientInfo = (ClientInfo)clientInput;
                    gui.history.insert("Recieved ClientInfo from: " + clientInfo.getName() + "\n",0);

                    //Ensure unique name
                    if(connectedClients.get(clientInfo.getName())==null){
                        //Store client info
                        clientInfo.setOutStream(out);
                        connectedClients.put(clientInfo.getName(),clientInfo);

                        //Send out success message to client
                        out.writeObject(new Message("Connected to Server", "Server",clientInfo.getName()));
                        out.reset();

                        //Send new client info to all other connected clients
                        //Send the new client info about all other clients
                        for (String key : connectedClients.keySet()){
                            ClientInfo otherClient = connectedClients.get(key);
                            //Do not send to yourself
                            if((otherClient.getName().compareTo(clientInfo.getName()))!=0){
                                otherClient.getOutStream().writeObject(clientInfo);
                                out.writeObject(otherClient);
                                out.reset();
                            }
                        }
                    }
                    else{
                        //Inform client about not unique name and request again
                        out.writeObject(new Message("Choose Unique Name", "Server",clientInfo.getName()));
                        out.reset();
                    }
                }

                //Client sent a message
                if(clientInput instanceof Message){
                    //Obtain message
                    Message clientMsg = (Message)clientInput;

                    //Client wants to disconnect
                    if((clientMsg.getMessage().compareTo("Disconnecting"))==0){
                        //Obtain client's information
                        ClientInfo closingInfo = connectedClients.get(clientMsg.getFrom());
                        gui.history.insert("Disconnecting: " + clientMsg.getFrom() +"\n",0);

                        //Send client ok signal to disconnect
                        closingInfo.getOutStream().writeObject(new Message("Disconnecting","Server",clientMsg.getFrom()));

                        //Send message to all other connected clients to remove closing client from their connectedList
                        for (String key : connectedClients.keySet()){
                            ClientInfo otherClient = connectedClients.get(key);
                            //Do not send to yourself
                            if((otherClient.getName().compareTo(closingInfo.getName()))!=0){
                                otherClient.getOutStream().writeObject(new Message("Closing Client","Server",closingInfo.getName()));
                            }
                        }

                        //Close client and remove their information
                        closingInfo.getOutStream().close();
                        in.close();
                        clientSocket.close();
                        connectedClients.remove(clientMsg.getFrom());
                        break;
                    }
                    //Client wants to send message to another client
                    else{
                        //Obtain receiving client name
                        ClientInfo receiverInfo = connectedClients.get(clientMsg.getTo());

                        //If receiving client exists, send them the message
                        if(receiverInfo!=null){
                            receiverInfo.getOutStream().writeObject(clientMsg);
                        }
                        else{
                            gui.history.insert("There is no client with name " + clientMsg.getTo()+ "\n",0);
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            System.err.println("Problem with Communication Server");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
} //End ConnectionThread






