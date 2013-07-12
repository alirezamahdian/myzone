/*
 =============================================================================
 |Copyright (C) 2012  Alireza Mahdian email:alireza.mahdian@colorado.edu     |
 |This program is free software: you can redistribute and modify 	         |
 |it under the terms of the GNU General Public License as published by       |
 |the Free Software Foundation, either version 3 of the License, or          |
 |(at your option) any later version.                                        |
 |                                                                           |
 |This program is distributed in the hope that it will be useful,            |
 |but WITHOUT ANY WARRANTY; without even the implied warranty of             |
 |MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              |
 |GNU General Public License for more details.                               |
 |                                                                           |
 |You should have received a copy of the GNU General Public License          |
 |along with this program.  If not, see <http://www.gnu.org/licenses/>.      |
 =============================================================================
 */

package MyZone;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Collections;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Text;

import MyZone.elements.*;
import MyZone.profile;

/*
 this file implements the feed class. feed class creates a news feed based on
 the recent activities of friends. the main function createFeed builds a feed
 based on the time interval specified by since and to variables passed to it.
 the feeds that are included after the createFeed call are the ones that have 
 a posted timestamp that is bigger than and smaller than since and to respectively.
 */

public class feed {
    
    private final static boolean DEBUG = false;
    Document dom;
    private String prefix;
    private List<user> users = new ArrayList();
    private Settings settings;
    private String feedName;
    public List<timelineEntry> stream;
    
    public feed(String prefix, String feedName, List<user> users){
        this.prefix = prefix;
        this.feedName = feedName;
        stream = new ArrayList();
        settings = new Settings(prefix);
        if (users != null){
            this.users.clear();
            for (int i = 0; i < users.size(); i++){
                user u = new user(users.get(i));
                this.users.add(u);
            }
        }
    }
    
    public String getName(){
        return feedName;
    }
    
    public void createFeed(long since, long to){
        stream.clear();
        settings.refresh(settings.BASIC_INFO);
        for (int i = 0; i <  users.size(); i++){
            user u = new user(users.get(i));
            List<timelineEntry> temp = new ArrayList();
            profile usr = new profile(u.getUsername(), settings.username, prefix);
            temp = usr.createTimeLine(since, to);
            addToStream(temp);
        }
        Collections.sort(stream);
    }
    
    public void addToStream(List<timelineEntry> toBeAdded){
        if (toBeAdded != null){
            stream.addAll(toBeAdded);
        }
    }
    
    public String displayFeed(){
        String result = "";
        try{
            for (int i = 0; i < stream.size(); i++){
                timelineEntry tle = new timelineEntry(stream.get(i));
                if(tle.type.equals("link")){
                    link l = tle.l;
                    result += l.generateHTML("feed");
                }
                if(tle.type.equals("wallPost")){
                    wallPost s = tle.s;
                    result += s.generateHTML("feed");
                }
                if(tle.type.equals("audio")){
                    audio a = tle.a;
                    result += a.generateHTML("feed", null);
                }
                if(tle.type.equals("video")){
                    video v = tle.v;
                    result += v.generateHTML("feed", null);
                }
                if(tle.type.equals("photo")){
                    photo p = tle.p;
                    result += p.generateHTML("feed", null);
                }
            }
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return result;
    }
    
}
