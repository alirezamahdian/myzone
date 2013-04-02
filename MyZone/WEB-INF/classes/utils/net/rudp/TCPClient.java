package utils.net.rudp;
import java.io.*;
import java.net.*;


class TCPClient
{
    public static void main(String argv[]) throws Exception
    {
        String sentence;
        String modifiedSentence;
        BufferedReader inFromUser = new BufferedReader( new InputStreamReader(System.in));
        ReliableSocket clientSocket = new ReliableSocket("localhost", 6789);
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedInputStream inFromServer = new BufferedInputStream(clientSocket.getInputStream());
        sentence = inFromUser.readLine();
        outToServer.writeBytes(sentence + '\n');
        outToServer.flush();
        byte[] buffer = new byte[1024];
        int read = inFromServer.read(buffer, 0, buffer.length);
        byte[] recv = new byte[read];
        System.arraycopy(buffer, 0, recv, 0, read);
        modifiedSentence = new String(recv, "UTF-8"); 
        System.out.println("FROM SERVER: " + modifiedSentence);
        clientSocket.close();
    }
}
