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
import java.awt.event.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import avr.lang.*;
import avr.connection.event.ConnectionListener;
import avr.connection.event.ConnectionEvent;

public class JTrackingInternalFrame extends JInternalFrame {

   private static final int BLOB_LENGTH = 5;

   private JTrackingPanel trackingP;

   private AbstractButton recordB;
   private AbstractButton sendCameraDataB;

   private Action recordAction;
   private Action playAction;
   private Action pauseAction;
   private Action stopAction;

   private FileInputStream inStream;
   private FileChannel inChannel;

   private Map indexMap;
   private int numTracked;

   private boolean paused;

   private javax.swing.Timer timer;

   private JSlider playbackS;

   private ConnectionListener connectionHandler;

   // common initializer for both constructors
   {
      trackingP = new JTrackingPanel();

      createActions();

      setJMenuBar(createMenuBar());
      getContentPane().add(trackingP, BorderLayout.CENTER);
   }

   public JTrackingInternalFrame() {
      super("Tracking", true, false, true, false);
      getContentPane().add(createToolBar(), BorderLayout.NORTH);
   }

   public JTrackingInternalFrame(File trackingData) throws FileNotFoundException, IOException {
      super("Tracking: " + trackingData.toString(), true, true, true, true);

      inStream = new FileInputStream(trackingData);
      inChannel = inStream.getChannel();

      indexMap = new HashMap();
      numTracked = -1;

      paused = false;

      // build an index map to map an index to a position in a file
      indexMap.put(new Integer(0), new Integer(0));

      ByteBuffer numTrackedBuffer = ByteBuffer.allocate(1);
      int currentIndex = 0;

      while(inChannel.read(numTrackedBuffer) != -1) {

         numTrackedBuffer.flip();
         numTracked = numTrackedBuffer.get() & 0xFF;

         currentIndex++;
         int position = (int)(inChannel.position() + (numTracked * BLOB_LENGTH));

         indexMap.put(new Integer(currentIndex), new Integer(position));

         inChannel.position(position);
         numTrackedBuffer.clear();

      }

      sendCameraDataB = new JCheckBox(new ProxyAction(this, "sendCameraData", "Send To Serial Port"));
      sendCameraDataB.setEnabled(AVRSystem.DEVICE.isConnected());

      playbackS = new JSlider(JSlider.HORIZONTAL, 0, currentIndex - 1, 0);
      playbackS.setMinorTickSpacing(1);
      playbackS.setMajorTickSpacing(20);
      playbackS.setPaintTicks(true);
      playbackS.setPaintTrack(true);
      playbackS.setPaintLabels(true);
      playbackS.setSnapToTicks(true);
      playbackS.addChangeListener(new PlaybackHandler());

      JPanel southP = new JPanel(new BorderLayout());
      southP.setBorder(new EmptyBorder(5, 0, 0, 0));

//      JPanel controlP = new JPanel(new BorderLayout());
      JPanel controlP = new JPanel();

      controlP.add(new JButton(playAction));
      controlP.add(new JButton(pauseAction));
      controlP.add(new JButton(stopAction));

//
//      JPanel controlNorthP = new JPanel();
//      controlNorthP.add(new JButton(playAction));
//      controlNorthP.add(new JButton(pauseAction));
//      controlNorthP.add(new JButton(stopAction));
//
//      JPanel controlSouthP = new JPanel();
//      controlSouthP.add(sendCameraDataB);
//
//      controlP.add(controlNorthP, BorderLayout.NORTH);
//      controlP.add(controlSouthP, BorderLayout.SOUTH);

      southP.add(playbackS, BorderLayout.NORTH);
      southP.add(controlP, BorderLayout.SOUTH);

      getContentPane().add(southP, BorderLayout.SOUTH);

      trackingP.setTrackingData(read(0));

      connectionHandler = new ConnectionHandler();
      AVRSystem.DEVICE.addConnectionListener(connectionHandler);

   }

   private void createActions() {
      recordAction = new ProxyAction(this, "record", "Start Recording", 's');
      playAction = new ProxyAction(this, "play", "Play", 'p');
      stopAction = new ProxyAction(this, "stop", "Stop", 's');
      pauseAction = new ProxyAction(this, "pause", "Pause");

      stopAction.setEnabled(false);
      pauseAction.setEnabled(false);

   }

