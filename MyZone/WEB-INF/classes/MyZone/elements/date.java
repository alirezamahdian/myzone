/* 
 =========================================================================
 |Copyright (C) 2012  Alireza Mahdian email:alireza.mahdian@colorado.edu |
 |This program is free software: you can redistribute it but NOT modify  |
 |it under the terms of the GNU General Public License as published by   |
 |the Free Software Foundation, either version 3 of the License, or      |
 |(at your option) any later version. Alireza Mahdian reserves all the   |
 |commit rights of this code.                                            |
 |                                                                       |
 |This program is distributed in the hope that it will be useful,        |
 |but WITHOUT ANY WARRANTY; without even the implied warranty of         |
 |MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          |
 |GNU General Public License for more details.                           |
 |                                                                       |
 |You should have received a copy of the GNU General Public License      |
 |along with this program.  If not, see <http://www.gnu.org/licenses/>.  |
 =========================================================================
 */

package MyZone.elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Text;

/*
 This file contains the implementation of the date element of a profile.
 Please refer to info.xsd for detailed description of its elements and 
 attributes.
 */

public class date {
    
    private int year;
    private int month;
	private int day;
    private int hour;
    private int minute;
    
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
    
    public date(){}
    
	public date(int year, int month, int day, int hour, int minute){
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
    }
	
    public date(date orig){
        this.year = orig.getYear();
        this.month = Integer.parseInt(orig.getMonth("numerical"));
        this.day = orig.getDay();
        this.hour = orig.getHour("24");
        this.minute = orig.getMinute();
    }
    
    public int getYear(){
        return year;
    }
    
    public int getDay(){
        return day;
    }
    
    public int getHour(String type){
        if (type.equals("12"))
            return (hour%12);
        return hour;
    }
    
    public int getMinute(){
        return minute;
    }
    
    public String getMinuteStr(){
        if (minute < 10){
            return ("0" + String.valueOf(minute));
        }
        return String.valueOf(minute);
    }
    
    public String getAMPM(){
        if (hour < 12)
            return "AM";
        return "PM";
    }
    
    public String getMonthStr(){
        if (month == 0)
            return "";
        if (month < 10){
            return ("0" + String.valueOf(month));
        }
        return String.valueOf(month);
    }
    
    public String getMonth(String type){
        if (type.equals("abstractAlphabetical")){
            if (month == 0){
                return " ";
            }
            if (month == 1){
                return "JAN";
            }
            if (month == 2){
                return "FEB";
            }
            if (month == 3){
                return "MAR";
            }
            if (month == 4){
                return "APR";
            }
            if (month == 5){
                return "MAY";
            }
            if (month == 6){
                return "JUN";
            }
            if (month == 7){
                return "JUL";
            }
            if (month == 8){
                return "AUG";
            }
            if (month == 9){
                return "SEP";
            }
            if (month == 10){
                return "OCT";
            }
            if (month == 11){
                return "NOV";
            }
            if (month == 12){
                return "DEC";
            }
        }else if(type.equals("completeAlphabetical")){
            if (month == 0){
                return " ";
            }
            if (month == 1){
                return "January";
            }
            if (month == 2){
                return "February";
            }
            if (month == 3){
                return "March";
            }
            if (month == 4){
                return "April";
            }
            if (month == 5){
                return "May";
            }
            if (month == 6){
                return "June";
            }
            if (month == 7){
                return "July";
            }
            if (month == 8){
                return "August";
            }
            if (month == 9){
                return "September";
            }
            if (month == 10){
                return "October";
            }
            if (month == 11){
                return "November";
            }
            if (month == 12){
                return "December";
            }
        }else if(type.equals("numerical")){
            return String.valueOf(month);
        }
        return "";
    }
    
    public void setYear(int year){
        this.year = year;
    }
    
    public void setMonth(int month){
        this.month = month;
    }
    
    public void setDay(int day){
        this.day = day;
    }
    
    public void setHour(int hour){
        this.hour = hour;
    }
    
    public void setMinute(int minute){
        this.minute = minute;
    }
    
	public void create(Element el) {
        if (getTextValue(el, "year") != null)
            year = Integer.parseInt(getTextValue(el, "year"));
        if (getTextValue(el, "month") != null)
            month = Integer.parseInt(getTextValue(el, "month"));
        if (getTextValue(el, "day") != null)
            day = Integer.parseInt(getTextValue(el, "day"));
        if (getTextValue(el, "hour") != null)
            hour = Integer.parseInt(getTextValue(el, "hour"));
        if (getTextValue(el, "minute") != null)
            minute = Integer.parseInt(getTextValue(el, "minute"));
	}
    
    public Element createDOMElement(Document dom){
        Element el = dom.createElement("date");
		
        Element yearEl = dom.createElement("year");
        Text yearText = dom.createTextNode(String.valueOf(year));
        yearEl.appendChild(yearText);
        el.appendChild(yearEl);
        
        Element monthEl = dom.createElement("month");
        Text monthText = dom.createTextNode(String.valueOf(month));
        monthEl.appendChild(monthText);
        el.appendChild(monthEl);
        
        Element dayEl = dom.createElement("day");
        Text dayText = dom.createTextNode(String.valueOf(day));
        dayEl.appendChild(dayText);
        el.appendChild(dayEl);
        Element hourEl = dom.createElement("hour");
        Text hourText = dom.createTextNode(String.valueOf(hour));
        hourEl.appendChild(hourText);
        el.appendChild(hourEl);
        Element minuteEl = dom.createElement("minute");
        Text minuteText = dom.createTextNode(String.valueOf(minute));
        minuteEl.appendChild(minuteText);
        el.appendChild(minuteEl);
        
		return el;
    }
    
    
    public String generateHTML(){
        String res = "";
        return res;
    }
    
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("date info \n");
		sb.append("year:" + year);
		sb.append("\n");
        sb.append("month:" + month);
		sb.append("\n");
        sb.append("day:" + day);
		sb.append("\n");
        sb.append("hour:" + hour);
		sb.append("\n");
        sb.append("minute:" + minute);
		sb.append("\n");
		return sb.toString();
	}
}
