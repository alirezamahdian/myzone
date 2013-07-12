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

package MyZone.elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Calendar;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Text;

import MyZone.elements.*;
import MyZone.timelineEntry;

/*
 This file contains the implementation of the pendingChange element.
 Please refer to pendingChanges.xsd for detailed description of its 
 elements and attributes.
 */

public class pendingChange {
    
    private long id;
    private String action;
    private String type;
    private String shareWith;
    private timelineEntry entry;
    private String belongsTo;
	
    private String getTextValue(Element ele, String tagName) {
		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);
		if(nl != null && nl.getLength() > 0) {
			Element el = (Element)nl.item(0);
            if (el.getFirstChild() != null)
                textVal = el.getFirstChild().getNodeValue();
		}
		return textVal;
	}
    
    public pendingChange(){
        entry = new timelineEntry();
    }
    
    public pendingChange(long id,
                         String action,
                         String type,
                         timelineEntry entry)
    {
        this.id = id;
        this.action = action;
        this.type = type;
        this.entry = new timelineEntry(entry);
        shareWith = "none";
        if (type.equals("link")){
            shareWith = entry.l.getShareWith();
            belongsTo = entry.l.getBelongsTo();
        }
        if (type.equals("wallPost")){
            shareWith = entry.s.getShareWith();
            belongsTo = entry.s.getBelongsTo();
        }
        if (type.equals("audioAlbum")){
            shareWith = entry.aa.getShareWith();
            belongsTo = entry.aa.getBelongsTo();
        }
        if (type.equals("videoAlbum")){
            shareWith = entry.va.getShareWith();
            belongsTo = entry.va.getBelongsTo();
        }
        if (type.equals("photoAlbum")){
            shareWith = entry.pa.getShareWith();
            belongsTo = entry.pa.getBelongsTo();
        }
        if (type.equals("audio")){
            shareWith = entry.a.getShareWith();
            belongsTo = entry.a.getBelongsTo();
        }
        if (type.equals("video")){
            shareWith = entry.v.getShareWith();
            belongsTo = entry.v.getBelongsTo();
        }
        if (type.equals("photo")){
            shareWith = entry.p.getShareWith();
            belongsTo = entry.p.getBelongsTo();
        }
        if (type.equals("linkComment")){
            List<comment> comments = entry.l.getComments();
            if (comments != null && comments.size() > 0)
            {
                comment c = (comment)comments.get(0);
                shareWith = c.getShareWith();
                belongsTo = c.getBelongsTo();
            }
        }
        if (type.equals("wallPostComment")){
            List<comment> comments = entry.s.getComments();
            if (comments != null && comments.size() > 0)
            {
                comment c = (comment)comments.get(0);
                shareWith = c.getShareWith();
                belongsTo = c.getBelongsTo();
            }
        }
        if (type.equals("audioAlbumComment")){
            List<comment> comments = entry.aa.getComments();
            if (comments != null && comments.size() > 0)
            {
                comment c = (comment)comments.get(0);
                shareWith = c.getShareWith();
                belongsTo = c.getBelongsTo();
            }
        }
        if (type.equals("videoAlbumComment")){
            List<comment> comments = entry.va.getComments();
            if (comments != null && comments.size() > 0)
            {
                comment c = (comment)comments.get(0);
                shareWith = c.getShareWith();
                belongsTo = c.getBelongsTo();
            }
        }
        if (type.equals("photoAlbumComment")){
            List<comment> comments = entry.pa.getComments();
            if (comments != null && comments.size() > 0)
            {
                comment c = (comment)comments.get(0);
                shareWith = c.getShareWith();
                belongsTo = c.getBelongsTo();
            }
        }
        if (type.equals("audioComment")){
            List<comment> comments = entry.a.getComments();
            if (comments != null && comments.size() > 0)
            {
                comment c = (comment)comments.get(0);
                shareWith = c.getShareWith();
                belongsTo = c.getBelongsTo();
            }
        }
        if (type.equals("videoComment")){
            List<comment> comments = entry.v.getComments();
            if (comments != null && comments.size() > 0)
            {
                comment c = (comment)comments.get(0);
                shareWith = c.getShareWith();
                belongsTo = c.getBelongsTo();
            }
        }
        if (type.equals("photoComment")){
            List<comment> comments = entry.p.getComments();
            if (comments != null && comments.size() > 0)
            {
                comment c = (comment)comments.get(0);
                shareWith = c.getShareWith();
                belongsTo = c.getBelongsTo();
            }
        }
        if (type.equals("linkLike")){
            List<like> likes = entry.l.getLikes();
            if (likes != null && likes.size() > 0)
            {
                like li = (like)likes.get(0);
                shareWith = li.getShareWith();
                belongsTo = li.getBelongsTo();
            }
        }
        if (type.equals("wallPostLike")){
            List<like> likes = entry.s.getLikes();
            if (likes != null && likes.size() > 0)
            {
                like li = (like)likes.get(0);
                shareWith = li.getShareWith();
                belongsTo = li.getBelongsTo();
            }
        }
        if (type.equals("audioAlbumLike")){
            List<like> likes = entry.aa.getLikes();
            if (likes != null && likes.size() > 0)
            {
                like li = (like)likes.get(0);
                shareWith = li.getShareWith();
                belongsTo = li.getBelongsTo();
            }
        }
        if (type.equals("videoAlbumLike")){
            List<like> likes = entry.va.getLikes();
            if (likes != null && likes.size() > 0)
            {
                like li = (like)likes.get(0);
                shareWith = li.getShareWith();
                belongsTo = li.getBelongsTo();
            }
        }
        if (type.equals("photoAlbumLike")){
            List<like> likes = entry.pa.getLikes();
            if (likes != null && likes.size() > 0)
            {
                like li = (like)likes.get(0);
                shareWith = li.getShareWith();
                belongsTo = li.getBelongsTo();
            }
        }
        if (type.equals("audioLike")){
            List<like> likes = entry.a.getLikes();
            if (likes != null && likes.size() > 0)
            {
                like li = (like)likes.get(0);
                shareWith = li.getShareWith();
                belongsTo = li.getBelongsTo();
            }
        }
        if (type.equals("videoLike")){
            List<like> likes = entry.v.getLikes();
            if (likes != null && likes.size() > 0)
            {
                like li = (like)likes.get(0);
                shareWith = li.getShareWith();
                belongsTo = li.getBelongsTo();
            }
        }
        if (type.equals("photoLike")){
            List<like> likes = entry.p.getLikes();
            if (likes != null && likes.size() > 0)
            {
                like li = (like)likes.get(0);
                shareWith = li.getShareWith();
                belongsTo = li.getBelongsTo();
            }
        }
        if (type.equals("linkDislike")){
            List<dislike> dislikes = entry.l.getDislikes();
            if (dislikes != null && dislikes.size() > 0)
            {
                dislike dli = (dislike)dislikes.get(0);
                shareWith = dli.getShareWith();
                belongsTo = dli.getBelongsTo();
            }
        }
        if (type.equals("wallPostDislike")){
            List<dislike> dislikes = entry.s.getDislikes();
            if (dislikes != null && dislikes.size() > 0)
            {
                dislike dli = (dislike)dislikes.get(0);
                shareWith = dli.getShareWith();
                belongsTo = dli.getBelongsTo();
            }
        }
        if (type.equals("audioAlbumDislike")){
            List<dislike> dislikes = entry.aa.getDislikes();
            if (dislikes != null && dislikes.size() > 0)
            {
                dislike dli = (dislike)dislikes.get(0);
                shareWith = dli.getShareWith();
                belongsTo = dli.getBelongsTo();
            }
        }
        if (type.equals("videoAlbumDislike")){
            List<dislike> dislikes = entry.va.getDislikes();
            if (dislikes != null && dislikes.size() > 0)
            {
                dislike dli = (dislike)dislikes.get(0);
                shareWith = dli.getShareWith();
                belongsTo = dli.getBelongsTo();
            }
        }
        if (type.equals("photoAlbumDislike")){
            List<dislike> dislikes = entry.pa.getDislikes();
            if (dislikes != null && dislikes.size() > 0)
            {
                dislike dli = (dislike)dislikes.get(0);
                shareWith = dli.getShareWith();
                belongsTo = dli.getBelongsTo();
            }
        }
        if (type.equals("audioDislike")){
            List<dislike> dislikes = entry.a.getDislikes();
            if (dislikes != null && dislikes.size() > 0)
            {
                dislike dli = (dislike)dislikes.get(0);
                shareWith = dli.getShareWith();
                belongsTo = dli.getBelongsTo();
            }
        }
        if (type.equals("videoDislike")){
            List<dislike> dislikes = entry.v.getDislikes();
            if (dislikes != null && dislikes.size() > 0)
            {
                dislike dli = (dislike)dislikes.get(0);
                shareWith = dli.getShareWith();
                belongsTo = dli.getBelongsTo();
            }
        }
        if (type.equals("photoDislike")){
            List<dislike> dislikes = entry.p.getDislikes();
            if (dislikes != null && dislikes.size() > 0)
            {
                dislike dli = (dislike)dislikes.get(0);
                shareWith = dli.getShareWith();
                belongsTo = dli.getBelongsTo();
            }
        }
    }
    
    public pendingChange(pendingChange orig)
    {
        this.id = orig.getId();
        this.action = orig.getAction();
        this.type = orig.getType();
        this.shareWith = orig.getShareWith();
        this.entry = new timelineEntry(orig.getEntry());
        this.belongsTo = orig.getBelongsTo();
    }
    
    public long getId(){
        return id;
    }
    
    public void setId(long id){
        this.id = id;
    }
    
    public String getAction(){
        return action;
    }
    
    public void setAction(String action){
        this.action = action;
    }
    
    public String getType(){
        return type;
    }
    
    public void setType(String type){
        this.type = type;
    }
    
    public String getShareWith(){
        return shareWith;
    }
    
    public void setShareWith(String shareWith){
        this.shareWith = shareWith;
    }
    
    public String getBelongsTo(){
        return belongsTo;
    }
    
    public void setBelongsTo(String belongsTo){
        this.belongsTo = belongsTo;
    }
    
    public timelineEntry getEntry(){
        return entry;
    }
    
    public void setEntry(timelineEntry entry){
        this.entry = new timelineEntry(entry);
    }
    
	public void create(Element el) {
		id = Long.parseLong(el.getAttribute("id"));
        action = el.getAttribute("action");
        type = el.getAttribute("type");
        shareWith = el.getAttribute("shareWith");
        belongsTo = el.getAttribute("belongsTo");
        
        if (type.equals("event")){
            NodeList events = el.getElementsByTagName("event");
            if(events != null && events.getLength() > 0) {
                Element eventEl = (Element)events.item(0);
                event e = new event();
                e.create(eventEl);
                entry = new timelineEntry(e.getId(), "event", e);
            }
        }
        if (type.equals("mirror")){
            NodeList mirrors = el.getElementsByTagName("mirror");
            if(mirrors != null && mirrors.getLength() > 0) {
                Element mirrorEl = (Element)mirrors.item(0);
                mirror m = new mirror();
                m.create(mirrorEl);
                entry = new timelineEntry(m.getId(), "mirror", m);
            }
        }
        
        if (type.equals("message")){
            NodeList messages = el.getElementsByTagName("message");
            if(messages != null && messages.getLength() > 0) {
                Element messageEl = (Element)messages.item(0);
                message m = new message();
                m.create(messageEl);
                entry = new timelineEntry(m.getId(), "message", m);
            }
        }
        if (type.equals("link")){
            NodeList links = el.getElementsByTagName("link");
            if(links != null && links.getLength() > 0) {
                Element linkEl = (Element)links.item(0);
                link l = new link();
                l.create(linkEl);
                entry = new timelineEntry(l.getId(), "link", l);
            }
        }
        if (type.equals("wallPost")){
            NodeList wallPosts = el.getElementsByTagName("wallPost");
            if(wallPosts != null && wallPosts.getLength() > 0) {
                Element wallPostEl = (Element)wallPosts.item(0);
                wallPost s = new wallPost();
                s.create(wallPostEl);
                entry = new timelineEntry(s.getId(), "wallPost", s);
            }
        }
        if (type.equals("audioAlbum")){
            NodeList audioAlbums = el.getElementsByTagName("audioAlbum");
            if(audioAlbums != null && audioAlbums.getLength() > 0) {
                Element audioAlbumEl = (Element)audioAlbums.item(0);
                audioAlbum aa = new audioAlbum();
                aa.create(audioAlbumEl);
                entry = new timelineEntry(aa.getId(), "audioAlbum", aa);
            }
        }
        if (type.equals("videoAlbum")){
            NodeList videoAlbums = el.getElementsByTagName("videoAlbum");
            if(videoAlbums != null && videoAlbums.getLength() > 0) {
                Element videoAlbumEl = (Element)videoAlbums.item(0);
                videoAlbum va = new videoAlbum();
                va.create(videoAlbumEl);
                entry = new timelineEntry(va.getId(), "videoAlbum", va);
            }
        }
        if (type.equals("photoAlbum")){
            NodeList photoAlbums = el.getElementsByTagName("photoAlbum");
            if(photoAlbums != null && photoAlbums.getLength() > 0) {
                Element photoAlbumEl = (Element)photoAlbums.item(0);
                photoAlbum pa = new photoAlbum();
                pa.create(photoAlbumEl);
                entry = new timelineEntry(pa.getId(), "photoAlbum", pa);
            }
        }
        if (type.equals("audio")){
            NodeList audios = el.getElementsByTagName("audio");
            if(audios != null && audios.getLength() > 0) {
                Element audioEl = (Element)audios.item(0);
                audio a = new audio();
                a.create(audioEl);
                entry = new timelineEntry(a.getId(), "audio", a);
            }
        }
        if (type.equals("video")){
            NodeList videos = el.getElementsByTagName("video");
            if(videos != null && videos.getLength() > 0) {
                Element videoEl = (Element)videos.item(0);
                video v = new video();
                v.create(videoEl);
                entry = new timelineEntry(v.getId(), "video", v);
            }
        }
        if (type.equals("photo")){
            NodeList photos = el.getElementsByTagName("photo");
            if(photos != null && photos.getLength() > 0) {
                Element photoEl = (Element)photos.item(0);
                photo p = new photo();
                p.create(photoEl);
                entry = new timelineEntry(p.getId(), "photo", p);
            }
        }
        if (type.equals("linkComment")){
            NodeList comments = el.getElementsByTagName("comment");
            if(comments != null && comments.getLength() > 0) {
                Element commentEl = (Element)comments.item(0);
                link l = new link();
                comment c = new comment();
                c.create(commentEl);
                l.addComment(c);
                l.setBelongsTo(c.getBelongsTo());
                l.setId(c.getParent());
                entry = new timelineEntry(0, "link", l);
            }
        }
        if (type.equals("wallPostComment")){
            NodeList comments = el.getElementsByTagName("comment");
            if(comments != null && comments.getLength() > 0) {
                Element commentEl = (Element)comments.item(0);
                wallPost s = new wallPost();
                comment c = new comment();
                c.create(commentEl);
                s.addComment(c);
                s.setBelongsTo(c.getBelongsTo());
                s.setId(c.getParent());
                entry = new timelineEntry(0, "wallPost", s);
            }
        }
        if (type.equals("audioAlbumComment")){
            NodeList comments = el.getElementsByTagName("comment");
            if(comments != null && comments.getLength() > 0) {
                Element commentEl = (Element)comments.item(0);
                audioAlbum aa = new audioAlbum();
                comment c = new comment();
                c.create(commentEl);
                aa.addComment(c);
                aa.setBelongsTo(c.getBelongsTo());
                aa.setId(c.getParent());
                entry = new timelineEntry(0, "audioAlbum", aa);
            }
        }
        if (type.equals("videoAlbumComment")){
            NodeList comments = el.getElementsByTagName("comment");
            if(comments != null && comments.getLength() > 0) {
                Element commentEl = (Element)comments.item(0);
                videoAlbum va = new videoAlbum();
                comment c = new comment();
                c.create(commentEl);
                va.addComment(c);
                va.setBelongsTo(c.getBelongsTo());
                va.setId(c.getParent());
                entry = new timelineEntry(0, "videoAlbum", va);
            }
        }
        if (type.equals("photoAlbumComment")){
            NodeList comments = el.getElementsByTagName("comment");
            if(comments != null && comments.getLength() > 0) {
                Element commentEl = (Element)comments.item(0);
                photoAlbum pa = new photoAlbum();
                comment c = new comment();
                c.create(commentEl);
                pa.addComment(c);
                pa.setBelongsTo(c.getBelongsTo());
                pa.setId(c.getParent());
                entry = new timelineEntry(0, "photoAlbum", pa);
            }
        }
        if (type.equals("audioComment")){
            NodeList comments = el.getElementsByTagName("comment");
            if(comments != null && comments.getLength() > 0) {
                Element commentEl = (Element)comments.item(0);
                audio a = new audio();
                comment c = new comment();
                c.create(commentEl);
                a.addComment(c);
                a.setBelongsTo(c.getBelongsTo());
                a.setParent(c.getGrandParent());
                a.setId(c.getParent());
                entry = new timelineEntry(0, "audio", a);
            }
        }
        if (type.equals("videoComment")){
            NodeList comments = el.getElementsByTagName("comment");
            if(comments != null && comments.getLength() > 0) {
                Element commentEl = (Element)comments.item(0);
                video v = new video();
                comment c = new comment();
                c.create(commentEl);
                v.addComment(c);
                v.setBelongsTo(c.getBelongsTo());
                v.setParent(c.getGrandParent());
                v.setId(c.getParent());
                entry = new timelineEntry(0, "video", v);
            }
        }
        if (type.equals("photoComment")){
            NodeList comments = el.getElementsByTagName("comment");
            if(comments != null && comments.getLength() > 0) {
                Element commentEl = (Element)comments.item(0);
                photo p = new photo();
                comment c = new comment();
                c.create(commentEl);
                p.addComment(c);
                p.setBelongsTo(c.getBelongsTo());
                p.setParent(c.getGrandParent());
                p.setId(c.getParent());
                entry = new timelineEntry(0, "photo", p);
            }
        }
        if (type.equals("linkLike")){
            NodeList likes = el.getElementsByTagName("like");
            if(likes != null && likes.getLength() > 0) {
                Element likeEl = (Element)likes.item(0);
                link l = new link();
                like li = new like();
                li.create(likeEl);
                l.addlike(li);
                l.setBelongsTo(li.getBelongsTo());
                l.setId(li.getParent());
                entry = new timelineEntry(0, "link", l);
            }
        }
        if (type.equals("wallPostLike")){
            NodeList likes = el.getElementsByTagName("like");
            if(likes != null && likes.getLength() > 0) {
                Element likeEl = (Element)likes.item(0);
                wallPost s = new wallPost();
                like li = new like();
                li.create(likeEl);
                s.addlike(li);
                s.setBelongsTo(li.getBelongsTo());
                s.setId(li.getParent());
                entry = new timelineEntry(0, "wallPost", s);
            }
        }
        if (type.equals("audioAlbumLike")){
            NodeList likes = el.getElementsByTagName("like");
            if(likes != null && likes.getLength() > 0) {
                Element likeEl = (Element)likes.item(0);
                audioAlbum aa = new audioAlbum();
                like li = new like();
                li.create(likeEl);
                aa.addlike(li);
                aa.setBelongsTo(li.getBelongsTo());
                aa.setId(li.getParent());
                entry = new timelineEntry(0, "audioAlbum", aa);
            }
        }
        if (type.equals("videoAlbumLike")){
            NodeList likes = el.getElementsByTagName("like");
            if(likes != null && likes.getLength() > 0) {
                Element likeEl = (Element)likes.item(0);
                videoAlbum va = new videoAlbum();
                like li = new like();
                li.create(likeEl);
                va.addlike(li);
                va.setBelongsTo(li.getBelongsTo());
                va.setId(li.getParent());
                entry = new timelineEntry(0, "videoAlbum", va);
            }
        }
        if (type.equals("photoAlbumLike")){
            NodeList likes = el.getElementsByTagName("like");
            if(likes != null && likes.getLength() > 0) {
                Element likeEl = (Element)likes.item(0);
                photoAlbum pa = new photoAlbum();
                like li = new like();
                li.create(likeEl);
                pa.addlike(li);
                pa.setBelongsTo(li.getBelongsTo());
                pa.setId(li.getParent());
                entry = new timelineEntry(0, "photoAlbum", pa);
            }
        }
        if (type.equals("audioLike")){
            NodeList likes = el.getElementsByTagName("like");
            if(likes != null && likes.getLength() > 0) {
                Element likeEl = (Element)likes.item(0);
                audio a = new audio();
                like li = new like();
                li.create(likeEl);
                a.addlike(li);
                a.setBelongsTo(li.getBelongsTo());
                a.setParent(li.getGrandParent());
                a.setId(li.getParent());
                entry = new timelineEntry(0, "audio", a);
            }
        }
        if (type.equals("videoLike")){
            NodeList likes = el.getElementsByTagName("like");
            if(likes != null && likes.getLength() > 0) {
                Element likeEl = (Element)likes.item(0);
                video v = new video();
                like li = new like();
                li.create(likeEl);
                v.addlike(li);
                v.setBelongsTo(li.getBelongsTo());
                v.setParent(li.getGrandParent());
                v.setId(li.getParent());
                entry = new timelineEntry(0, "video", v);
            }
        }
        if (type.equals("photoLike")){
            NodeList likes = el.getElementsByTagName("like");
            if(likes != null && likes.getLength() > 0) {
                Element likeEl = (Element)likes.item(0);
                photo p = new photo();
                like li = new like();
                li.create(likeEl);
                p.addlike(li);
                p.setBelongsTo(li.getBelongsTo());
                p.setParent(li.getGrandParent());
                p.setId(li.getParent());
                entry = new timelineEntry(0, "photo", p);
            }
        }
        if (type.equals("linkDislike")){
            NodeList dislikes = el.getElementsByTagName("dislike");
            if(dislikes != null && dislikes.getLength() > 0) {
                Element dislikeEl = (Element)dislikes.item(0);
                link l = new link();
                dislike dli = new dislike();
                dli.create(dislikeEl);
                l.addDislike(dli);
                l.setBelongsTo(dli.getBelongsTo());
                l.setId(dli.getParent());
                entry = new timelineEntry(0, "link", l);
            }
        }
        if (type.equals("wallPostDislike")){
            NodeList dislikes = el.getElementsByTagName("dislike");
            if(dislikes != null && dislikes.getLength() > 0) {
                Element dislikeEl = (Element)dislikes.item(0);
                wallPost s = new wallPost();
                dislike dli = new dislike();
                dli.create(dislikeEl);
                s.addDislike(dli);
                s.setBelongsTo(dli.getBelongsTo());
                s.setId(dli.getParent());
                entry = new timelineEntry(0, "wallPost", s);
            }
        }
        if (type.equals("audioAlbumDislike")){
            NodeList dislikes = el.getElementsByTagName("dislike");
            if(dislikes != null && dislikes.getLength() > 0) {
                Element dislikeEl = (Element)dislikes.item(0);
                audioAlbum aa = new audioAlbum();
                dislike dli = new dislike();
                dli.create(dislikeEl);
                aa.addDislike(dli);
                aa.setBelongsTo(dli.getBelongsTo());
                aa.setId(dli.getParent());
                entry = new timelineEntry(0, "audioAlbum", aa);
            }
        }
        if (type.equals("videoAlbumDislike")){
            NodeList dislikes = el.getElementsByTagName("dislike");
            if(dislikes != null && dislikes.getLength() > 0) {
                Element dislikeEl = (Element)dislikes.item(0);
                videoAlbum va = new videoAlbum();
                dislike dli = new dislike();
                dli.create(dislikeEl);
                va.addDislike(dli);
                va.setBelongsTo(dli.getBelongsTo());
                va.setId(dli.getParent());
                entry = new timelineEntry(0, "videoAlbum", va);
            }
        }
        if (type.equals("photoAlbumDislike")){
            NodeList dislikes = el.getElementsByTagName("dislike");
            if(dislikes != null && dislikes.getLength() > 0) {
                Element dislikeEl = (Element)dislikes.item(0);
                photoAlbum pa = new photoAlbum();
                dislike dli = new dislike();
                dli.create(dislikeEl);
                pa.addDislike(dli);
                pa.setBelongsTo(dli.getBelongsTo());
                pa.setId(dli.getParent());
                entry = new timelineEntry(0, "photoAlbum", pa);
            }
        }
        if (type.equals("audioDislike")){
            NodeList dislikes = el.getElementsByTagName("dislike");
            if(dislikes != null && dislikes.getLength() > 0) {
                Element dislikeEl = (Element)dislikes.item(0);
                audio a = new audio();
                dislike dli = new dislike();
                dli.create(dislikeEl);
                a.addDislike(dli);
                a.setBelongsTo(dli.getBelongsTo());
                a.setParent(dli.getGrandParent());
                a.setId(dli.getParent());
                entry = new timelineEntry(0, "audio", a);
            }
        }
        if (type.equals("videoDislike")){
            NodeList dislikes = el.getElementsByTagName("dislike");
            if(dislikes != null && dislikes.getLength() > 0) {
                Element dislikeEl = (Element)dislikes.item(0);
                video v = new video();
                dislike dli = new dislike();
                dli.create(dislikeEl);
                v.addDislike(dli);
                v.setBelongsTo(dli.getBelongsTo());
                v.setParent(dli.getGrandParent());
                v.setId(dli.getParent());
                entry = new timelineEntry(0, "video", v);
            }
        }
        if (type.equals("photoDislike")){
            NodeList dislikes = el.getElementsByTagName("dislike");
            if(dislikes != null && dislikes.getLength() > 0) {
                Element dislikeEl = (Element)dislikes.item(0);
                photo p = new photo();
                dislike dli = new dislike();
                dli.create(dislikeEl);
                p.addDislike(dli);
                p.setBelongsTo(dli.getBelongsTo());
                p.setParent(dli.getGrandParent());
                p.setId(dli.getParent());
                entry = new timelineEntry(0, "photo", p);
            }
        }
        
	}
    
    public Element createDOMElement(Document dom){
        Element el = dom.createElement("pendingChange");
		
        el.setAttribute("id", String.valueOf(id));
        el.setAttribute("action", action);
        el.setAttribute("type", type);
        el.setAttribute("shareWith", shareWith);
        el.setAttribute("belongsTo", belongsTo);
        if (type.equals("mirror")){
            el.appendChild(entry.mi.createDOMElement(dom));
        }
        if (type.equals("event")){
            el.appendChild(entry.e.createDOMElement(dom));
        }
        if (type.equals("message")){
            el.appendChild(entry.m.createDOMElement(dom));
        }
        if (type.equals("link")){
            el.appendChild(entry.l.createDOMElement(dom));
        }
        if (type.equals("wallPost")){
            el.appendChild(entry.s.createDOMElement(dom));
        }
        if (type.equals("audioAlbum")){
            el.appendChild(entry.aa.createDOMElement(dom));
        }
        if (type.equals("videoAlbum")){
            el.appendChild(entry.va.createDOMElement(dom));
        }
        if (type.equals("photoAlbum")){
            el.appendChild(entry.pa.createDOMElement(dom));
        }
        if (type.equals("audio")){
            el.appendChild(entry.a.createDOMElement(dom));
        }
        if (type.equals("video")){
            el.appendChild(entry.v.createDOMElement(dom));
        }
        if (type.equals("photo")){
            el.appendChild(entry.p.createDOMElement(dom));
        }
        if (type.equals("linkComment")){
            List<comment> comments = entry.l.getComments();
            if (comments != null && comments.size() > 0){
                comment c = comments.get(0);
                el.appendChild(c.createDOMElement(dom));
            }
        }
        if (type.equals("wallPostComment")){
            List<comment> comments = entry.s.getComments();
            if (comments != null && comments.size() > 0){
                comment c = comments.get(0);
                el.appendChild(c.createDOMElement(dom));
            }
        }
        if (type.equals("audioAlbumComment")){
            List<comment> comments = entry.aa.getComments();
            if (comments != null && comments.size() > 0){
                comment c = comments.get(0);
                el.appendChild(c.createDOMElement(dom));
            }
        }
        if (type.equals("videoAlbumComment")){
            List<comment> comments = entry.va.getComments();
            if (comments != null && comments.size() > 0){
                comment c = comments.get(0);
                el.appendChild(c.createDOMElement(dom));
            }
        }
        if (type.equals("photoAlbumComment")){
            List<comment> comments = entry.pa.getComments();
            if (comments != null && comments.size() > 0){
                comment c = comments.get(0);
                el.appendChild(c.createDOMElement(dom));
            }
        }
        if (type.equals("audioComment")){
            List<comment> comments = entry.a.getComments();
            if (comments != null && comments.size() > 0){
                comment c = comments.get(0);
                el.appendChild(c.createDOMElement(dom));
            }
        }
        if (type.equals("videoComment")){
            List<comment> comments = entry.v.getComments();
            if (comments != null && comments.size() > 0){
                comment c = comments.get(0);
                el.appendChild(c.createDOMElement(dom));
            }
        }
        if (type.equals("photoComment")){
            List<comment> comments = entry.p.getComments();
            if (comments != null && comments.size() > 0){
                comment c = comments.get(0);
                el.appendChild(c.createDOMElement(dom));
            }
        }
        if (type.equals("linkLike")){
            List<like> likes = entry.l.getLikes();
            if (likes != null && likes.size() > 0){
                like li = likes.get(0);
                el.appendChild(li.createDOMElement(dom));
            }
        }
        if (type.equals("wallPostLike")){
            List<like> likes = entry.s.getLikes();
            if (likes != null && likes.size() > 0){
                like li = likes.get(0);
                el.appendChild(li.createDOMElement(dom));
            }
        }
        if (type.equals("audioAlbumLike")){
            List<like> likes = entry.aa.getLikes();
            if (likes != null && likes.size() > 0){
                like li = likes.get(0);
                el.appendChild(li.createDOMElement(dom));
            }
        }
        if (type.equals("videoAlbumLike")){
            List<like> likes = entry.va.getLikes();
            if (likes != null && likes.size() > 0){
                like li = likes.get(0);
                el.appendChild(li.createDOMElement(dom));
            }
        }
        if (type.equals("photoAlbumLike")){
            List<like> likes = entry.pa.getLikes();
            if (likes != null && likes.size() > 0){
                like li = likes.get(0);
                el.appendChild(li.createDOMElement(dom));
            }
        }
        if (type.equals("audioLike")){
            List<like> likes = entry.a.getLikes();
            if (likes != null && likes.size() > 0){
                like li = likes.get(0);
                el.appendChild(li.createDOMElement(dom));
            }
        }
        if (type.equals("videoLike")){
            List<like> likes = entry.v.getLikes();
            if (likes != null && likes.size() > 0){
                like li = likes.get(0);
                el.appendChild(li.createDOMElement(dom));
            }
        }
        if (type.equals("photoLike")){
            List<like> likes = entry.p.getLikes();
            if (likes != null && likes.size() > 0){
                like li = likes.get(0);
                el.appendChild(li.createDOMElement(dom));
            }
        }
        if (type.equals("linkDislike")){
            List<dislike> dislikes = entry.l.getDislikes();
            if (dislikes != null && dislikes.size() > 0){
                dislike dli = dislikes.get(0);
                el.appendChild(dli.createDOMElement(dom));
            }
        }
        if (type.equals("wallPostDislike")){
            List<dislike> dislikes = entry.s.getDislikes();
            if (dislikes != null && dislikes.size() > 0){
                dislike dli = dislikes.get(0);
                el.appendChild(dli.createDOMElement(dom));
            }
        }
        if (type.equals("audioAlbumDislike")){
            List<dislike> dislikes = entry.aa.getDislikes();
            if (dislikes != null && dislikes.size() > 0){
                dislike dli = dislikes.get(0);
                el.appendChild(dli.createDOMElement(dom));
            }
        }
        if (type.equals("videoAlbumDislike")){
            List<dislike> dislikes = entry.va.getDislikes();
            if (dislikes != null && dislikes.size() > 0){
                dislike dli = dislikes.get(0);
                el.appendChild(dli.createDOMElement(dom));
            }
        }
        if (type.equals("photoAlbumDislike")){
            List<dislike> dislikes = entry.pa.getDislikes();
            if (dislikes != null && dislikes.size() > 0){
                dislike dli = dislikes.get(0);
                el.appendChild(dli.createDOMElement(dom));
            }
        }
        if (type.equals("audioDislike")){
            List<dislike> dislikes = entry.a.getDislikes();
            if (dislikes != null && dislikes.size() > 0){
                dislike dli = dislikes.get(0);
                el.appendChild(dli.createDOMElement(dom));
            }
        }
        if (type.equals("videoDislike")){
            List<dislike> dislikes = entry.v.getDislikes();
            if (dislikes != null && dislikes.size() > 0){
                dislike dli = dislikes.get(0);
                el.appendChild(dli.createDOMElement(dom));
            }
        }
        if (type.equals("photoDislike")){
            List<dislike> dislikes = entry.p.getDislikes();
            if (dislikes != null && dislikes.size() > 0){
                dislike dli = dislikes.get(0);
                el.appendChild(dli.createDOMElement(dom));
            }
        }
		return el;
    }
    
    public String generateHTML(String type){
        String res = "";
        return res;
    }
    
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("pendingChange info \n");
		sb.append("id:" + id);
		sb.append("\n");
        sb.append("action:" + action);
		sb.append("\n");
        sb.append("type:" + type);
		sb.append("\n");
        sb.append("shareWith:" + shareWith);
		sb.append("\n");
        sb.append("belongsTo:" + belongsTo);
		sb.append("\n");
        if (type.equals("event")){
            sb.append(entry.e);
        }
        if (type.equals("mirror")){
            sb.append(entry.mi);
        }
        if (type.equals("message")){
            sb.append(entry.m);
        }
        if (type.equals("link")){
            sb.append(entry.l);
        }
        if (type.equals("wallPost")){
            sb.append(entry.s);
        }
		if (type.equals("audioAlbum")){
            sb.append(entry.aa);
        }
        if (type.equals("videoAlbum")){
            sb.append(entry.va);
        }
        if (type.equals("photoAlbum")){
            sb.append(entry.pa);
        }
        if (type.equals("audio")){
            sb.append(entry.a);
        }
        if (type.equals("video")){
            sb.append(entry.v);
        }
        if (type.equals("photo")){
            sb.append(entry.p);
        }
        if (type.equals("linkComment")){
            List<comment> comments = entry.l.getComments();
            if (comments != null && comments.size() > 0)
            {
                comment c = comments.get(0);
                sb.append(c);
            }
        }
        if (type.equals("wallPostComment")){
            List<comment> comments = entry.s.getComments();
            if (comments != null && comments.size() > 0)
            {
                comment c = comments.get(0);
                sb.append(c);
            }
        }
        if (type.equals("audioAlbumComment")){
            List<comment> comments = entry.aa.getComments();
            if (comments != null && comments.size() > 0)
            {
                comment c = comments.get(0);
                sb.append(c);
            }
        }
        if (type.equals("videoAlbumComment")){
            List<comment> comments = entry.va.getComments();
            if (comments != null && comments.size() > 0)
            {
                comment c = comments.get(0);
                sb.append(c);
            }
        }
        if (type.equals("photoAlbumComment")){
            List<comment> comments = entry.pa.getComments();
            if (comments != null && comments.size() > 0)
            {
                comment c = comments.get(0);
                sb.append(c);
            }
        }
        if (type.equals("audioComment")){
            List<comment> comments = entry.a.getComments();
            if (comments != null && comments.size() > 0)
            {
                comment c = comments.get(0);
                sb.append(c);
            }
        }
        if (type.equals("videoComment")){
            List<comment> comments = entry.v.getComments();
            if (comments != null && comments.size() > 0)
            {
                comment c = comments.get(0);
                sb.append(c);
            }
        }
        if (type.equals("photoComment")){
            List<comment> comments = entry.p.getComments();
            if (comments != null && comments.size() > 0)
            {
                comment c = comments.get(0);
                sb.append(c);
            }
        }
        if (type.equals("linkLike")){
            List<like> likes = entry.l.getLikes();
            if (likes != null && likes.size() > 0)
            {
                like li = likes.get(0);
                sb.append(li);
            }
        }
        if (type.equals("wallPostLike")){
            List<like> likes = entry.s.getLikes();
            if (likes != null && likes.size() > 0)
            {
                like li = likes.get(0);
                sb.append(li);
            }
        }
        if (type.equals("audioAlbumLike")){
            List<like> likes = entry.aa.getLikes();
            if (likes != null && likes.size() > 0)
            {
                like li = likes.get(0);
                sb.append(li);
            }
        }
        if (type.equals("videoAlbumLike")){
            List<like> likes = entry.va.getLikes();
            if (likes != null && likes.size() > 0)
            {
                like li = likes.get(0);
                sb.append(li);
            }
        }
        if (type.equals("photoAlbumLike")){
            List<like> likes = entry.pa.getLikes();
            if (likes != null && likes.size() > 0)
            {
                like li = likes.get(0);
                sb.append(li);
            }
        }
        if (type.equals("audioLike")){
            List<like> likes = entry.a.getLikes();
            if (likes != null && likes.size() > 0)
            {
                like li = likes.get(0);
                sb.append(li);
            }
        }
        if (type.equals("videoLike")){
            List<like> likes = entry.v.getLikes();
            if (likes != null && likes.size() > 0)
            {
                like li = likes.get(0);
                sb.append(li);
            }
        }
        if (type.equals("photoLike")){
            List<like> likes = entry.p.getLikes();
            if (likes != null && likes.size() > 0)
            {
                like li = likes.get(0);
                sb.append(li);
            }
        }
        if (type.equals("linkDislike")){
            List<dislike> dislikes = entry.l.getDislikes();
            if (dislikes != null && dislikes.size() > 0)
            {
                dislike dli = dislikes.get(0);
                sb.append(dli);
            }
        }
        if (type.equals("wallPostDislike")){
            List<dislike> dislikes = entry.s.getDislikes();
            if (dislikes != null && dislikes.size() > 0)
            {
                dislike dli = dislikes.get(0);
                sb.append(dli);
            }
        }
        if (type.equals("audioAlbumDislike")){
            List<dislike> dislikes = entry.aa.getDislikes();
            if (dislikes != null && dislikes.size() > 0)
            {
                dislike dli = dislikes.get(0);
                sb.append(dli);
            }
        }
        if (type.equals("videoAlbumDislike")){
            List<dislike> dislikes = entry.va.getDislikes();
            if (dislikes != null && dislikes.size() > 0)
            {
                dislike dli = dislikes.get(0);
                sb.append(dli);
            }
        }
        if (type.equals("photoAlbumDislike")){
            List<dislike> dislikes = entry.pa.getDislikes();
            if (dislikes != null && dislikes.size() > 0)
            {
                dislike dli = dislikes.get(0);
                sb.append(dli);
            }
        }
        if (type.equals("audioDislike")){
            List<dislike> dislikes = entry.a.getDislikes();
            if (dislikes != null && dislikes.size() > 0)
            {
                dislike dli = dislikes.get(0);
                sb.append(dli);
            }
        }
        if (type.equals("videoDislike")){
            List<dislike> dislikes = entry.v.getDislikes();
            if (dislikes != null && dislikes.size() > 0)
            {
                dislike dli = dislikes.get(0);
                sb.append(dli);
            }
        }
        if (type.equals("photoDislike")){
            List<dislike> dislikes = entry.p.getDislikes();
            if (dislikes != null && dislikes.size() > 0)
            {
                dislike dli = dislikes.get(0);
                sb.append(dli);
            }
        }
		return sb.toString();
	}
}
