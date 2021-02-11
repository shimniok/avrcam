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

package avr.device;

import avr.connection.AbstractConnection;
import avr.connection.SerialConnection;
import avr.connection.SerialParams;
import avr.connection.event.ConnectionEvent;
import avr.connection.event.ConnectionListener;
import avr.device.event.DataListener;
import avr.io.AVRInputStream;
import avr.lang.AVRSystem;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import java.awt.Color;
import java.awt.EventQueue;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

public class Device implements ConnectionListener {

   public static void main(String[] args) throws Exception {
      Device device = new Device();

      SerialParams params = new SerialParams();

      device.setConnection(new SerialConnection("COM1", params));

      device.connect();
      device.sendPing();
      device.sendDumpFrame();
      device.sendGetVersion();
      device.sendSetRegisters(new HashMap());
      device.sendReset();
      device.sendEnableTracking();
      device.sendDisableTracking();
      device.sendSetColorMap(null, null, null);

      try {
         System.in.read();
      } catch(IOException ioe) {
         ioe.printStackTrace();
      }
      device.disconnect();

   }

   private static final String ACK = "ACK";
   private static final String NCK = "NCK";
   private static final String VERSION = "AVR";

   private static final String COLOR_MAP_KEY = "avr.color.map";

   // timeout after 3 seconds
   private static final int RESPONSE_TIMEOUT = 3000;

   private java.util.List connectionListeners;
   private java.util.List dataListeners;
   private AbstractConnection con;

   private InputStream in;
   private OutputStream out;

   private int[][] colorMap;
   private Color[] mapColors;

   private Timer responseTimer;
   private TimerTask responseTask;

   public Device() {
      connectionListeners = new ArrayList(3);
      dataListeners = new ArrayList(3);
      loadMap();

      responseTimer = new Timer();
   }

   public Color[] getMapColors() {
      return mapColors;
   }

   public int[][] getColorMap() {
      return colorMap;
   }

   public void setColorMap(int[][] colorMap) {
      this.colorMap = colorMap;
      setMapColors();
      saveMap();
   }

   public void saveMap() {
      try {
         ByteArrayOutputStream bOut = new ByteArrayOutputStream();
         DataOutputStream dOut = new DataOutputStream(bOut);

         dOut.writeInt(colorMap.length);
         for(int i = 0; i < colorMap.length; i++) {
            dOut.writeInt(colorMap[i].length);
            for(int j = 0; j < colorMap[i].length; j++) {
               dOut.writeInt(colorMap[i][j]);
            }
         }

         dOut.close();

         AVRSystem.PREFS.putByteArray(COLOR_MAP_KEY, bOut.toByteArray());

      } catch(Exception e) {
         e.printStackTrace();
      }
   }

   public void loadMap() {
      try {
         byte[] data = AVRSystem.PREFS.getByteArray(COLOR_MAP_KEY, null);
         if(data != null) {
            ByteArrayInputStream bIn = new ByteArrayInputStream(data);
            DataInputStream dIn = new DataInputStream(bIn);

            int width = dIn.readInt();
            colorMap = new int[width][];
            for(int i = 0; i < colorMap.length; i++) {
               colorMap[i] = new int[dIn.readInt()];
               for(int j = 0; j < colorMap[i].length; j++) {
                  colorMap[i][j] = dIn.readInt();
               }
            }

            dIn.close();
         } else {
            colorMap = new int[3][AVRSystem.NUM_INTENSITIES];
         }

         setMapColors();

      } catch(Exception e) {
         e.printStackTrace();
      }

   }

   private void setMapColors() {
      mapColors = new Color[8];

      for(int col = 0; col < mapColors.length; col++) {
         int value = 0;

         int red = 0;
         int green = 0;
         int blue = 0;

         int numRed = 0;
         int numGreen = 0;
         int numBlue = 0;


         for(int i = 0; i < AVRSystem.NUM_INTENSITIES; i++) {

            if((colorMap[0][i] & (0x01 << (7 - col))) != 0) {
               red += i << 4;
               numRed++;
            }

            if((colorMap[1][i] & (0x01 << (7 - col))) != 0) {
               green += i << 4;
               numGreen++;
            }

            if((colorMap[2][i] & (0x01 << (7 - col))) != 0) {
               blue += i << 4;
               numBlue++;
            }

         }

         if(numRed > 0) {
            red /= numRed;
         }

         if(numGreen > 0) {
            green /= numGreen;
         }

         if(numBlue > 0) {
            blue /= numBlue;
         }

         value = (red << 16) | (green << 8) | blue;

         mapColors[col] = new Color(value);
      }

   }