   private JMenuBar createMenuBar() {

      JMenuBar menubar = new JMenuBar();

      JMenu fileM = new JMenu("File");

      fileM.add(new ProxyAction(this, "pack", "Reset Size", 'R'));
      fileM.addSeparator();
      fileM.add(new ProxyAction(this, "dispose", "Exit", 'X'));

      menubar.add(fileM);

      return menubar;

   }

   private JToolBar createToolBar() {

      JToolBar toolbar = new JToolBar();
      toolbar.setFloatable(false);

      recordB = new JToggleButton(recordAction);

      toolbar.add(recordB);

      return toolbar;

   }

   public void dispose() {
      if(inStream != null) {
         if(timer != null) {
            stop();
         }
         try {
            inStream.close();
            inChannel.close();
         } catch(Exception e) {
            e.printStackTrace();
         }
      }
      AVRSystem.DEVICE.removeConnectionListener(connectionHandler);
      super.dispose();
   }

   public void play() {
      if(timer == null) {
         // set the timer to fire for 30 frames per second
         timer = new javax.swing.Timer((int)(1000 / 30), new UpdateSliderHandler());
      }
      timer.start();
      playAction.setEnabled(false);
      stopAction.setEnabled(true);
      pauseAction.setEnabled(true);
   }

   public void pause() {
      if(!paused) {
         timer.stop();
         playAction.setEnabled(false);
         stopAction.setEnabled(false);
         pauseAction.putValue(Action.NAME, "Resume");
         paused = true;
      } else {
         timer.start();
         playAction.setEnabled(false);
         stopAction.setEnabled(true);
         pauseAction.putValue(Action.NAME, "Pause");
         paused = false;
      }
   }

   public void stop() {
      timer.stop();
      playAction.setEnabled(true);
      stopAction.setEnabled(false);
      pauseAction.setEnabled(false);
      playbackS.setValue(0);
   }

   public void sendCameraData() {
      if(sendCameraDataB.isSelected()) {
         trackingP.startSendingCameraData();
      } else {
         trackingP.stopSendingCameraData();
      }
   }

   private ByteBuffer read(int position) throws IOException {

      inChannel.position(position);

      ByteBuffer numTrackedBuffer = ByteBuffer.allocate(1);
      inChannel.read(numTrackedBuffer);
      numTrackedBuffer.flip();

      int numTracked = numTrackedBuffer.get() & 0xFF;

      ByteBuffer blobBuffer = ByteBuffer.allocate(1 + (numTracked * BLOB_LENGTH));

      inChannel.position(inChannel.position() - 1);
      inChannel.read(blobBuffer);
      blobBuffer.flip();

      return blobBuffer;

   }

   public void record() {
      if(recordB.isSelected()) {
         try {
            trackingP.startRecording();
            recordB.setText("Stop Recording");
         } catch(Exception e) {
            recordB.setSelected(false);
         }
      } else {
         stopRecording();
      }
   }

   private void stopRecording() {
      try {
         trackingP.stopRecording();
         recordB.setText("Start Recording");
      } catch(Exception e) {
         recordB.setSelected(true);
      }
   }

   public void startTracking() {
      trackingP.startTracking();
   }

   public void stopTracking() {
      if(recordB.isSelected()) {
         stopRecording();
      }
      trackingP.stopTracking();
   }

   private final class PlaybackHandler implements ChangeListener {
      public void stateChanged(ChangeEvent ce) {
         try {
            trackingP.setTrackingData(read(((Integer)indexMap.get(new Integer(playbackS.getValue()))).intValue()));
         } catch(IOException ioe) {
            ioe.printStackTrace();
            AVRSystem.LOG.severe(ioe.getMessage());
         }
      }
   }

   private final class UpdateSliderHandler implements ActionListener {
      public void actionPerformed(ActionEvent ae) {
         if(playbackS.getValue() == playbackS.getMaximum()) {
            stop();
         } else {
            try {
               playbackS.setValue(playbackS.getValue() + 1);
            } catch(Exception e) {
               e.printStackTrace();
            }
         }
      }
   }

   private final class ConnectionHandler implements ConnectionListener {
      public void connected(ConnectionEvent ce) {
         sendCameraDataB.setEnabled(true);
      }

      public void disconnected(ConnectionEvent ce) {
         trackingP.stopSendingCameraData();
         sendCameraDataB.setSelected(false);
         sendCameraDataB.setEnabled(false);
      }

   }

}
