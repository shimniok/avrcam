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

package avr.connection;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

/***********************************************************************
 * This class represents a Connection to a Serial Port.
 */
public class SerialConnection extends AbstractConnection {

   private SerialPort serialPort;
   private SerialParams params;
   private String comPort;

   public static String[] getSerialPorts() {

      ArrayList ports = new ArrayList();

      Enumeration allPorts = CommPortIdentifier.getPortIdentifiers();
      CommPortIdentifier identifier;

      while(allPorts.hasMoreElements()) {
         identifier = (CommPortIdentifier)allPorts.nextElement();
         if(identifier.getPortType() == CommPortIdentifier.PORT_SERIAL) {
            ports.add(identifier.getName());
         }
      }

      Collections.reverse(ports);

      return (String[])ports.toArray(new String[ports.size()]);

   }

   public SerialConnection(String comPort) {
      this(comPort, new SerialParams());
   }

   public SerialConnection(String comPort, SerialParams params) {
      this.comPort = comPort;
      this.params = params;
   }

   public Object getConnectionObject() {
      return serialPort;
   }

   public synchronized void connect() throws Exception {
      CommPortIdentifier identifier = CommPortIdentifier.getPortIdentifier(comPort);
      serialPort = (SerialPort)identifier.open("AVRcamView", 2000);
      serialPort.setFlowControlMode(params.getFlowControl());
      serialPort.setSerialPortParams(params.getBaudRate(),
                                     params.getDataBits(),
                                     params.getStopBits(),
                                     params.getParity());
      setConnected();
   }

   public synchronized void disconnect() throws IOException {
      serialPort.close();
      setDisconnected();
   }

   public InputStream getInputStream() throws IOException {
      return serialPort.getInputStream();
   }

   public OutputStream getOutputStream() throws IOException {
      return serialPort.getOutputStream();
   }

   public ByteChannel getChannel() {
      return null;
   }

   public String toString() {
      return serialPort.toString();
   }

   public void setSerialParams(SerialParams params) throws UnsupportedCommOperationException {
      serialPort.setFlowControlMode(params.getFlowControl());
      serialPort.setSerialPortParams(params.getBaudRate(),
                                     params.getDataBits(),
                                     params.getStopBits(),
                                     params.getParity());
      this.params = params;
   }

   public String getComPort() {
      return comPort;
   }

   public SerialParams getSerialParams() {
      return params;
   }

}