   public AbstractConnection getConnection() {
      return con;
   }

   public void setConnection(AbstractConnection con) {
      this.con = con;
   }

   public boolean isConnected() {
      return con != null && con.isConnected();
   }

   public void addConnectionListener(ConnectionListener listener) {
      AVRSystem.LOG.finest("Added Connection Listener: " + listener);
      connectionListeners.add(listener);
   }

   public void removeConnectionListener(ConnectionListener listener) {
      AVRSystem.LOG.finest("Removed Connection Listener: " + listener);
      connectionListeners.remove(listener);
   }

   public void connected(ConnectionEvent event) {
      ConnectionListener[] listeners = (ConnectionListener[])connectionListeners.toArray(new ConnectionListener[connectionListeners.size()]);
      for(int i = 0; i < listeners.length; i++) {
         ((ConnectionListener)listeners[i]).connected(event);
      }
   }

   public void disconnected(ConnectionEvent event) {
      ConnectionListener[] listeners = (ConnectionListener[])connectionListeners.toArray(new ConnectionListener[connectionListeners.size()]);
      for(int i = 0; i < listeners.length; i++) {
         ((ConnectionListener)listeners[i]).disconnected(event);
      }
   }

   public void connect() throws Exception {

      if(!isConnected()) {
         con.connect();

         if(con instanceof SerialConnection) {
            SerialPort serialPort = (SerialPort)con.getConnectionObject();
            serialPort.notifyOnDataAvailable(true);
            try {
               serialPort.addEventListener(new SerialEventHandler(this));
            } catch(TooManyListenersException tmle) {
               AVRSystem.LOG.severe(tmle.getMessage());
            }
         }

         in = new AVRInputStream(con.getInputStream());
         out = con.getOutputStream();

         AVRSystem.LOG.config("Device connected to " + con.toString());

         connected(new ConnectionEvent(con));

      }
   }

   public void disconnect() {

      if(isConnected()) {
         try {
            con.disconnect();
            AVRSystem.LOG.config("Device Disconnected");
            disconnected(new ConnectionEvent(con));
            con = null;
         } catch(IOException ioe) {
            AVRSystem.LOG.severe(ioe.getMessage());
         }
      }
   }

   public InputStream getInputStream() {
      return in;
   }

   public void addDataListener(DataListener dl) {
      if(dataListeners.add(dl)) {
         AVRSystem.LOG.finest("Added Data Listener: " + dl);
      }
   }

   public void removeDataListener(DataListener dl) {
      if(dataListeners.remove(dl)) {
         AVRSystem.LOG.finest("Removed Data Listener: " + dl);
      }
   }

   protected void handleString(String data) {
      if(data.equals(ACK)) {
         fireACKReceived();
      } else if(data.equals(NCK)) {
         fireNCKReceived();
      } else if(data.startsWith(VERSION)) {
         fireVERSIONReceived(data);
      } else {
         StringBuffer builder = new StringBuffer("UNKNOWN PACKET: (");
         ByteBuffer bytes = ByteBuffer.wrap(data.getBytes());
         builder.append(bytes.remaining()).append(") ");
         while(bytes.hasRemaining()) {
            builder.append(Integer.toHexString(bytes.get() & 0xFF)).append(' ');
         }
         AVRSystem.LOG.warning(builder.toString());
      }
   }

   protected void handleData(ByteBuffer data) {
      if(data.hasRemaining()) {
         byte dataType = data.get();
         if(dataType == 0x0B) {
            fireFrameDataReceived(data);
         } else if(dataType == 0x0A) {
            fireTrackingDataReceived(data);
         } else {
            handleString(new String(data.array(), 0, data.limit()));
         }
      }
   }

