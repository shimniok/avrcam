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
import java.nio.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.metal.MetalLookAndFeel;

import avr.device.event.*;
import avr.lang.*;

public class JMessagePanel extends JPanel {

   private JTextArea messageTA;

   public JMessagePanel() {
      super(new BorderLayout());

      JLabel titleL = new JLabel("System Messages", JLabel.CENTER);
      titleL.setFont(titleL.getFont().deriveFont(16.0f));

      JButton closeB = new JButton(new ProxyAction(this, "close", UIManager.getIcon("InternalFrame.paletteCloseIcon")));
      closeB.setBackground(MetalLookAndFeel.getPrimaryControlShadow());
      closeB.setBorder(new EmptyBorder(0, 0, 0, 0));

      messageTA = new JTextArea(6, 20);
      messageTA.setEditable(false);

      Box titleBox = new Box(BoxLayout.X_AXIS);
      titleBox.setBorder(new EmptyBorder(5, 5, 5, 5));
      titleBox.add(titleL);
      titleBox.add(Box.createHorizontalGlue());
      titleBox.add(closeB);

      JPanel southP = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      southP.setBorder(new EtchedBorder());
      southP.add(new JButton(new ProxyAction(this, "clear", "Clear", 'c')));

      add(titleBox, BorderLayout.NORTH);
      add(new JScrollPane(messageTA), BorderLayout.CENTER);
      add(southP, BorderLayout.SOUTH);

      AVRSystem.DEVICE.addDataListener(new DataHandler(messageTA));
   }

   public void append(String message) {
      messageTA.append(message);
      messageTA.append("\n");
   }

   public void clear() {
      messageTA.setText("");
   }

   public void close() {
      setVisible(false);
   }

   private final static class DataHandler implements DataListener {

      private JTextArea messageTA;

      public DataHandler(JTextArea messageTA) {
         this.messageTA = messageTA;
      }

      public void ack() {
         messageTA.append("ACK\n");
      }

      public void nck() {
         messageTA.append("NCK\n");
      }

      public void version(String version) {
         messageTA.append(version);
         messageTA.append("\n");
      }

      public void responseTimerExpired() {
         messageTA.append("Response Timer Expired\n");
      }

      public void frameData(ByteBuffer data) {
         messageTA.append("Frame Data (" + (data.get() & 0xFF) + ")\n");
      }

      public void trackingData(ByteBuffer data) {
         messageTA.append("Tracking Data (" + (data.get() & 0xFF) + ")\n");
      }

   }

}
