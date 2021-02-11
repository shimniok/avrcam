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

import avr.connection.SerialParams;
import gnu.io.SerialPort;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

public class JSerialPanel extends JPanel {

   public static void main(String[] args) throws Exception {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      int option = new JSerialPanel().showDialog(null, null);
      System.out.println(option);
   }

   private static final Integer[] BAUD_RATES = {
                                              new Integer(115200),
                                              new Integer(57600),
                                              new Integer(38400),
                                              new Integer(19200),
                                              new Integer(9600),
                                              new Integer(4800)
   };

   private static final Integer[] DATA_BITS = {
                                              new Integer(8),
                                              new Integer(7),
                                              new Integer(6),
                                              new Integer(5)
   };

   private static final String[] PARITY = {
                                          "None",
                                          "Odd",
                                          "Even",
                                          "Mark",
                                          "Space"
   };

   private static final Number[] STOP_BITS = {
                                             new Integer(1),
                                             new Double(1.5),
                                             new Integer(2)
   };

   public static final String[] FLOW_CONTROL = {
                                               "None",
                                               "Hardware",
                                               "Xon / Xoff"
   };


   private static final int UNKNOWN_OPTION = 0x00;
   public static final int OK_OPTION = 0x01;
   public static final int CANCEL_OPTION = 0x02;

   private JComboBox baudRateCB;
   private JComboBox dataBitsCB;
   private JComboBox stopBitsCB;
   private JComboBox parityCB;
   private JComboBox flowControlCB;

   private JDialog dialog;
   private int option;

   public JSerialPanel() {
      super(new GridLayout(5, 2, 10, 10));
      setBorder(new EmptyBorder(5, 5, 5, 5));

      baudRateCB = new JComboBox(BAUD_RATES);
      dataBitsCB = new JComboBox(DATA_BITS);
      stopBitsCB = new JComboBox(STOP_BITS);
      parityCB = new JComboBox(PARITY);
      flowControlCB = new JComboBox(FLOW_CONTROL);

      add(new JLabel("Baud Rate:", JLabel.RIGHT));
      add(baudRateCB);
      add(new JLabel("Data Bits:", JLabel.RIGHT));
      add(dataBitsCB);
      add(new JLabel("Stop Bits:", JLabel.RIGHT));
      add(stopBitsCB);
      add(new JLabel("Parity:", JLabel.RIGHT));
      add(parityCB);
      add(new JLabel("Flow Control:", JLabel.RIGHT));
      add(flowControlCB);
   }

   public void setSerialParameters(SerialParams params) {
      if(params != null) {
         baudRateCB.setSelectedItem(new Integer(params.getBaudRate()));
         dataBitsCB.setSelectedItem(new Integer(params.getDataBits()));
         parityCB.setSelectedIndex(params.getParity());

         switch(params.getStopBits()) {
            case 1:
               stopBitsCB.setSelectedIndex(0);
               break;
            case 2:
               stopBitsCB.setSelectedIndex(2);
               break;
            case 3:
               stopBitsCB.setSelectedIndex(1);
               break;
         }

         if(params.getFlowControl() == SerialPort.FLOWCONTROL_NONE) {
            flowControlCB.setSelectedIndex(0);
         } else if(params.getFlowControl() == (SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT)) {
            flowControlCB.setSelectedIndex(1);
         } else if(params.getFlowControl() == (SerialPort.FLOWCONTROL_XONXOFF_IN | SerialPort.FLOWCONTROL_XONXOFF_OUT)) {
            flowControlCB.setSelectedIndex(2);
         }
      }
   }

   public SerialParams getSerialParameters() {
      int baudRate = ((Integer)baudRateCB.getSelectedItem()).intValue();
      int dataBits = ((Integer)dataBitsCB.getSelectedItem()).intValue();
      int parity = parityCB.getSelectedIndex();
      int stopBits = 0;
      int flowControl = 0;

      switch(stopBitsCB.getSelectedIndex()) {
         case 0:
            stopBits = 1;
            break;
         case 1:
            stopBits = 3;
            break;
         case 2:
            stopBits = 2;
            break;
      }

      switch(flowControlCB.getSelectedIndex()) {
         case 0:
            flowControl = SerialPort.FLOWCONTROL_NONE;
            break;
         case 1:
            flowControl = SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT;
            break;
         case 2:
            flowControl = SerialPort.FLOWCONTROL_XONXOFF_IN | SerialPort.FLOWCONTROL_XONXOFF_OUT;
            break;
      }

      return new SerialParams(baudRate, dataBits, stopBits, parity, flowControl);
   }

   public void ok() {
      option = OK_OPTION;
      dialog.setVisible(false);
   }

   public void cancel() {
      option = CANCEL_OPTION;
      dialog.setVisible(false);
   }

   public int showDialog(Frame owner, SerialParams params) {

      if(dialog == null) {
         dialog = new JDialog(owner, "Serial Port Parameters", true);

         dialog.getContentPane().add(this, BorderLayout.CENTER);
         dialog.getContentPane().add(createButtonPanel(), BorderLayout.SOUTH);
         dialog.pack();
         dialog.setResizable(false);
      }

      option = UNKNOWN_OPTION;

      setSerialParameters(params);
      dialog.setLocationRelativeTo(owner);
      dialog.setVisible(true);

      return option;

   }

   private JComponent createButtonPanel() {

      JPanel buttonP = new JPanel(new FlowLayout(FlowLayout.RIGHT));

      buttonP.setBorder(new EtchedBorder());

      buttonP.add(new JButton(new ProxyAction(this, "ok", "OK", 'o')));
      buttonP.add(new JButton(new ProxyAction(this, "cancel", "Cancel", 'c')));

      return buttonP;
   }

}
