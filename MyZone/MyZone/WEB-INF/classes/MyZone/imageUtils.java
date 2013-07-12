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

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/*
 This file implements the auxilary class imageUtils. imageUtils is used to 
 create thumbnail from a profile image as well as computing the width and 
 height of an image. 
 */

public class imageUtils {
    
    private final static boolean DEBUG = false;
    private BufferedImage originalImage;
    private int type;
    
	public imageUtils(File f){
        try{
            originalImage = ImageIO.read(f);
            type = originalImage.getType() == 0? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
    }
    
    public int getWidth(){
        return originalImage.getWidth();
    }
    
    public int getHeight(){
        return originalImage.getHeight();
    }
    
    public boolean resizeImageWithHint(String filename, int width, int height){
        try{
            BufferedImage resizedImage = new BufferedImage(width, height, type);
            Graphics2D g = resizedImage.createGraphics();
            if (originalImage.getWidth() > originalImage.getHeight()){
                g.drawImage(originalImage, 0, 0, width, height, (int)((originalImage.getWidth() - originalImage.getHeight()) / 2), 0, ((originalImage.getWidth() - originalImage.getHeight()) / 2) + originalImage.getHeight(), originalImage.getHeight(), null);
            }else{
                g.drawImage(originalImage, 0, 0, width, height, 0, (int)((originalImage.getHeight() - originalImage.getWidth()) / 2), originalImage.getWidth(), ((originalImage.getHeight() - originalImage.getWidth()) / 2) + originalImage.getWidth(), null);
            }
            g.dispose();	
            g.setComposite(AlphaComposite.Src);
            
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                               RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
            ImageIO.write(resizedImage, "jpg", new File(filename));
        }catch(Exception e){
            if (DEBUG){
                e.printStackTrace();
            }
        }
        return true;
    }	
}