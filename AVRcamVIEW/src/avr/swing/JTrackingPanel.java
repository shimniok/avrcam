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
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.text.*;
import java.util.*;
import javax.swing.*;

import avr.device.event.*;
import avr.lang.*;

public class JTrackingPanel extends JPanel {

   private static final DateFormat DATE_FORMAT;

   static {
      DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh.mm.ss");
   }

   private ColorBlob[] blobs;
   private DataListener dataHandler;

   private Dimension preferredSize;

   private boolean recording;
   private boolean sendCameraData;

   private FileOutputStream outStream;
   private FileChannel outChannel;

   public JTrackingPanel() {
      super(null);

      recording = false;
      sendCameraData = false;

      setBackground(Color.BLACK);
      dataHandler = new TrackingHandler();

      preferredSize = null;
   }

   public void startRecording() throws FileNotFoundException {
      File recordFile = new File(DATE_FORMAT.format(new Date()) + ".trk");
      outStream = new FileOutputStream(recordFile);
      outChannel = outStream.getChannel();

      recording = true;

      AVRSystem.LOG.info("Started Recording");
   }

   public void stopRecording() throws IOException {

      recording = false;

      outStream.close();
      outChannel.close();

      AVRSystem.LOG.info("Stopped Recording");

   }

   public void startTracking() {
      AVRSystem.DEVICE.addDataListener(dataHandler);
   }

   public void stopTracking() {
      AVRSystem.DEVICE.removeDataListener(dataHandler);
   }

   public void startSendingCameraData() {
      sendCameraData = true;
   }

   public void stopSendingCameraData() {
      sendCameraData = false;
   }

   public Dimension getMinimumSize() {
      return getPreferredSize();
   }

   public Dimension getPreferredSize() {
      if(preferredSize == null) {
         Insets insets = this.getInsets();
         preferredSize = new Dimension(insets.left + AVRSystem.IMAGE_WIDTH + insets.right,
                                       insets.top + AVRSystem.IMAGE_HEIGHT + insets.bottom);
      }

      return preferredSize;
   }

   public Dimension getMaximumSize() {
      return getPreferredSize();
   }

   public void paintComponent(Graphics g) {

      super.paintComponent(g);

      Dimension size = getSize();

      Insets insets = getInsets();

      double xScale = size.width /
                      (double)(insets.left + 5 + AVRSystem.IMAGE_WIDTH + 5 +
                               insets.right);
      double yScale = size.height /
                      (double)(insets.top + 5 + AVRSystem.IMAGE_HEIGHT + 5 +
                               insets.bottom);
      double scale = Math.min(xScale, yScale);

      int imageWidth = (int)(AVRSystem.IMAGE_WIDTH * scale);
      int imageHeight = (int)(AVRSystem.IMAGE_HEIGHT * scale);

      // it is possible for the width or height to be 0 when
      // the window is resized.  If this occurs, don't try
      // to paint anything. just return
      if(imageWidth <= 0 || imageHeight <= 0) {
         return;
      }

      Image bufferedImage = createImage(imageWidth, imageHeight);

      Graphics2D bufferGraphics = (Graphics2D)bufferedImage.getGraphics();

      bufferGraphics.setColor(Color.WHITE);
      bufferGraphics.fillRect(0, 0, imageWidth, imageHeight);

      bufferGraphics.scale(scale, scale);

      for(int i = 0; (blobs != null) && (i < blobs.length); i++) {
         ColorBlob blob = blobs[i];
         if(blob != null) {
            bufferGraphics.setColor(AVRSystem.DEVICE.getMapColors()[blob.
                                    colorIndex]);
            bufferGraphics.fillRect(blob.center.x - 2, blob.center.y - 2, 4, 4);
            bufferGraphics.setColor(Color.BLACK);
            bufferGraphics.drawRect(blob.bounds.x, blob.bounds.y,
                                    blob.bounds.width, blob.bounds.height);
         }
      }

      g.drawImage(bufferedImage,
                  (size.width - imageWidth) / 2, (size.height - imageHeight) / 2,
                  imageWidth, imageHeight, this);


   }

   public void setTrackingData(ByteBuffer data) {
      blobs = new ColorBlob[data.get() & 0xFF];

      for(int i = 0; i < blobs.length; i++) {
         blobs[i] = new ColorBlob(data);
      }

      repaint();

      if(sendCameraData) {
         data.position(0);
         try {
            AVRSystem.DEVICE.sendCameraData(data);
         } catch(IOException ioe) {
            ioe.printStackTrace(System.err);
            AVRSystem.LOG.warning(ioe.getMessage());
         }
      }

   }

   private final class TrackingHandler extends DataAdapter {

      public void trackingData(ByteBuffer data) {
         setTrackingData(data);

         if(recording) {
            data.reset();
            try {
               outChannel.write(data);
            } catch(IOException ioe) {
               AVRSystem.LOG.warning("TRACKING: " + ioe.getMessage());
            }
         }

      }

   }

   private final static class ColorBlob {

      public final int colorIndex;
      public final Point center;
      public final Rectangle bounds;

      public ColorBlob(ByteBuffer data) {
         colorIndex = data.get();

         int x = data.get() & 0xFF;
         int y = data.get() & 0xFF;
         int width = (data.get() & 0xFF) - x;
         int height = (data.get() & 0xFF) - y;
         bounds = new Rectangle(x, y, width, height);

         center = new Point(x + (width / 2), y + (height / 2));
      }

      public String toString() {
         return "ColorBlob: " + colorIndex + " (" + center.x + ", " + center.y + ") " +
                " [" + bounds.x + ", " + bounds.y + ", " + bounds.width + ", " + bounds.height + "]";
      }


   }

}
