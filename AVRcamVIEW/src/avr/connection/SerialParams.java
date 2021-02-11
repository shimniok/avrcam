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

import gnu.io.SerialPort;
import java.io.Serializable;

public class SerialParams implements Serializable {

   public static void main(String[] args) {
      System.out.println("Flow Control None: " + SerialPort.FLOWCONTROL_NONE);
      System.out.println("Flow Control RTSCTS IN: " + SerialPort.FLOWCONTROL_RTSCTS_IN);
      System.out.println("Flow Control RTSCTS OUT: " + SerialPort.FLOWCONTROL_RTSCTS_OUT);
      System.out.println("Flow Control XON/XOFF IN: " + SerialPort.FLOWCONTROL_XONXOFF_IN);
      System.out.println("Flow Control XON/XOFF OUT: " + SerialPort.FLOWCONTROL_XONXOFF_OUT);

      System.out.println("Data Bits 5: " + SerialPort.DATABITS_5);
      System.out.println("Data Bits 6: " + SerialPort.DATABITS_6);
      System.out.println("Data Bits 7: " + SerialPort.DATABITS_7);
      System.out.println("Data Bits 8: " + SerialPort.DATABITS_8);

      System.out.println("Parity Even: " + SerialPort.PARITY_EVEN);
      System.out.println("Parity Odd: " + SerialPort.PARITY_ODD);
      System.out.println("Parity Mark: " + SerialPort.PARITY_MARK);
      System.out.println("Parity Space: " + SerialPort.PARITY_SPACE);
      System.out.println("Parity None: " + SerialPort.PARITY_NONE);

      System.out.println("Stop Bits 1: " + SerialPort.STOPBITS_1);
      System.out.println("Stop Bits 1.5: " + SerialPort.STOPBITS_1_5);
      System.out.println("Stop Bits 2: " + SerialPort.STOPBITS_2);
   }

   private int baudRate;
   private int dataBits;
   private int stopBits;
   private int parity;
   private int flowControl;

   public SerialParams() {
      this(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE, SerialPort.FLOWCONTROL_NONE);
   }

   public SerialParams(int baudRate, int dataBits,
                       int stopBits, int parity, int flowControl) {
      this.baudRate = baudRate;
      this.dataBits = dataBits;
      this.stopBits = stopBits;
      this.parity = parity;
      this.flowControl = flowControl;
   }

   public int getBaudRate() {
      return baudRate;
   }

   public int getFlowControl() {
      return flowControl;
   }

   public int getDataBits() {
      return dataBits;
   }

   public int getParity() {
      return parity;
   }

   public int getStopBits() {
      return stopBits;
   }

   public void setStopBits(int stopBits) {
      this.stopBits = stopBits;
   }

   public void setParity(int parity) {
      this.parity = parity;
   }

   public void setFlowControl(int flowControl) {
      this.flowControl = flowControl;
   }

   public void setDataBits(int dataBits) {
      this.dataBits = dataBits;
   }

   public void setBaudRate(int baudRate) {
      this.baudRate = baudRate;
   }

   public String toString() {
      StringBuffer builder = new StringBuffer("Serial Parameters[");
      builder.append("baudrate=").append(baudRate)
             .append("databits=").append(dataBits)
             .append("stopbits=").append(stopBits)
             .append("parity=").append(parity)
             .append("flowcontrol=").append(flowControl);
      return builder.toString();
   }

}
