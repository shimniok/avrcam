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
import javax.swing.*;
import javax.swing.border.*;

import avr.device.event.*;
import avr.lang.*;

public class JAboutDialog extends JDialog {

   private static final String ABOUT_INFO =
      "AVRcamVIEW:<br>" +
      "&nbsp;&nbsp;&nbsp;Version: " + AVRSystem.RELEASE_MAJOR + "." + AVRSystem.RELEASE_MINOR + "<br>" +
      "&nbsp;&nbsp;&nbsp;Build Date: " + AVRSystem.BUILD_DATE;

   private JLabel aboutL;
   private DataListener handler;
   private JMessagePanel messageP;

   public JAboutDialog(Frame owner, JMessagePanel messageP) {
      super(owner, "About", true);

      this.messageP = messageP;

      Font labelFont = new Font("Dialog", Font.BOLD, 12);

      aboutL = new JLabel("<html>" + ABOUT_INFO + "</html>");
      aboutL.setFont(labelFont);
      aboutL.setBorder(new EmptyBorder(20, 40, 20, 40));

      Container contentPane = getContentPane();

      JPanel southP = new JPanel();
      southP.add(new JButton( new ProxyAction(this, "dispose", "OK", 'o')));

      contentPane.add(aboutL, BorderLayout.CENTER);
      contentPane.add(southP, BorderLayout.SOUTH);

      setResizable(false);

      pack();

   }

   public void setVersion(String version) {
      StringBuffer builder = new StringBuffer();
      builder.append("<html>")
             .append(ABOUT_INFO)
             .append("<br><br><hr><br>AVRcam")
             .append("<BR>&nbsp;&nbsp;&nbsp;&nbsp;")
             .append(version)
             .append("<html>");
      aboutL.setText(builder.toString());
      pack();
   }

   public void showDialog() {

      setLocationRelativeTo(getOwner());

      if(AVRSystem.DEVICE.isConnected()) {
         try {
            handler = new DataHandler(this);
            AVRSystem.DEVICE.addDataListener(handler);
            messageP.append("Get Version");
            AVRSystem.DEVICE.sendGetVersion();
         } catch(IOException ioe) {
            AVRSystem.LOG.severe("Could not get version: " + ioe.getMessage());
            AVRSystem.DEVICE.removeDataListener(handler);
         }
      }
      setVisible(true);
   }

   public void dispose() {
      AVRSystem.DEVICE.removeDataListener(handler);
      super.dispose();
   }

   private final static class DataHandler extends DataAdapter {

      private JAboutDialog dialog;

      public DataHandler(JAboutDialog dialog) {
         this.dialog = dialog;
      }

      public void nck() {
         AVRSystem.DEVICE.removeDataListener(this);
         dialog.setVersion("Unable to retrieve version.");
      }

      public void responseTimerExpired() {
         AVRSystem.DEVICE.removeDataListener(this);
         dialog.setVersion("Response Timer Expired.");
      }

      public void version(String version) {
         AVRSystem.DEVICE.removeDataListener(this);
         dialog.setVersion(version);
      }

   }

}
