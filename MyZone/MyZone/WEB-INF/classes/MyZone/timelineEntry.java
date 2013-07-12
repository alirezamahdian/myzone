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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Text;

import MyZone.elements.*;

/*
 This file contains the implementation of the timelineEntry element.
 for every audio, audioAlbum, video, videoAlbum, photo, photoAlbum, link,
 wallPost, deletedEntry in the profile there will be a timelineEntry in the
 corresponding wall xml file. 
 */

public class timelineEntry implements Comparable<timelineEntry>{
    
    public String type;
    public long id;
    public long lastUpdateTime;
    public link l = new link();
    public wallPost s = new wallPost();
    public audioAlbum aa = new audioAlbum();
    public audio a = new audio();
    public videoAlbum va = new videoAlbum();
    public video v = new video();
    public photoAlbum pa = new photoAlbum();
    public photo p = new photo();
    public event e = new event();
    public message m = new message();
    public deletedEntry de = new deletedEntry();
    public mirror mi = new mirror();
	
    public timelineEntry(){}
    
    public timelineEntry(long id, String type, link l){
        this.type = type;
        this.id = id;
        lastUpdateTime = l.getLastUpdateTime();
        this.l = new link(l);
    }
    
    public timelineEntry(long id, String type, wallPost s){
        this.type = type;
        this.id = id;
        lastUpdateTime = s.getLastUpdateTime();
        this.s = new wallPost(s);
    }
    
    public timelineEntry(long id, String type, audioAlbum aa){
        this.type = type;
        this.id = id;
        lastUpdateTime = aa.getLastUpdateTime();
        this.aa = new audioAlbum(aa);
    }
    
    public timelineEntry(long id, String type, audio a){
        this.type = type;
        this.id = id;
        lastUpdateTime = a.getLastUpdateTime();
        this.a = new audio(a);
    }
    
    public timelineEntry(long id, String type, photoAlbum pa){
        this.type = type;
        this.id = id;
        lastUpdateTime = pa.getLastUpdateTime();
        this.pa = new photoAlbum(pa);
    }
    
    public timelineEntry(long id, String type, photo p){
        this.type = type;
        this.id = id;
        lastUpdateTime = p.getLastUpdateTime();
        this.p = new photo(p);
    }
    
    public timelineEntry(long id, String type, videoAlbum va){
        this.type = type;
        this.id = id;
        lastUpdateTime = va.getLastUpdateTime();
        this.va = new videoAlbum(va);
    }
    
    public timelineEntry(long id, String type, video v){
        this.type = type;
        this.id = id;
        lastUpdateTime = v.getLastUpdateTime();
        this.v = new video(v);
    }
    
    public timelineEntry(long id, String type, event e){
        this.type = type;
        this.id = id;
        lastUpdateTime = e.getLastUpdateTime();
        this.e = new event(e);
    }
    
    public timelineEntry(long id, String type, message m){
        this.type = type;
        this.id = id;
        lastUpdateTime = m.getId();
        this.m = new message(m);
    }
    
    public timelineEntry(long id, String type, deletedEntry de){
        this.type = type;
        this.id = id;
        lastUpdateTime = de.getLastUpdateTime();
        this.de = new deletedEntry(de);
    }
    
    public timelineEntry(long id, String type, mirror mi){
        this.type = type;
        this.id = id;
        lastUpdateTime = mi.getLastUpdateTime();
        this.mi = new mirror(mi);
    }
    
    public timelineEntry(timelineEntry orig){
        type = new String(orig.type);
        id = orig.id;
        lastUpdateTime = orig.lastUpdateTime;
        l = new link(orig.l);
        s = new wallPost(orig.s);
        aa = new audioAlbum(orig.aa);
        a = new audio(orig.a);
        va = new videoAlbum(orig.va);
        v = new video(orig.v);
        pa = new photoAlbum(orig.pa);
        p = new photo(orig.p);
        e = new event(orig.e);
        m = new message(orig.m);
        de = new deletedEntry(orig.de);
        mi = new mirror(orig.mi);
    }
    
