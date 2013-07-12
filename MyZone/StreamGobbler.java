import java.io.*;
class StreamGobbler extends Thread {
    InputStream is;
    
    // reads everything from is until empty.
    StreamGobbler(InputStream is) {
        this.is = is;
    }
    
    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ( (line = br.readLine()) != null)
                System.out.println(line);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}