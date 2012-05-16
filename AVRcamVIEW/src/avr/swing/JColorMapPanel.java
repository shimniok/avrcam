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
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import avr.connection.event.*;
import avr.device.event.*;
import avr.swing.*;

import avr.lang.*;

public class JColorMapPanel extends JColorMapInterface {

   public static void main(String[] args) {
      JFrame frame = new JFrame();
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.add(new JColorMapPanel(null));
      frame.pack();
      frame.setVisible(true);
   }

   private JLabel[] systemColorsL;

   private JMessagePanel messageP;
   private JSelectionPanel[] redColorPanels;
   private JSelectionPanel[] greenColorPanels;
   private JSelectionPanel[] blueColorPanels;

   private Action checkAction;
   private Action clearColumnAction;
   private Action clearAction;
   private Action resetAction;
   private Action sendAction;

   public JColorMapPanel(JMessagePanel messageP) {
      super(new BorderLayout());

      this.messageP = messageP;

      checkAction = new ProxyAction(this, "check", "Auto Check", 'a');
      clearColumnAction = new ProxyAction(this, "clearColumn", "Clear Column(s)", 'l');
      clearAction = new ProxyAction(this, "clear", "Clear All", 'c');
      resetAction = new ProxyAction(this, "reset", "Reset", 'r');
      sendAction = new ProxyAction(this, "send", "Send", 's');

      sendAction.setEnabled(false);

      redColorPanels = new JSelectionPanel[AVRSystem.NUM_INTENSITIES];
      greenColorPanels = new JSelectionPanel[AVRSystem.NUM_INTENSITIES];
      blueColorPanels = new JSelectionPanel[AVRSystem.NUM_INTENSITIES];

      JPanel titleP = new JPanel();

      JLabel titleL = new JLabel("System Colors: ");
      titleL.setFont(titleL.getFont().deriveFont(16.0f));

      titleP.add(titleL);

      systemColorsL = new JLabel[8];
      for(int i = 0; i < systemColorsL.length; i++) {
         systemColorsL[i] = new JLabel();
         systemColorsL[i].setBorder(new LineBorder(Color.BLACK));
         systemColorsL[i].setOpaque(true);
         systemColorsL[i].setPreferredSize(new Dimension(20, 20));

         titleP.add(systemColorsL[i]);
      }

      // set the number of rows one less than the number of intensities because we
      // do not use the first row.
      JPanel centerP = new JPanel(new GridLayout(AVRSystem.NUM_INTENSITIES - 1, 1, 0, 5));
      centerP.setBorder(new EmptyBorder(0, 5, 5, 5));

      Box row;

      Dimension rowLabelSize = null;
      int intensityIncrement = (int)(256 / AVRSystem.NUM_INTENSITIES);

      for(int i = 0; i < AVRSystem.NUM_INTENSITIES; i++) {
         redColorPanels[i] = new JSelectionPanel(Color.RED);
         greenColorPanels[i] = new JSelectionPanel(Color.GREEN);
         blueColorPanels[i] = new JSelectionPanel(Color.BLUE);

         row = new Box(BoxLayout.X_AXIS);

         JLabel rowL = new JLabel("" + (i * intensityIncrement), JLabel.CENTER);

         // set all the labels to the same size
         if(rowLabelSize == null) {
            rowLabelSize = new Dimension(rowL.getPreferredSize().width * 4,
                                         rowL.getPreferredSize().height);
         }

         rowL.setPreferredSize(rowLabelSize);
         rowL.setMinimumSize(rowLabelSize);

         row.add(rowL);
         row.add(Box.createHorizontalStrut(5));
         row.add(redColorPanels[i]);
         row.add(Box.createHorizontalStrut(5));
         row.add(greenColorPanels[i]);
         row.add(Box.createHorizontalStrut(5));
         row.add(blueColorPanels[i]);

         // we do not use the first row
         if(i != 0) {
            centerP.add(row);
         }
      }

      Box southBox = new Box(BoxLayout.X_AXIS);
      southBox.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 5, 5, 5)));

      southBox.add(new JButton(checkAction));
      southBox.add(Box.createHorizontalGlue());
      southBox.add(new JButton(clearColumnAction));
      southBox.add(Box.createHorizontalStrut(5));
      southBox.add(new JButton(clearAction));
      southBox.add(Box.createHorizontalStrut(5));
      southBox.add(new JButton(resetAction));
      southBox.add(Box.createHorizontalStrut(5));
      southBox.add(new JButton(sendAction));

      add(titleP, BorderLayout.NORTH);
      add(centerP, BorderLayout.CENTER);
      add(southBox, BorderLayout.SOUTH);

      // load the current color map
      reset();

      AVRSystem.DEVICE.addConnectionListener(new ConnectionHandler());
   }

   private static String formatColorMapException(InvalidColorMapException icme) {

      StringBuffer builder = new StringBuffer("Invalid Color Map: ");

      int[] indicies = icme.getIndicies();

      builder.append("Indicies ");

      for(int i = 0; i < indicies.length; i++) {

         builder.append(indicies[i]);

         if((i + 1) < indicies.length) {
            builder.append(" and ");
         }

      }

      builder.append(" intersect at values Red: ")
             .append(icme.getRed())
             .append(" Green: ")
             .append(icme.getGreen())
             .append(" Blue: ")
             .append(icme.getBlue());

      return builder.toString();

   }

   public void check() {

      try {
         checkMap();

         JOptionPane.showMessageDialog(getRootPane(),
                                       "Color Map is valid.",
                                       "Color Map Validated",
                                       JOptionPane.INFORMATION_MESSAGE);
      } catch(InvalidColorMapException icme) {
         JOptionPane.showMessageDialog(getRootPane(),
                                       formatColorMapException(icme),
                                       "Check Failed",
                                       JOptionPane.ERROR_MESSAGE);
      }

   }

   public boolean isColumnClear(int column) {

      int value = 0;

      /* *********************************************
       * NOTE: This one loop is only checking hte
       * length of the red color panels but is also
       * looping over the green and blue ones!!!!
       **/
      for(int r = 0; r < redColorPanels.length; r++) {
         value |= redColorPanels[r].getValue() |
                  greenColorPanels[r].getValue() |
                  blueColorPanels[r].getValue();
      }

      return (value & (0x01 << (7 - column))) == 0;
   }

   /* *************************************
    * Copied from java.lang.Integer class
    * from the JDK 1.5 version.
    */
   private int bitCount(int i) {
      i = i - ((i >>> 1) & 0x55555555);
      i = (i & 0x33333333) + ((i >>> 2) & 0x33333333);
      i = (i + (i >>> 4)) & 0x0f0f0f0f;
      i = i + (i >>> 8);
      i = i + (i >>> 16);
      return i & 0x3f;
   }

   private void checkMap() throws InvalidColorMapException {
      for(int r = 0; r < redColorPanels.length; r++) {
         int red = redColorPanels[r].getValue();
         for(int g = 0; g < greenColorPanels.length; g++) {
            int green = greenColorPanels[g].getValue();
            for(int b = 0; b < blueColorPanels.length; b++) {
               int blue = blueColorPanels[b].getValue();
               int value = red & green & blue;

               // In JDk 1.5 the Integer class has the bitCount
               // method.  To be backward compatible, use the bitCount
               // method above.
//               if(value != 0 && (Integer.bitCount(value) > 1)) {
               if(value != 0 && (bitCount(value) > 1)) {
                  int[] indicies = new int[bitCount(value)];
                  int count = 0;
                  for(int i = 0; i < 8; i++) {
                     if((value & (0x80 >>> i)) != 0) {
                        indicies[count++] = (i + 1);
                     }
                  }

                  throw new InvalidColorMapException("Color Map is invalid.", indicies, r * 16, g * 16, b * 16);
               }
            }
         }
      }

   }

   public void clearColumn() {

      JPanel displayP = new JPanel(new BorderLayout());

      JPanel selectColP = new JPanel();

      JCheckBox[] colCB = new JCheckBox[8];
      for(int i = 0; i < colCB.length; i++) {
         colCB[i] = new JCheckBox((i + 1) + "");
         selectColP.add(colCB[i]);
      }

      displayP.add(new JLabel("Select Color Map Column:"), BorderLayout.NORTH);
      displayP.add(selectColP, BorderLayout.SOUTH);

      int option = JOptionPane.showConfirmDialog(getRootPane(),
                                                 displayP,
                                                 "Select Column(s) to clear:",
                                                 JOptionPane.OK_CANCEL_OPTION,
                                                 JOptionPane.QUESTION_MESSAGE);

      if(option == JOptionPane.OK_OPTION) {

         /* *********************************************
          * NOTE: This one loop is only checking hte
          * length of the red color panels but is also
          * looping over the green and blue ones!!!!
          **/
         for(int col = 0; col < 8; col++) {
            if(colCB[col].isSelected()) {
               for(int i = 0; i < redColorPanels.length; i++) {
                  redColorPanels[i].set(col, false);
                  greenColorPanels[i].set(col, false);
                  blueColorPanels[i].set(col, false);
               }
            }
         }
      }
   }

   public void clear() {
      /* *********************************************
       * NOTE: This one loop is only checking the
       * length of the red color panels but is also
       * looping over the green and blue ones!!!!
       **/
      for(int col = 0; col < redColorPanels.length; col++) {
         redColorPanels[col].setValue(0);
         greenColorPanels[col].setValue(0);
         blueColorPanels[col].setValue(0);
      }
   }

   public void reset() {
      int[][] colorMap = AVRSystem.DEVICE.getColorMap();

      /* *********************************************
       * NOTE: This one loop is only checking the
       * length of the red color panels but is also
       * looping over the green and blue ones!!!!
       **/
      for(int col = 0; col < redColorPanels.length; col++) {
         redColorPanels[col].setValue(colorMap[0][col]);
         greenColorPanels[col].setValue(colorMap[1][col]);
         blueColorPanels[col].setValue(colorMap[2][col]);
      }

      for(int i = 0; i < systemColorsL.length; i++) {
         systemColorsL[i].setBackground(AVRSystem.DEVICE.getMapColors()[i]);
      }
   }

   public void send() {

      try {

         checkMap();

         int[][] newColorMap = new int[3][AVRSystem.NUM_INTENSITIES];
         for(int r = 0; r < redColorPanels.length; r++) {
            newColorMap[0][r] = redColorPanels[r].getValue();
         }
         for(int g = 0; g < greenColorPanels.length; g++) {
            newColorMap[1][g] = greenColorPanels[g].getValue();
         }
         for(int b = 0; b < blueColorPanels.length; b++) {
            newColorMap[2][b] = blueColorPanels[b].getValue();
         }

         DataListener handler = new DataHandler(newColorMap);
         try {

            AVRSystem.DEVICE.addDataListener(handler);
            getRootPane().getGlassPane().setVisible(true);
            SwingUtilities.getRootPane(messageP).getGlassPane().setVisible(true);
            AVRSystem.DEVICE.sendSetColorMap(newColorMap[0], newColorMap[1], newColorMap[2]);
            messageP.append("Sent Color Map");
         } catch(IOException ioe) {
            AVRSystem.DEVICE.removeDataListener(handler);
            getRootPane().getGlassPane().setVisible(false);
            SwingUtilities.getRootPane(messageP).getGlassPane().setVisible(false);
            ioe.printStackTrace();
            AVRSystem.LOG.severe(ioe.getMessage());
         }
      } catch(InvalidColorMapException icme) {
         JOptionPane.showMessageDialog(getRootPane(),
                                       formatColorMapException(icme),
                                       "Check Failed",
                                       JOptionPane.ERROR_MESSAGE);
      }
   }

   public void setColor(int index, int color) {

      int red   = (color >>> 20) & 0x0F;
      int green = (color >>> 12) & 0x0F;
      int blue  = (color >>>  4) & 0x0F;

      /* *********************************************
       * NOTE: This one loop is only checking the
       * length of the red color panels but is also
       * looping over the green and blue ones!!!!
       **/
      for(int i = 0; i < redColorPanels.length; i++) {
         redColorPanels[i].set(index, i == red);
         greenColorPanels[i].set(index, i == green);
         blueColorPanels[i].set(index, i == blue);
      }

   }

   private final static class JSelectionPanel extends JPanel {

      private JCheckBox[] boxes;

      public JSelectionPanel(Color rowColor) {
         super();
         setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
         setBackground(rowColor);

         boxes = new JCheckBox[8];

         add(Box.createHorizontalGlue());
         for(int i = 0; i < boxes.length; i++) {
            boxes[i] = new JCheckBox();
            boxes[i].setBackground(rowColor);
            if((i != 0) && ((i % 4) == 0)) {
               add(Box.createHorizontalStrut(5));
            }
            add(boxes[i]);
         }
         add(Box.createHorizontalGlue());

      }

      public void set(int index, boolean selected) {
         boxes[index].setSelected(selected);
      }

      public void setValue(int value) {
         for(int i = 0; i < boxes.length; i++) {
            if((value & (1 << (7 - i))) > 0) {
               boxes[i].setSelected(true);
            } else {
               boxes[i].setSelected(false);
            }
         }
      }

      public int getValue() {
         int value = 0;

         for(int i = 0; i < boxes.length; i++) {
            if(boxes[i].isSelected()) {
               value |= 1 << (7 - i);
               value &= 0xFF;
            }
         }

         return value;
      }

   }

   private final class DataHandler extends DataAdapter {

      private int[][] colorMap;

      public DataHandler(int[][] colorMap) {
         this.colorMap = colorMap;
      }

      public void ack() {
         AVRSystem.DEVICE.setColorMap(colorMap);
         reset();
         SwingUtilities.getRootPane(messageP).getGlassPane().setVisible(false);
         getRootPane().getGlassPane().setVisible(false);
         AVRSystem.DEVICE.removeDataListener(this);
      }

      public void nck() {
         getRootPane().getGlassPane().setVisible(false);
         SwingUtilities.getRootPane(messageP).getGlassPane().setVisible(false);
         AVRSystem.DEVICE.removeDataListener(this);
         JOptionPane.showMessageDialog(getRootPane(), "Set Color Map NCK Received", "NCK Received", JOptionPane.ERROR_MESSAGE);
      }

      public void responseTimerExpired() {
         getRootPane().getGlassPane().setVisible(false);
         SwingUtilities.getRootPane(messageP).getGlassPane().setVisible(false);
         AVRSystem.DEVICE.removeDataListener(this);
         JOptionPane.showMessageDialog(messageP.getRootPane(), "Response Timer Expired", "Timer Expired", JOptionPane.ERROR_MESSAGE);
      }

   }

   private final class ConnectionHandler implements ConnectionListener {
      public void connected(ConnectionEvent ce) {
         sendAction.setEnabled(true);
      }

      public void disconnected(ConnectionEvent ce) {
         sendAction.setEnabled(false);
      }

   }

}