    public Element createDOMElement(Document dom){
        Element el = dom.createElement("timeLineEntry");
        
        el.setAttribute("id", String.valueOf(id));
        el.setAttribute("type", type);
        el.setAttribute("lastUpdateTime", String.valueOf(lastUpdateTime));
        
        if (type.equals("link")){
            el.appendChild(l.createDOMElement(dom));
            return el;
        }
        if (type.equals("wallPost")){
            el.appendChild(s.createDOMElement(dom));
            return el;
        }
        if (type.equals("audio")){
            el.appendChild(a.createDOMElement(dom));
            return el;
        }
        if (type.equals("audioAlbum")){
            el.appendChild(aa.createDOMElement(dom));
            return el;
        }
        if (type.equals("video")){
            el.appendChild(v.createDOMElement(dom));
            return el;
        }
        if (type.equals("videoAlbum")){
            el.appendChild(va.createDOMElement(dom));
            return el;
        }
        if (type.equals("photo")){
            el.appendChild(p.createDOMElement(dom));
            return el;
        }
        if (type.equals("photoAlbum")){
            el.appendChild(pa.createDOMElement(dom));
            return el;
        }
        if (type.equals("deletedEntry")){
            el.appendChild(de.createDOMElement(dom));
            return el;
        }
        if (type.equals("event")){
            el.appendChild(e.createDOMElement(dom));
            return el;
        }
        if (type.equals("mirror")){
            el.appendChild(mi.createDOMElement(dom));
            return el;
        }
        if (type.equals("message")){
            el.appendChild(m.createDOMElement(dom));
            return el;
        }
        return el;
    }
    
    public void create(Element el) {
        id = Long.parseLong(el.getAttribute("id"));
        lastUpdateTime = Long.parseLong(el.getAttribute("lastUpdateTime"));
        type = el.getAttribute("type");
        if (type.equals("link")){
            NodeList nl = el.getElementsByTagName("link");
            if(nl != null && nl.getLength() > 0) {
                Element El = (Element)nl.item(0);
                l.create(El);
            }
            return;
        }
        if (type.equals("wallPost")){
            NodeList nl = el.getElementsByTagName("wallPost");
            if(nl != null && nl.getLength() > 0) {
                Element El = (Element)nl.item(0);
                s.create(El);
            }
            return;
        }
        if (type.equals("audio")){
            NodeList nl = el.getElementsByTagName("audio");
            if(nl != null && nl.getLength() > 0) {
                Element El = (Element)nl.item(0);
                a.create(El);
            }
            return;
        }
        if (type.equals("audioAlbum")){
            NodeList nl = el.getElementsByTagName("audioAlbum");
            if(nl != null && nl.getLength() > 0) {
                Element El = (Element)nl.item(0);
                aa.create(El);
            }
            return;
        }
        if (type.equals("video")){
            NodeList nl = el.getElementsByTagName("video");
            if(nl != null && nl.getLength() > 0) {
                Element El = (Element)nl.item(0);
                v.create(El);
            }
            return;
        }
        if (type.equals("videoAlbum")){
            NodeList nl = el.getElementsByTagName("videoAlbum");
            if(nl != null && nl.getLength() > 0) {
                Element El = (Element)nl.item(0);
                va.create(El);
            }
            return;
        }
        if (type.equals("photo")){
            NodeList nl = el.getElementsByTagName("photo");
            if(nl != null && nl.getLength() > 0) {
                Element El = (Element)nl.item(0);
                p.create(El);
            }
            return;
        }
        if (type.equals("photoAlbum")){
            NodeList nl = el.getElementsByTagName("photoAlbum");
            if(nl != null && nl.getLength() > 0) {
                Element El = (Element)nl.item(0);
                pa.create(El);
            }
            return;
        }
        if (type.equals("deletedEntry")){
            NodeList nl = el.getElementsByTagName("deletedEntry");
            if(nl != null && nl.getLength() > 0) {
                Element El = (Element)nl.item(0);
                de.create(El);
            }
            return;
        }
        if (type.equals("event")){
            NodeList nl = el.getElementsByTagName("event");
            if(nl != null && nl.getLength() > 0) {
                Element El = (Element)nl.item(0);
                e.create(El);
            }
            return;
        }
        if (type.equals("message")){
            NodeList nl = el.getElementsByTagName("message");
            if(nl != null && nl.getLength() > 0) {
                Element El = (Element)nl.item(0);
                m.create(El);
            }
            return;
        }
        if (type.equals("mirror")){
            NodeList nl = el.getElementsByTagName("mirror");
            if(nl != null && nl.getLength() > 0) {
                Element El = (Element)nl.item(0);
                mi.create(El);
            }
            return;
        }
	}
    
    public int compareTo(timelineEntry o){
        if (this.id > o.id) return -1;
        if (this.id < o.id) return 1;
        return 0;
    }
}
