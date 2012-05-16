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
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

import avr.device.event.*;
import avr.lang.*;
import avr.swing.filechooser.LogFileFilter;

public class JCaptureInternalFrame extends JInternalFrame {

   private static final Format DATE_FORMAT;
   private static final Format DATE_FILE_NAME_FORMAT;

   static {
      DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
      DATE_FILE_NAME_FORMAT = new SimpleDateFormat("yyyyMMdd hhmmss");
   }

   private JMessagePanel messageP;
   private JColorMapInterface colorMapP;
   private JCapturePanel captureP;
   private DataListener dataHandler;

   private String filename;

   private JLabel redValueL;
   private JLabel greenValueL;
   private JLabel blueValueL;

   private boolean fromCamera;

   public JCaptureInternalFrame(JMessagePanel messageP) {
      this(messageP, null);
   }

   public JCaptureInternalFrame(JMessagePanel messageP, JColorMapInterface colorMapP) {
      this(messageP, colorMapP, null);
   }

   public JCaptureInternalFrame(JMessagePanel messageP, JColorMapInterface colorMapP, File file) {
      super("Capture Frame" + ((file == null) ? ": " + DATE_FORMAT.format(new Date())
                                              : " " + file.toString()),
            true, true, true, true);

      this.filename = DATE_FILE_NAME_FORMAT.format(new Date()) + ".byr";
      this.fromCamera = file == null;

      this.messageP = messageP;
      this.colorMapP = colorMapP;

      setDefaultCloseOperation(DISPOSE_ON_CLOSE);
      setJMenuBar(createMenuBar());

      redValueL = new JLabel("");
      greenValueL = new JLabel("");
      blueValueL = new JLabel("");

      captureP = new JCapturePanel();

      if(file != null) {
         try {
            captureP.openBayer(file);
         } catch(IOException ioe) {
            AVRSystem.LOG.severe(ioe.getMessage());
            ioe.printStackTrace();
         }
      }

      captureP.addMouseMotionListener(new MouseMotionHandler());

      Box southBox = new Box(BoxLayout.X_AXIS);
      southBox.setBorder(new EmptyBorder(5, 5, 5, 5));

      southBox.add(Box.createHorizontalGlue());
      southBox.add(new JLabel("Red: "));
      southBox.add(redValueL);
      southBox.add(Box.createHorizontalStrut(5));
      southBox.add(new JLabel("Green: "));
      southBox.add(greenValueL);
      southBox.add(Box.createHorizontalStrut(5));
      southBox.add(new JLabel("Blue: "));
      southBox.add(blueValueL);
      southBox.add(Box.createHorizontalGlue());

      getContentPane().add(captureP, BorderLayout.CENTER);
      getContentPane().add(southBox, BorderLayout.SOUTH);

      if(colorMapP != null) {
         captureP.addMouseListener(new MouseHandler());
      }

   }

   private JMenuBar createMenuBar() {
      JMenuBar menubar = new JMenuBar();

      JMenu fileM = new JMenu("File");

      fileM.add(new ProxyAction(this, "save", "Save", 's'));
      fileM.add(new ProxyAction(this, "pack", "Reset Size", 'r'));
      fileM.addSeparator();
      fileM.add(new ProxyAction(this, "dispose", "Exit", 'x'));

      menubar.add(fileM);

      return menubar;
   }

   public void save() {

      javax.swing.filechooser.FileFilter[] filters = AVRSystem.FILE_CHOOSER.getChoosableFileFilters();
      for(int i = 0; i < filters.length; i++) {
         AVRSystem.FILE_CHOOSER.removeChoosableFileFilter(filters[i]);
      }

      AVRSystem.FILE_CHOOSER.addChoosableFileFilter(
         new LogFileFilter("Bayer Image File (*." + AVRSystem.BAYER_FILE_EXT + ")",
                           "." + AVRSystem.BAYER_FILE_EXT));

      AVRSystem.FILE_CHOOSER.setSelectedFile(new File(filename));
      int option = AVRSystem.FILE_CHOOSER.showSaveDialog(getDesktopPane().getRootPane());
      if(option == JFileChooser.APPROVE_OPTION) {
         try {
            File file = AVRSystem.FILE_CHOOSER.getSelectedFile();
            if(!file.getName().toLowerCase().endsWith(AVRSystem.BAYER_FILE_EXT)) {
               file = new File(file.getName() + "." + AVRSystem.BAYER_FILE_EXT);
            }
            captureP.saveBayer(file);
         } catch(IOException ioe) {
            ioe.printStackTrace();
            AVRSystem.LOG.severe(ioe.getMessage());
         }
      }
   }

   public void setVisible(boolean visible) {
      if(fromCamera) {
         if(visible) {
            try {
               dataHandler = new DumpFrameHandler();
               AVRSystem.DEVICE.addDataListener(dataHandler);
               getDesktopPane().getRootPane().getGlassPane().setVisible(true);
               AVRSystem.DEVICE.sendDumpFrame();
               messageP.append("Capture Snapshot");
            } catch(IOException ioe) {
               AVRSystem.DEVICE.removeDataListener(dataHandler);
               AVRSystem.LOG.severe(ioe.getMessage());
               messageP.append("Capture not sent");
            }
         } else {
            AVRSystem.DEVICE.removeDataListener(dataHandler);
         }
      }

      super.setVisible(visible);
   }