   protected void fireACKReceived() {
      if(responseTask != null) {
         responseTask.cancel();
      }
      AVRSystem.LOG.info("Received: ACK");
      DataListener[] listeners = (DataListener[])dataListeners.toArray(new DataListener[dataListeners.size()]);
      for(int i = 0; i < listeners.length; i++) {
         ((DataListener)listeners[i]).ack();
      }
   }

   private void fireResponseTimerExpired() {
      AVRSystem.LOG.severe("Response Timer Expired");
      DataListener[] listeners = (DataListener[])dataListeners.toArray(new DataListener[dataListeners.size()]);
      for(int i = 0; i < listeners.length; i++) {
         ((DataListener)listeners[i]).responseTimerExpired();
      }
   }

   protected void fireNCKReceived() {
      if(responseTask != null) {
         responseTask.cancel();
      }
      AVRSystem.LOG.info("Received: NCK");
      DataListener[] listeners = (DataListener[])dataListeners.toArray(new DataListener[dataListeners.size()]);
      for(int i = 0; i < listeners.length; i++) {
         ((DataListener)listeners[i]).nck();
      }
   }

   protected void fireVERSIONReceived(String version) {
      if(responseTask != null) {
         responseTask.cancel();
      }
      AVRSystem.LOG.info("Received: " + version);
      DataListener[] listeners = (DataListener[])dataListeners.toArray(new DataListener[dataListeners.size()]);
      for(int i = 0; i < listeners.length; i++) {
         ((DataListener)listeners[i]).version(version);
      }
   }

   protected void fireFrameDataReceived(ByteBuffer data) {

      int position = data.position();
      StringBuffer buf = new StringBuffer("Received: Frame Dump (" + (data.get(position) & 0xFF) + ")");

      data.position(0);

      while(data.hasRemaining()) {
         int b = data.get() & 0xFF;
         buf.append(' ').append(((b & 0xF0) == 0) ? "0" : "").append(Integer.toHexString(b).toUpperCase());
      }

      data.position(position);

      AVRSystem.LOG.info(buf.toString());
//      AVRSystem.LOG.info("Received: Frame Dump (" + (data.get(data.position()) & 0xFF) + ")");
      DataListener[] listeners = (DataListener[])dataListeners.toArray(new DataListener[dataListeners.size()]);
      for(int i = 0; i < listeners.length; i++) {
         data.mark();
         ((DataListener)listeners[i]).frameData(data);
         data.reset();
      }
   }

   protected void fireTrackingDataReceived(ByteBuffer data) {

      int position = data.position();
      StringBuffer buf = new StringBuffer("Received: Tracking Info (" + (data.get(position) & 0xFF) + ")");

      data.position(0);

      while(data.hasRemaining()) {
         int b = data.get() & 0xFF;
         buf.append(' ').append(((b & 0xF0) == 0) ? "0" : "").append(Integer.toHexString(b).toUpperCase());
      }

      data.position(position);

      AVRSystem.LOG.info(buf.toString());
      DataListener[] listeners = (DataListener[])dataListeners.toArray(new DataListener[dataListeners.size()]);
      for(int i = 0; i < listeners.length; i++) {
         data.mark();
         ((DataListener)listeners[i]).trackingData(data);
         data.reset();
      }
   }

   private void sendRequest(byte[] data) throws IOException {
      sendRequest(data, 0, data.length);
   }

   private void sendRequest(byte[] data, int off, int len) throws IOException {

      responseTask = new ResponseTask();
      responseTimer.schedule(responseTask, RESPONSE_TIMEOUT);

      if(isConnected()) {

         out.write(data, off, len);
         out.write((byte)'\n');
         out.flush();

         StringBuffer builder = new StringBuffer("Sending: ");
         for(int i = off; i < len; i++) {
            builder.append((char)data[i]);
         }
         AVRSystem.LOG.info(builder.toString());
      } else {
         AVRSystem.LOG.warning("AVRcam not connected.");
      }
   }

