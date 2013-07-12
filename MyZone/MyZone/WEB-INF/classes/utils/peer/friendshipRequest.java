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

package utils.peer;

public class friendshipRequest{
    public String requesterID;
    public String passphrase;
    public friendshipRequest(friendshipRequest orig){
        requesterID = new String(orig.requesterID);
        passphrase = new String(orig.passphrase);
    }
    public friendshipRequest(){
        
    }
    
    public String toString(){
        String res = "Friendship Request: " + requesterID + " " + passphrase;
        return res;
    }
}