import javafx.util.Pair;

import java.net.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.*;

public class Client extends JFrame implements ActionListener
{
    // GUI items
    JButton sendButton;
    JButton connectButton;
    JTextField machineInfo;
    JTextField portInfo;
    JTextField message;
    JTextField p_value;
    JTextField q_value;
    JTextArea history;
    JTextField user_name;
    JTextField receiver_name;
    JComboBox recievers_list = new JComboBox();

    // Network Items
    boolean connected;
    Socket serverSocket;
    ObjectOutputStream out;
    ObjectInputStream in;
    ClientInfo myInfo;
    HashMap<String, ClientInfo> connectedClients = new HashMap();
    Pair publicKey;
    Pair privateKey;
    RSA rsa;


    //Set up GUI and Client
    public Client()
    {
        super( "Echo Client" );

        //get content pane and set its layout
        Container container = getContentPane();
        container.setLayout (new BorderLayout ());

        //set up the North panel
        JPanel upperPanel = new JPanel ();
        upperPanel.setLayout (new GridLayout (8,3));
        container.add (upperPanel, BorderLayout.NORTH);

        //create buttons
        connected = false;

        //Add fields to panel
        upperPanel.add ( new JLabel ("Message: ", JLabel.RIGHT) );
        message = new JTextField ("");
        message.addActionListener( this );
        upperPanel.add( message );

        sendButton = new JButton( "Send Message" );
        sendButton.addActionListener( this );
        sendButton.setEnabled (false);
        upperPanel.add( sendButton );

        connectButton = new JButton( "Connect to Server" );
        connectButton.addActionListener( this );
        upperPanel.add( connectButton );

        upperPanel.add ( new JLabel ("Server Address: ", JLabel.RIGHT) );
        machineInfo = new JTextField ("127.0.0.1");
        upperPanel.add( machineInfo );

        upperPanel.add ( new JLabel ("Server Port: ", JLabel.RIGHT) );
        portInfo = new JTextField ("");
        upperPanel.add( portInfo );

        upperPanel.add ( new JLabel ("p: ", JLabel.RIGHT) );
        p_value = new JTextField ("");
        p_value.addActionListener( this );
        upperPanel.add( p_value );

        upperPanel.add ( new JLabel ("q: ", JLabel.RIGHT) );
        q_value = new JTextField ("");
        q_value.addActionListener( this );
        upperPanel.add( q_value );

        upperPanel.add ( new JLabel ("user_name: ", JLabel.RIGHT) );
        user_name = new JTextField ("");
        user_name.addActionListener( this );
        upperPanel.add( user_name );

        upperPanel.add ( new JLabel ("Choose sender: ", JLabel.RIGHT) );
        upperPanel.add( recievers_list );



        history = new JTextArea ( 10, 40 );
        history.setEditable(false);
        container.add( new JScrollPane(history) ,  BorderLayout.CENTER);

        setSize( 500, 700 );
        setVisible( true );

    }

    public static void main( String args[] )
    {
        Client application = new Client();
        application.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
    }

    //Handle client connection and disconnection gui
    public void actionPerformed( ActionEvent event )
    {
        //If connected, send message
        if ( connected &&
                (event.getSource() == sendButton ||
                        event.getSource() == message ) )
        {
            encryptMessage(message.getText(),(String)recievers_list.getSelectedItem());
        }

        //If not connected, connect and send information about yourself
        else if (event.getSource() == connectButton)
        {
            if(doCheckPQ()){
                doManageConnection();
                doSendClientInfo();
            }

        }
    }

    //Ensure that provided p & q values are valid
    public boolean doCheckPQ(){
        //Send public key and name
        int p = Integer.parseInt(p_value.getText());
        int q = Integer.parseInt(q_value.getText());

        //Verify p & q
        rsa = new RSA(p,q);
        Pair tempPublic = rsa.getPublicKey();
        Pair tempPrivate = rsa.getPrivateKey();

        //If not valid disconnect client
        if(tempPublic==null){
            JOptionPane.showMessageDialog(null,"Invalid p & q values. Pick new ones and reconnect");
                return false;
        }
        //Return true
        else{
            publicKey = new Pair(tempPublic.getKey(),tempPublic.getValue());
            privateKey = new Pair(tempPrivate.getKey(),tempPrivate.getValue());
            return true;
        }

    }