   public void sendSetRegisters(Map registers) throws IOException {
      ByteBuffer buffer = ByteBuffer.allocate(60);
      buffer.put((byte)'C')
            .put((byte)'R');

     Set entries = registers.entrySet();
     for(Iterator i = entries.iterator(); i.hasNext();) {
        Map.Entry entry = (Map.Entry)i.next();
        buffer.put((byte)' ').put(entry.getKey().toString().getBytes())
              .put((byte)' ').put(entry.getValue().toString().getBytes());
     }

      buffer.put((byte)'\r');
      buffer.flip();
      sendRequest(buffer.array(), 0, buffer.remaining());
   }

   public void sendDisableTracking() throws IOException {
      sendRequest(new byte[] { (byte)'D', (byte)'T', (byte)'\r'} );
   }

   public void sendDumpFrame() throws IOException {
      sendRequest(new byte[] { (byte)'D', (byte)'F', (byte)'\r'} );
   }

   public void sendEnableTracking() throws IOException {
      sendRequest(new byte[] { (byte)'E', (byte)'T', (byte)'\r'} );
   }

   public void sendGetVersion() throws IOException {
      sendRequest(new byte[] { (byte)'G', (byte)'V', (byte)'\r'} );
   }

   public void sendPing() throws IOException {
      sendRequest(new byte[] { (byte)'P', (byte)'G', (byte)'\r'} );
   }

   public void sendReset() throws IOException {
      sendRequest(new byte[] { (byte)'R', (byte)'S', (byte)'\r'} );
   }

   public void sendSetColorMap(int[] red, int[] green, int[] blue) throws IOException {
      ByteBuffer buffer = ByteBuffer.allocate(2 + 4 * red.length * 3 + 1);
      buffer.put("SM".getBytes());
      for(int i = 0; i < red.length; i++) {
         buffer.put((" " + red[i] + "").getBytes());
      }
      for(int i = 0; i < green.length; i++) {
         buffer.put((" " + green[i] + "").getBytes());
      }
      for(int i = 0; i < blue.length; i++) {
         buffer.put((" " + blue[i] + "").getBytes());
      }
      buffer.put("\r".getBytes());
      buffer.flip();
      sendRequest(buffer.array(), 0, buffer.remaining());
   }

   public void sendCameraData(ByteBuffer data) throws IOException {

      out.write((byte)0x0A);
      out.write(data.array(), 0, data.remaining());
      out.write((byte)0xFF);

      out.flush();

   }

   private final static class SerialEventHandler implements SerialPortEventListener {

      private Device device;

      public SerialEventHandler(Device device) {
         this.device = device;
      }

      public void serialEvent(SerialPortEvent spe) {
         if(spe.getEventType() == SerialPortEvent.DATA_AVAILABLE) {

            try {

               do {

                  byte[] data = new byte[1024];

                  int bytesRead = device.getInputStream().read(data);
                  if(bytesRead > 0) {
                     // take only bytesRead - 1 to remove the
                     // terminating character '\r' or 0xFF
                     EventQueue.invokeLater(new GUITask(device, ByteBuffer.wrap(data, 0, bytesRead - 1)));
                  }

               } while(device.getInputStream().available() > 0);

            } catch(IOException ioe) {
               AVRSystem.LOG.severe(ioe.getMessage());
               device.disconnect();
            }

         }
      }

   }

   private final static class GUITask implements Runnable {
      private ByteBuffer data;
      private Device device;

      public GUITask(Device device, ByteBuffer data) {
         this.device = device;
         this.data = data;
      }

      public void run() {
         device.handleData(data);
      }

   }

   private final class ResponseTask extends TimerTask {

      public void run() {
         // make sure we fire the timer expired event
         // using the Event Dispatch Thread.  If not,
         // queue this TimerTask in the Event Queue.
         if(EventQueue.isDispatchThread()) {
            fireResponseTimerExpired();
         } else {
            EventQueue.invokeLater(this);
         }
      }

   }

   /**
    * Only used for debugging purposes, this will simulate an ACK coming from the device.
    */
   public void simulateACK() {
      fireACKReceived();
   }

   /**
    * Only used for debugging purposes, this will simulate an NCK coming from the device.
    */
   public void simulateNCK() {
      fireNCKReceived();
   }

}