   private Point translatePointToImage(Point mouse) {

      Dimension size = captureP.getSize();
      Insets insets = captureP.getInsets();
      Dimension preferredSize = captureP.getPreferredSize();

      double scaleX = size.width / (double)preferredSize.width;
      double scaleY = size.height / (double)preferredSize.height;
      double scale = Math.min(scaleX, scaleY);

      int imageX = (int)(insets.left + ((5 + AVRSystem.IMAGE_WIDTH + 10) * scale));
      int imageY = (int)(insets.top + (5 * scale));
      int imageWidth = (int)(AVRSystem.IMAGE_WIDTH * scale);
      int imageHeight = (int)(AVRSystem.IMAGE_HEIGHT * scale);

      Point imagePoint = null;

      if(((mouse.x >= imageX) && (mouse.x < (imageX + imageWidth))) &&
         ((mouse.y >= imageY) && (mouse.y < (imageY + imageHeight)))) {

         scale = 1 / scale;

         int x = (int)((mouse.x * scale) - (insets.left + 5 + AVRSystem.IMAGE_WIDTH + 10));
         int y = (int)((mouse.y * scale) - (insets.top + 5));

         imagePoint = new Point(x, y);
      }

      return imagePoint;

   }

   private final class DumpFrameHandler extends DataAdapter {

      private int frameCount;

      public DumpFrameHandler() {
         frameCount = 0;
      }

      public void ack() {
         frameCount = 0;
      }

      public void nck() {
         getDesktopPane().getRootPane().getGlassPane().setVisible(false);
         AVRSystem.DEVICE.removeDataListener(this);
         JOptionPane.showMessageDialog(messageP.getRootPane(), "Capture NCK Received", "NCK Received", JOptionPane.ERROR_MESSAGE);
      }

      public void responseTimerExpired() {
         getDesktopPane().getRootPane().getGlassPane().setVisible(false);
         AVRSystem.DEVICE.removeDataListener(this);
         JOptionPane.showMessageDialog(messageP.getRootPane(), "Response Timer Expired", "Timer Expired", JOptionPane.ERROR_MESSAGE);
      }

      public void frameData(ByteBuffer data) {

         frameCount++;
         captureP.setRow(data.get() & 0xFF, data, frameCount == 0x48);

         if(frameCount == 0x48) {
            AVRSystem.DEVICE.removeDataListener(this);
            getDesktopPane().getRootPane().getGlassPane().setVisible(false);
         }

      }

   }

   private final class MouseHandler extends MouseAdapter {

      private JPopupMenu popupM;
      private int color;

      public void mouseReleased(MouseEvent me) {
         if(SwingUtilities.isRightMouseButton(me)) {
            Point imagePoint = translatePointToImage(me.getPoint());

            if(imagePoint != null) {

               color = captureP.getRGB(imagePoint.x, imagePoint.y);

               if(popupM == null) {
                  popupM = new JPopupMenu();
                  try {

                     JMenuItem colorMapMI = new JMenuItem("Add to Color Map");
                     colorMapMI.addActionListener(new ActionHandler());
                     popupM.add(colorMapMI);
                  } catch(Exception e) {
                  }
               }

               popupM.show((Component)me.getSource(), me.getX(), me.getY());

            }

         }

      }

      public final class ActionHandler implements ActionListener {

         private JPanel displayP = null;
         private JRadioButton[] colRB = null;

         public void actionPerformed(ActionEvent ae) {

            int column = 0;
            int option = JOptionPane.YES_OPTION;

            do {

               option = JOptionPane.YES_OPTION;
               column = getColorMapColumn();

               if(column != -1) {

                  if(!colorMapP.isColumnClear(column)) {

                     option = JOptionPane.showConfirmDialog(getRootPane(),
                        "Index " + column + " is already set. Overwrite current value?",
                        "Overwrite current index value?",
                        JOptionPane.
                        YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                  }
               } else {
                  option = JOptionPane.CANCEL_OPTION;
               }

            } while(option == JOptionPane.NO_OPTION);

            if(option == JOptionPane.YES_OPTION) {
               SwingUtilities.getWindowAncestor(colorMapP).setVisible(true);
               colorMapP.setColor(column, color);
            }
         }

         private int getColorMapColumn() {
            if(displayP == null) {
               displayP = new JPanel(new BorderLayout());

               JPanel selectColP = new JPanel();

               ButtonGroup bg = new ButtonGroup();

               colRB = new JRadioButton[8];
               for(int i = 0; i < colRB.length; i++) {
                  colRB[i] = new JRadioButton((i + 1) + "");
                  bg.add(colRB[i]);
                  selectColP.add(colRB[i]);
               }

               colRB[0].setSelected(true);

               displayP.add(new JLabel("Select Color Map Column:"), BorderLayout.NORTH);
               displayP.add(selectColP, BorderLayout.SOUTH);
            }

            int option = JOptionPane.showConfirmDialog(getRootPane(),
                                                       displayP,
                                                       "Add to Color Map Column",
                                                       JOptionPane.OK_CANCEL_OPTION,
                                                       JOptionPane.QUESTION_MESSAGE);

            int selected = -1;

            if(option == JOptionPane.OK_OPTION) {
               for(int i = 0; selected == -1 && i < colRB.length; i++) {
                  if(colRB[i].isSelected()) {
                     selected = i;
                  }
               }

            }

            return selected;

         }

      }

   }

   private final class MouseMotionHandler implements MouseMotionListener {

      public void mouseMoved(MouseEvent me) {

         Point imagePoint = translatePointToImage(me.getPoint());

         if(imagePoint != null) {

            int color = captureP.getRGB(imagePoint.x, imagePoint.y);

            redValueL.setText(((color & 0xFF0000) >> 16) + "");
            greenValueL.setText(((color & 0x00FF00) >> 8) + "");
            blueValueL.setText(((color & 0x0000FF) >> 0) + "");

         } else {

            redValueL.setText("");
            greenValueL.setText("");
            blueValueL.setText("");

         }

      }

      public void mouseDragged(MouseEvent me) {
      }
   }

}
