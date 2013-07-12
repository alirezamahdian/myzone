import java.io.*;

public class run_win{
    
    public static void main(String[] args){
        try{
            Runtime rt = Runtime.getRuntime();
            Process jetty = rt.exec("java -Djetty.class.path=\".;./lib/ext/bcprov-jdk15on-148.jar;./lib/ext/commons-codec-1.6.jar;./lib/ext/commons-logging-1.1.1.jar;./lib/ext/fluent-hc-4.2.5.jar;./lib/ext/httpclient-4.2.5.jar;./lib/ext/httpclient-cache-4.2.5.jar;./lib/ext/httpcore-4.2.4.jar;./lib/ext/httpmime-4.2.5.jar\" -jar start.jar");
            //output both stdout and stderr data from proc to stdout of this process
            StreamGobbler errorGobbler = new StreamGobbler(jetty.getErrorStream());
            StreamGobbler outputGobbler = new StreamGobbler(jetty.getInputStream());
            errorGobbler.start();
            outputGobbler.start();
            
            Process myzone = rt.exec("java -cp .;bcprov-jdk15on-148.jar;commons-codec-1.6.jar;commons-logging-1.1.1.jar;fluent-hc-4.2.5.jar;httpclient-4.2.5.jar;httpclient-cache-4.2.5.jar;httpcore-4.2.4.jar;httpmime-4.2.5.jar MyZone.MyZoneEngine", null, new File("./MyZone/WEB-INF/classes/"));
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