    //Handles message encryption, creation, and sending to server
    public void encryptMessage(String text, String receiver)
    {
        try
        {
            //Encrypt message
            ArrayList<Long> encrypedMessage = rsa.encrypt(text);

            //Send encrypted message
            for(int i=0;i<encrypedMessage.size();i++){
                String temp = encrypedMessage.get(i).toString();
                Message msg = new Message(temp,myInfo.getName(),receiver);
                //out.writeObject(msg);
                //out.reset();
            }
            out.writeObject(new Message(text,myInfo.getName(),receiver));
            out.reset();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void decryptMessage(Message msg){
        ArrayList<Long> blocks = new ArrayList<>();
        msg = rsa.decrypt(msg);
        history.insert(msg.getFrom() + ": " + msg.getMessage() + "\n",0);
    }

    //Handles sending client information to the server
    public void doSendClientInfo(){
        try
        {
            if(connected){
                myInfo = new ClientInfo(user_name.getText(),(int)publicKey.getKey(),(int)publicKey.getValue());
                out.writeObject(myInfo);
                out.reset();
           }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    //Handles connecting to client to server
    public void doManageConnection()
    {
        if (!connected)
        {
            String machineName = null;
            int portNum = -1;
            try {
                machineName = machineInfo.getText();
                portNum = Integer.parseInt(portInfo.getText());

                //Connect to server with IP & Port #
                serverSocket = new Socket(machineName, portNum );

                //Create communication streams
                out = new ObjectOutputStream(serverSocket.getOutputStream());
                in = new ObjectInputStream(serverSocket.getInputStream());

                //Start a new thread to read from the socket
                new CommunicationReadThread (in, this, connectedClients);

                //Update and enable gui
                sendButton.setEnabled(true);
                connected = true;
                connectButton.setText("Disconnect from Server");
            }
            catch (NumberFormatException e) {
                history.insert ( "Server Port must be an integer\n", 0);
            }
            catch (UnknownHostException e) {
                history.insert("Don't know about host: " + machineName , 0);
            }
            catch (IOException e) {
                history.insert ("Couldn't get I/O for "
                        + "the connection to: " + machineName , 0);
            }
        }
        //If connected
        else
        {
            //Send disconnect request to server
            encryptMessage("Disconnecting","Server");
        }
    }


    //Thread handles socket reads from server
    class CommunicationReadThread extends Thread
    {
        private Client gui;
        private ObjectInputStream in;
        private HashMap<String,ClientInfo> connectedClients;

        public CommunicationReadThread (ObjectInputStream inparam, Client ec3, HashMap<String,ClientInfo> cc)
        {
            in = inparam;
            gui = ec3;
            connectedClients = cc;
            start();
            gui.history.insert ("Communicating with Port\n", 0);
        }

        public void run()
        {
            try {
                Object serverInput;

                //Read in what server has sent
                while(((serverInput=in.readObject()) != null)){

                    //New connected client information
                    if(serverInput instanceof ClientInfo){
                        //Add to connected list
                        ClientInfo clientInfo = (ClientInfo)serverInput;
                        connectedClients.put(clientInfo.getName(),clientInfo);

                        //Update Connected Clients List
                        recievers_list.addItem(clientInfo.getName());
                        recievers_list.setVisible(true);
                        recievers_list.revalidate();
                        recievers_list.repaint();
                    }

                    //Server sent a message
                    if(serverInput instanceof Message){
                        //Obtain message
                        Message msg = (Message)serverInput;

                        //Client is now connected to server
                        if((msg.getMessage().compareTo("Connected to Server"))==0){
                            //Enable message sending
                            gui.history.insert("Server verfied client info...ready to chat\n",0);
                        }
                        //Server requesting another name to be chosen
                        else if(msg.getMessage().compareTo("Choose Unique Name")==0){
                            gui.history.insert("Name not unique, choose another name\n",0);
                            //Disconnect client from server
                            //Close out all streams and remove client info/connected clients
                            myInfo = null;
                            out.close();
                            in.close();
                            sendButton.setEnabled(false);
                            connected = false;
                            connectedClients.clear();
                            myInfo = null;
                            connectButton.setText("Connect to Server");
                            gui.history.insert("Disconnecting From Server - Reconnect when you have a unique name\n",0);
                            break;
                        }
                        //Server gives okay to disconnect
                        else if(msg.getMessage().compareTo("Disconnecting")==0){
                            //Close out all streams and remove client info/connected clients
                            myInfo = null;
                            out.close();
                            in.close();
                            sendButton.setEnabled(false);
                            connected = false;
                            connectedClients.clear();
                            myInfo = null;
                            connectButton.setText("Connect to Server");
                            gui.history.insert("Disconnecting From Server\n",0);
                            break;
                        }
                        //Sever tells client to remove a client from the connected list
                        else if((msg.getMessage().compareTo("Closing Client"))==0){
                            connectedClients.remove(msg.getTo());
                            //Update Connected Clients List
                            recievers_list.removeItem(msg.getTo());
                        }
                        else{
                            decryptMessage(msg);
                        }
                    }
                }
                in.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                //System.exit(1);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

} // end class EchoServer3







