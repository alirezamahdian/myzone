package utils.net.rudp;

import java.io.*;
import java.net.*;

class TCPServer
{
    public static void main(String argv[]) throws Exception
    {
        String clientSentence;
        String capitalizedSentence;
        ReliableServerSocket welcomeSocket = new ReliableServerSocket(6789);
        
        while(true)
        {
            Socket connectionSocket = welcomeSocket.accept();
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
            BufferedInputStream inFromClient = new BufferedInputStream(connectionSocket.getInputStream());
            byte[] buffer = new byte[1024];
            long lastReception = System.currentTimeMillis();
            long idleTime = 0;
            inFromClient.read(buffer, 0, 4);
            byte[] recv = new byte[4];
            System.arraycopy(buffer, 0, recv, 0, 4);
            
            clientSentence = new String(recv, "UTF-8");
            System.out.println("Received: " + clientSentence);
            capitalizedSentence = clientSentence.toUpperCase() + '\n';
            outToClient.writeBytes(capitalizedSentence);
            outToClient.flush();
        }
    }
}
