import java.io.*;

public class run_unix{
    
    public static void main(String[] args){
        try{
            Runtime rt = Runtime.getRuntime();
            Process jetty = rt.exec("java -Djetty.class.path=\".:./lib/ext/bcprov-jdk15on-148.jar\" -jar start.jar");
            //output both stdout and stderr data from proc to stdout of this process
            StreamGobbler errorGobbler = new StreamGobbler(jetty.getErrorStream());
            StreamGobbler outputGobbler = new StreamGobbler(jetty.getInputStream());
            errorGobbler.start();
            outputGobbler.start();
            
            Process myzone = rt.exec("java -cp .:bcprov-jdk15on-148.jar MyZone.MyZoneEngine", null, new File("./MyZone/WEB-INF/classes/"));
            //output both stdout and stderr data from proc to stdout of this process
            StreamGobbler errorGobblerMyZone = new StreamGobbler(myzone.getErrorStream());
            StreamGobbler outputGobblerMyZone = new StreamGobbler(myzone.getInputStream());
            errorGobblerMyZone.start();
            outputGobblerMyZone.start();
            jetty.waitFor();
            myzone.waitFor();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}