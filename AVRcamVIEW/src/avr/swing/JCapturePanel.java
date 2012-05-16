/*
    AVRcamVIEW: A PC application to test out the functionallity of the
     AVRcam real-time image processing engine.
    Copyright (C) 2004    Brent A. Taylor

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public
    License as published by the Free Software Foundation; either
    version 2 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public
    License along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

   For more information on the AVRcamVIEW, please contact:

   taylorba@comcast.net

   or go to www.jrobot.net for more details regarding the system.
*/

package avr.swing;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import javax.swing.*;

import avr.lang.*;

public class JCapturePanel extends JPanel {

   // constant variables for the interpolate state machine
   private static final int RED        = 0x01;
   private static final int BLUE       = 0x02;
   private static final int GREEN_EVEN = 0x04;
   private static final int GREEN_ODD  = 0x08;

   private BufferedImage bayerImage;
   private BufferedImage image;

   public JCapturePanel() {
      super(null);

      bayerImage = new BufferedImage(AVRSystem.IMAGE_WIDTH, AVRSystem.IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
      image = new BufferedImage(AVRSystem.IMAGE_WIDTH, AVRSystem.IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);

   }

   public void paintComponent(Graphics g) {
      super.paintComponent(g);
      if(bayerImage != null) {

         Dimension size = getSize();
         Insets insets = getInsets();

         Image offscreenImage = this.createImage(size.width, size.height);

         Graphics2D g2d = (Graphics2D)offscreenImage.getGraphics();

         // scale the images so they fit side by side for either the full
         // width or full height of the window
         double xScale = size.width / (double)(insets.left + 5 + AVRSystem.IMAGE_WIDTH + 10 + AVRSystem.IMAGE_WIDTH + 5 + insets.right);
         double yScale = size.height / (double)(insets.top + 5 + AVRSystem.IMAGE_HEIGHT + 5 + insets.bottom);
         double scale = Math.min(xScale, yScale);

         g2d.scale(scale, scale);

         g2d.drawImage(bayerImage, insets.left + 5, insets.top + 5, null);
         g2d.drawImage(image, insets.left + 5 + AVRSystem.IMAGE_WIDTH + 10, insets.top + 5, null);

         g.drawImage(offscreenImage, 0, 0, null);

      }
   }

   public Dimension getMinimumSize() {
      return getPreferredSize();
   }

   public Dimension getPreferredSize() {
      Insets insets = getInsets();
      return new Dimension(insets.left + 5 + AVRSystem.IMAGE_WIDTH + 10 + AVRSystem.IMAGE_WIDTH + 5 + insets.right,
                           insets.top + 5 + AVRSystem.IMAGE_HEIGHT + 5 + insets.bottom);
   }

   public Dimension getMaximumSize() {
      return getPreferredSize();
   }

   public int getRGB(int x, int y) {
      return image.getRGB(x, y);
   }

   public void openBayer(File file) throws IOException {

      FileInputStream inStream = new FileInputStream(file);
      FileChannel inChannel = inStream.getChannel();

      ByteBuffer[] buffers = new ByteBuffer[bayerImage.getWidth()];

      for(int x = 0; x < buffers.length; x++) {
         buffers[x] = ByteBuffer.allocate(bayerImage.getHeight() * 4);
      }

      inChannel.read(buffers);

      inStream.close();
      inChannel.close();

      int[] pixels = new int[bayerImage.getHeight()];

      for(int x = 0; x < bayerImage.getWidth(); x++) {
         buffers[x].flip();
         buffers[x].asIntBuffer().get(pixels);
         bayerImage.setRGB(x, 0, 1, pixels.length, pixels, 0, 1);
      }

      interpolate();

   }

   public void saveBayer(File file) throws IOException {

      ByteBuffer[] buffers = new ByteBuffer[bayerImage.getWidth()];

      for(int x = 0; x < buffers.length; x++) {
         buffers[x] = ByteBuffer.allocate(bayerImage.getHeight() * 4);
         for(int y = 0; y < bayerImage.getHeight(); y++) {
            buffers[x].putInt(bayerImage.getRGB(x, y));
         }
         buffers[x].flip();
      }

      FileOutputStream outStream = new FileOutputStream(file);
      FileChannel outChannel = outStream.getChannel();

      outChannel.write(buffers);

      outStream.close();
      outChannel.close();

   }

   public void setRow(int row, ByteBuffer data, boolean finished) {

      int[] pixels = new int[AVRSystem.IMAGE_WIDTH * 2];

      int x = 0;

      while(data.hasRemaining()) {

         byte pixel = data.get();

         if((x & 1) == 0) {

            // green
            pixels[x] = (pixel & 0xF0) << 8;

            // blue
            pixels[AVRSystem.IMAGE_WIDTH + x] = (pixel & 0x0F) << 4;

         } else {

            // green
            pixels[AVRSystem.IMAGE_WIDTH + x] = ((pixel & 0x0F) << 4) << 8;

            // red
            pixels[x] = (pixel & 0xF0) << 16;

         }

         x++;

      }

      bayerImage.setRGB(0, row * 2, AVRSystem.IMAGE_WIDTH, 2, pixels, 0, AVRSystem.IMAGE_WIDTH);

      if(finished) {
         interpolate();
      }

      repaint();

   }

   /* ********************************************************
    * The bayerImage is in the bayer format shown below.
    *
    *      |   |   |   |   |   |   |   |   |   | . | 1 | 1 |
    *      |   |   |   |   |   |   |   |   |   | . | 7 | 7 |
    *      | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | . | 4 | 5 |
    * -----+---+---+---+---+---+---+---+---+---+ . +---+---+
    *    0 | G | R | G | R | G | R | G | R | G | . | G | R |
    * -----+---+---+---+---+---+---+---+---+---+ . +---+---+
    *    1 | B | G | B | G | B | G | B | G | B | . | B | G |
    * -----+---+---+---+---+---+---+---+---+---+ . +---+---+
    *    2 | G | R | G | R | G | R | G | R | G | . | G | R |
    * -----+---+---+---+---+---+---+---+---+---+ . +---+---+
    *    3 | B | G | B | G | B | G | B | G | B | . | B | G |
    * -----+---+---+---+---+---+---+---+---+---+ . +---+---+
    *    4 | G | R | G | R | G | R | G | R | G | . | G | R |
    * -----+---+---+---+---+---+---+---+---+---+ . +---+---+
    *    5 | B | G | B | G | B | G | B | G | B | . | B | G |
    * -----+---+---+---+---+---+---+---+---+---+ . +---+---+
    *    .   .   .   .   .   .   .   .   .   .   .   .   .
    * -----+---+---+---+---+---+---+---+---+---+ . +---+---+
    *  142 | G | R | G | R | G | R | G | R | G | . | G | R |
    * -----+---+---+---+---+---+---+---+---+---+ . +---+---+
    *  143 | B | G | B | G | B | G | B | G | B | . | B | G |
    * -----+---+---+---+---+---+---+---+---+---+ . +---+---+
    *
    * The corners are calculated, then the edges, then the center.
    *
    */

   private void interpolate() {

      int red = 0;
      int green = 0;
      int blue = 0;

      int currColor = GREEN_ODD;
      int nextColor = RED;

      int width = AVRSystem.IMAGE_WIDTH;
      int height = AVRSystem.IMAGE_HEIGHT;

      // *** do the corners of the image ***
      // upper left corner
      red   = bayerImage.getRGB(1, 0);
      green = bayerImage.getRGB(0, 0);
      blue  = bayerImage.getRGB(0, 1);
      image.setRGB(0, 0, red | green | blue);

      // upper right corner
      red   = bayerImage.getRGB(width - 1, 0);
      green = (bayerImage.getRGB(width - 2, 0) + bayerImage.getRGB(width - 1, 1)) / 2;
      blue  = bayerImage.getRGB(width - 2, 1);
      image.setRGB(width - 1, 0, red | green | blue);

      // lower left corner
      red   = bayerImage.getRGB(1, height - 2);
      green = (bayerImage.getRGB(0, height - 2) + bayerImage.getRGB(1, height - 1)) / 2;
      blue  = bayerImage.getRGB(0, height - 1);
      image.setRGB(0, height - 1, red | green | blue);

      // lower right corner
      red   = bayerImage.getRGB(width - 1, height - 2);
      green = bayerImage.getRGB(width - 1, height - 1);
      blue  = bayerImage.getRGB(width - 2, height - 1);
      image.setRGB(width - 1, height - 1, red | green | blue);

      // *** do the north edge
      currColor = RED;
      for(int x = 1, y = 0; x < width - 1; x++) {
         switch(currColor) {
            case RED:
               red   = bayerImage.getRGB(x, y);
               green = (bayerImage.getRGB(x + 1, y) + bayerImage.getRGB(x, y + 1) + bayerImage.getRGB(x - 1, y)) / 3;
               blue  = (bayerImage.getRGB(x + 1, y + 1) + bayerImage.getRGB(x - 1, y + 1)) / 2;
               nextColor = GREEN_EVEN;
               break;
            case GREEN_EVEN:
               red   = (bayerImage.getRGB(x - 1, y) + bayerImage.getRGB(x + 1, y)) / 2;
               green = bayerImage.getRGB(x, y);
               blue  = bayerImage.getRGB(x, y + 1);
               nextColor = RED;
               break;
            default:
               AVRSystem.LOG.warning("Invalid color for row start: " + currColor);
         }
         currColor = nextColor;
         image.setRGB(x, y, red | green | blue);
      }

      // *** do the west edge
      currColor = BLUE;
      for(int y = 1, x = 0; y < height - 1; y++) {
         switch(currColor) {
            case BLUE:
               red   = (bayerImage.getRGB(x + 1, y + 1) + bayerImage.getRGB(x + 1, y - 1)) / 2;
               green = (bayerImage.getRGB(x, y - 1) + bayerImage.getRGB(x + 1, y) + bayerImage.getRGB(x, y + 1)) / 3;
               blue  = bayerImage.getRGB(x, y);
               nextColor = GREEN_EVEN;
               break;
            case GREEN_EVEN:
               red   = bayerImage.getRGB(x + 1, y);
               green = bayerImage.getRGB(x, y);
               blue  = (bayerImage.getRGB(x, y - 1) + bayerImage.getRGB(x, y + 1)) / 2;
               nextColor = BLUE;
               break;
         }
         currColor = nextColor;
         image.setRGB(x, y, red | green | blue);
      }

      // *** do the east edge
      currColor = GREEN_ODD;
      for(int y = 1, x = width - 1; y < height - 1; y++) {
         switch(currColor) {
            case RED:
               blue  = (bayerImage.getRGB(x - 1, y + 1) + bayerImage.getRGB(x - 1, y - 1)) / 2;
               green = (bayerImage.getRGB(x, y - 1) + bayerImage.getRGB(x - 1, y) + bayerImage.getRGB(x, y + 1)) / 3;
               red   = bayerImage.getRGB(x, y);
               nextColor = GREEN_EVEN;
               break;
            case GREEN_ODD:
               blue  = bayerImage.getRGB(x - 1, y);
               green = bayerImage.getRGB(x, y);
               red   = (bayerImage.getRGB(x, y - 1) + bayerImage.getRGB(x, y + 1)) / 2;
               nextColor = BLUE;
               break;
         }
         currColor = nextColor;
         image.setRGB(x, y, red | green | blue);
      }

      // *** do the south edge
      currColor = GREEN_ODD;
      for(int x = 1, y = height - 1; x < width - 1; x++) {
         switch(currColor) {
            case GREEN_ODD:
               red   = bayerImage.getRGB(x, y - 1);
               green = bayerImage.getRGB(x, y);
               blue  = (bayerImage.getRGB(x - 1, y) + bayerImage.getRGB(x + 1, y)) / 2;
               nextColor = BLUE;
               break;
            case BLUE:
               red   = (bayerImage.getRGB(x - 1, y - 1) + bayerImage.getRGB(x + 1, y - 1)) / 2;
               green = (bayerImage.getRGB(x, y - 1) + bayerImage.getRGB(x + 1, y) + bayerImage.getRGB(x - 1, y)) / 3;
               blue  = bayerImage.getRGB(x, y);
               nextColor = GREEN_ODD;
               break;
            default:
               AVRSystem.LOG.warning("Invalid color for row start: " + currColor);
         }
         currColor = nextColor;
         image.setRGB(x, y, red | green | blue);
      }

      // *** do the center box ***
      currColor = RED;
      for(int y = 1; y < height - 1; y++) {
         // change the starting color for the row
         switch(currColor) {
            case RED:
               currColor = GREEN_ODD;
               break;
            case GREEN_ODD:
               currColor = RED;
               break;
            default:
               AVRSystem.LOG.warning("Invalid color for row start: " + currColor);

         }
         for(int x = 1; x < width - 1; x++) {

            switch(currColor) {
               case RED:
                  red   = bayerImage.getRGB(x, y);

                  green = (bayerImage.getRGB(x, y - 1) + bayerImage.getRGB(x + 1, y) +
                           bayerImage.getRGB(x, y + 1) + bayerImage.getRGB(x - 1, y)) / 4;

                  blue  = (bayerImage.getRGB(x - 1, y - 1) + bayerImage.getRGB(x + 1, y + 1) +
                           bayerImage.getRGB(x + 1, y - 1) + bayerImage.getRGB(x - 1, y + 1)) / 4;
                  nextColor = GREEN_EVEN;
                  break;
               case GREEN_EVEN:
                  red   = (bayerImage.getRGB(x - 1, y) + bayerImage.getRGB(x + 1, y)) / 2;
                  green = bayerImage.getRGB(x, y);
                  blue  = (bayerImage.getRGB(x, y - 1) + bayerImage.getRGB(x, y + 1)) / 2;
                  nextColor = RED;
                  break;
               case GREEN_ODD:
                  red   = (bayerImage.getRGB(x, y - 1) + bayerImage.getRGB(x, y + 1)) / 2;
                  green = bayerImage.getRGB(x, y);
                  blue  = (bayerImage.getRGB(x - 1, y) + bayerImage.getRGB(x + 1, y)) / 2;
                  nextColor = BLUE;
                  break;
               case BLUE:
                  red   = (bayerImage.getRGB(x - 1, y - 1) + bayerImage.getRGB(x + 1, y + 1) +
                           bayerImage.getRGB(x + 1, y - 1) + bayerImage.getRGB(x - 1, y + 1)) / 4;

                  green = (bayerImage.getRGB(x, y - 1) + bayerImage.getRGB(x + 1, y) +
                           bayerImage.getRGB(x, y + 1) + bayerImage.getRGB(x - 1, y)) / 4;

                  blue  = bayerImage.getRGB(x, y);
                  nextColor = GREEN_ODD;
                  break;
               default:
                  AVRSystem.LOG.warning("Invalid color: " + currColor);
            }

            currColor = nextColor;

            image.setRGB(x, y, red | green | blue);
         }
      }

   }

}
