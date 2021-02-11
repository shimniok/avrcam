package avr.swing;

import avr.connection.event.ConnectionEvent;
import avr.connection.event.ConnectionListener;
import avr.device.event.DataAdapter;
import avr.device.event.DataListener;
import avr.lang.AVRSystem;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.Enumeration;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalSliderUI;

public class JNewColorMapPanel extends JColorMapInterface {

   public static void main(String[] args) {

      JFrame frame = new JFrame("Testing");

//      frame.getContentPane().setLayout(new FlowLayout());
      frame.getContentPane().add(new JNewColorMapPanel(null), BorderLayout.CENTER);
      frame.pack();

      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      frame.show();
   }

   private JSelectionPanel redP;
   private JSelectionPanel greenP;
   private JSelectionPanel blueP;
   private JIndexRadioButton indicies[];

   private static final int RED_INDEX = 0;
   private static final int GREEN_INDEX = 1;
   private static final int BLUE_INDEX = 2;

   private static final int RED_MASK = 0xFF0000;
   private static final int GREEN_MASK = 0x00FF00;
   private static final int BLUE_MASK = 0x0000FF;

   private static final int MIN_INDEX = 0;
   private static final int MAX_INDEX = 1;

   private int[][][] cachedMap;

   private JMessagePanel messageP;

   private Action checkAction;
   private Action clearColumnAction;
   private Action clearAction;
   private Action resetAction;
   private Action sendAction;

   public JNewColorMapPanel(JMessagePanel messageP) {
      super(null);

      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

      this.messageP = messageP;

      cachedMap = new int[8][3][2];

      redP = new JSelectionPanel(JSelectionPanel.RED_INDEX);
      greenP = new JSelectionPanel(JSelectionPanel.GREEN_INDEX);
      blueP = new JSelectionPanel(JSelectionPanel.BLUE_INDEX);

      JPanel topP = new JPanel();
      topP.add(new JLabel("Color Index:"));

      ButtonGroup bg = new ButtonGroup();

      indicies = new JIndexRadioButton[8];

      ProxyAction indexAction = new ProxyAction(this, "updateSliders", (String)null);
      ItemListener itemHandler = new ItemHandler();

      for(int i = 0; i < 8; i++) {
         indicies[i] = new JIndexRadioButton("" + (i + 1), i == 0);
         indicies[i].addActionListener(indexAction);
         indicies[i].addItemListener(itemHandler);
         indicies[i].setOpaque(false);
         bg.add(indicies[i]);
         topP.add(indicies[i]);
      }

      checkAction = new ProxyAction(this, "check", "Auto Check", 'a');
      clearColumnAction = new ProxyAction(this, "clearColumn", "Clear Indicies", 'l');
      clearAction = new ProxyAction(this, "clear", "Clear All", 'c');
      resetAction = new ProxyAction(this, "reset", "Reset", 'r');
      sendAction = new ProxyAction(this, "send", "Send", 's');

      sendAction.setEnabled(false);

      Box southBox = new Box(BoxLayout.X_AXIS);
      southBox.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 5, 5, 5)));

      southBox.add(new JButton(checkAction));
      southBox.add(Box.createHorizontalGlue());
      southBox.add(Box.createHorizontalStrut(5));
      southBox.add(new JButton(clearColumnAction));
      southBox.add(Box.createHorizontalStrut(5));
      southBox.add(new JButton(clearAction));
      southBox.add(Box.createHorizontalStrut(5));
      southBox.add(new JButton(resetAction));
      southBox.add(Box.createHorizontalStrut(5));
      southBox.add(new JButton(sendAction));

      add(topP);
      add(Box.createVerticalStrut(5));
      add(redP);
      add(Box.createVerticalStrut(5));
      add(greenP);
      add(Box.createVerticalStrut(5));
      add(blueP);
      add(Box.createVerticalStrut(5));
      add(southBox);

      reset();

      AVRSystem.DEVICE.addConnectionListener(new ConnectionHandler());
   }

   private static String formatColorMapException(InvalidColorMapException icme) {

      StringBuilder builder = new StringBuilder("Invalid Color Map: ");

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
         JOptionPane.showMessageDialog(getRootPane(), formatColorMapException(icme));
      }

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

      updateCacheMap();

      int[][] colorMap = convert();

      for(int redI = 0; redI < AVRSystem.NUM_INTENSITIES; redI++) {
         int red = colorMap[0][redI];
         for(int greenI = 0; greenI < AVRSystem.NUM_INTENSITIES; greenI++) {
            int green = colorMap[1][greenI];
            for(int blueI = 0; blueI < AVRSystem.NUM_INTENSITIES; blueI++) {
               int blue = colorMap[2][blueI];

               int value = red & green & blue;

               // In JDk 1.5 the Integer class has the bitCount
               // method.  To be backward compatible, use the bitCount
               // method above.
//               if(value != 0 && (Integer.bitCount(value) > 1)) {
               if(value != 0 && (bitCount(value) > 1)) {

                  int[] myIndicies = new int[bitCount(value)]; // mes 04/18/2012
                  int count = 0;
                  for(int i = 0; i < 8; i++) {
                     if((value & (0x80 >>> i)) != 0) {
                        myIndicies[count++] = (i + 1); // mes 04/18/2012
                     }
                  }

                  throw new InvalidColorMapException("Color Map is invalid.", myIndicies, redI * 16, greenI * 16, blueI * 16); // mes 04/18/2012
               }

            }

         }
      }

   }

   private int[][] convert() {

      int[][] colorMap = new int[3][AVRSystem.NUM_INTENSITIES];

      for(int color = 0; color < 3; color++) {
         for(int col = 0; col < 8; col++) {
            for(int i = 1; i < AVRSystem.NUM_INTENSITIES; i++) {

               int min = cachedMap[col][color][MIN_INDEX];
               int max = cachedMap[col][color][MAX_INDEX];

               if((min <= (i * 16)) && ((i * 16) <= max)) {
                  colorMap[color][i] |= (0x01 << (7 - col));
               }

            }

         }
      }

      return colorMap;

   }

    @Override
   public boolean isColumnClear(int column) {

      return cachedMap[column][RED_INDEX][MIN_INDEX] == 0 &&
             cachedMap[column][RED_INDEX][MAX_INDEX] == 0 &&
             cachedMap[column][GREEN_INDEX][MIN_INDEX] == 0 &&
             cachedMap[column][GREEN_INDEX][MAX_INDEX] == 0 &&
             cachedMap[column][BLUE_INDEX][MIN_INDEX] == 0 &&
             cachedMap[column][BLUE_INDEX][MAX_INDEX] == 0;

   }

   public void clear() {
      cachedMap = new int[8][3][2];

      updateSliders();
      updateBackgrounds();
   }

   public void reset() {

      int[][] colorMap = AVRSystem.DEVICE.getColorMap();

      for(int c = 0; c < 8; c++) {
         int minRed = 0;
         int maxRed = 0;
         int minGreen = 0;
         int maxGreen = 0;
         int minBlue = 0;
         int maxBlue = 0;

         for(int i = 0; i < AVRSystem.NUM_INTENSITIES; i++) {

            int value = i * 16;

            if((colorMap[0][i] & (0x01 << (7 - c))) != 0) {
               if(minRed == 0 || value < minRed) {
                  minRed = value;
               }
               if(maxRed < (value)) {
                  maxRed = value;
               }
            }

            if((colorMap[1][i] & (0x01 << (7 - c))) != 0) {
               if(minGreen == 0 || value < minGreen) {
                  minGreen = value;
               }
               if(maxGreen < (value)) {
                  maxGreen = value;
               }
            }

            if((colorMap[2][i] & (0x01 << (7 - c))) != 0) {
               if(minBlue == 0 || value < minBlue) {
                  minBlue = value;
               }
               if(maxBlue < (value)) {
                  maxBlue = value;
               }
            }

         }

         int col = c;

         cachedMap[col][RED_INDEX][MIN_INDEX] = minRed;
         cachedMap[col][RED_INDEX][MAX_INDEX] = maxRed;

         cachedMap[col][GREEN_INDEX][MIN_INDEX] = minGreen;
         cachedMap[col][GREEN_INDEX][MAX_INDEX] = maxGreen;

         cachedMap[col][BLUE_INDEX][MIN_INDEX] = minBlue;
         cachedMap[col][BLUE_INDEX][MAX_INDEX] = maxBlue;

      }

      updateSliders();
      updateBackgrounds();

   }

   public void clearColumn() {

      JPanel displayP = new JPanel(new BorderLayout());

      JPanel selectColP = new JPanel();

      JCheckBox[] colCB = new JCheckBox[8];
      for(int i = 0; i < colCB.length; i++) {
         colCB[i] = new JCheckBox((i + 1) + "");
         selectColP.add(colCB[i]);
      }

      displayP.add(new JLabel("Select Color Map Index:"), BorderLayout.NORTH);
      displayP.add(selectColP, BorderLayout.SOUTH);

      int option = JOptionPane.showConfirmDialog(getRootPane(),
                                                 displayP,
                                                 "Select Indicies to clear:",
                                                 JOptionPane.OK_CANCEL_OPTION,
                                                 JOptionPane.QUESTION_MESSAGE);

      if(option == JOptionPane.OK_OPTION) {

         /* *********************************************
          * NOTE: This one loop is only checking the
          * length of the red color panels but is also
          * looping over the green and blue ones!!!!
          **/
         for(int col = 0; col < 8; col++) {
            if(colCB[col].isSelected()) {
               cachedMap[col][RED_INDEX][MIN_INDEX] = 0;
               cachedMap[col][RED_INDEX][MAX_INDEX] = 0;
               cachedMap[col][GREEN_INDEX][MIN_INDEX] = 0;
               cachedMap[col][GREEN_INDEX][MAX_INDEX] = 0;
               cachedMap[col][BLUE_INDEX][MIN_INDEX] = 0;
               cachedMap[col][BLUE_INDEX][MAX_INDEX] = 0;
            }
         }

         updateSliders();
         updateBackgrounds();
      }
   }

   public void send() {

      try {

         checkMap();

         int[][] newColorMap = convert();

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

      int minRed = (color & RED_MASK) >>> 16;
      int minGreen = (color & GREEN_MASK) >>> 8;
      int minBlue = (color & BLUE_MASK) >>> 0;

      int maxRed = minRed;
      int maxGreen = minGreen;
      int maxBlue = minBlue;

      if(minRed == 16) {
         maxRed *= 3;
      } else if(minRed == 240) {
         minRed -= (16 * 2);
      } else {
         minRed -= 16;
         maxRed += 16;
      }

      if(minGreen == 16) {
         maxGreen *= 3;
      } else if(minGreen == 240) {
         minGreen -= (16 * 2);
      } else {
         minGreen -= 16;
         maxGreen += 16;
      }

      if(minBlue == 16) {
         maxBlue *= 3;
      } else if(minBlue == 240) {
         minBlue -= (16 * 2);
      } else {
         minBlue -= 16;
         maxBlue += 16;
      }

      cachedMap[index][RED_INDEX][MIN_INDEX] = minRed;
      cachedMap[index][RED_INDEX][MAX_INDEX] = maxRed;

      cachedMap[index][GREEN_INDEX][MIN_INDEX] = minGreen;
      cachedMap[index][GREEN_INDEX][MAX_INDEX] = maxGreen;

      cachedMap[index][BLUE_INDEX][MIN_INDEX] = minBlue;
      cachedMap[index][BLUE_INDEX][MAX_INDEX] = maxBlue;

      indicies[index].setSelected(true);

      updateSliders();
//      updateBackgrounds();

   }

   private void updateCacheMap() {

      for(int i = 0; i < 8; i++) {
         if(indicies[i].isSelected()) {
            cachedMap[i][RED_INDEX][MIN_INDEX] = redP.getMin();
            cachedMap[i][RED_INDEX][MAX_INDEX] = redP.getMax();
            cachedMap[i][GREEN_INDEX][MIN_INDEX] = greenP.getMin();
            cachedMap[i][GREEN_INDEX][MAX_INDEX] = greenP.getMax();
            cachedMap[i][BLUE_INDEX][MIN_INDEX] = blueP.getMin();
            cachedMap[i][BLUE_INDEX][MAX_INDEX] = blueP.getMax();
         }
      }

   }

   public void update() {
      updateCacheMap();
      updateSliders();
   }

   public void updateBackgrounds() {

      for(int i = 0; i < indicies.length; i++) {

         int avgRed = (cachedMap[i][RED_INDEX][MIN_INDEX] +
                       cachedMap[i][RED_INDEX][MAX_INDEX]) / 2;
         int avgGreen = (cachedMap[i][GREEN_INDEX][MIN_INDEX] +
                         cachedMap[i][GREEN_INDEX][MAX_INDEX]) / 2;
         int avgBlue = (cachedMap[i][BLUE_INDEX][MIN_INDEX] +
                        cachedMap[i][BLUE_INDEX][MAX_INDEX]) / 2;

         indicies[i].setToolTipText("Average Color: Red (" + avgRed + ") Green (" + avgGreen + ") Blue (" + avgBlue + ")");

         indicies[i].setMinColor(
            new Color(cachedMap[i][RED_INDEX][MIN_INDEX],
                      cachedMap[i][GREEN_INDEX][MIN_INDEX],
                      cachedMap[i][BLUE_INDEX][MIN_INDEX]));
         indicies[i].setMaxColor(
            new Color(cachedMap[i][RED_INDEX][MAX_INDEX],
                      cachedMap[i][GREEN_INDEX][MAX_INDEX],
                      cachedMap[i][BLUE_INDEX][MAX_INDEX]));
         indicies[i].repaint();

      }

   }

   public void updateSliders() {

      for(int i = 0; i < indicies.length; i++) {

         if(indicies[i].isSelected()) {

            redP.setMin(cachedMap[i][RED_INDEX][MIN_INDEX]);
            redP.setMax(cachedMap[i][RED_INDEX][MAX_INDEX]);
            greenP.setMin(cachedMap[i][GREEN_INDEX][MIN_INDEX]);
            greenP.setMax(cachedMap[i][GREEN_INDEX][MAX_INDEX]);
            blueP.setMin(cachedMap[i][BLUE_INDEX][MIN_INDEX]);
            blueP.setMax(cachedMap[i][BLUE_INDEX][MAX_INDEX]);

         }

      }


   }

   private static final class JIndexRadioButton extends JRadioButton {

      private Color min;
      private Color max;

      public JIndexRadioButton(String text, boolean selected) {
         super(text, selected);
         min = Color.BLACK;
         max = Color.BLACK;
      }

      public Dimension getPreferredSize() {
         Dimension size = super.getPreferredSize();
         size.width += 20;
         return size;
      }

      public void setMinColor(Color min) {
         this.min = min;
      }

      public void setMaxColor(Color max) {
         this.max = max;
      }

      public void paintComponent(Graphics g) {

         AbstractButton b = (AbstractButton) this;
         JComponent c = this;

         Dimension size = c.getSize();

         Font f = c.getFont();
         FontMetrics fm = c.getFontMetrics(f);

         Rectangle viewRect = new Rectangle(size);
         Rectangle iconRect = new Rectangle();
         Rectangle textRect = new Rectangle();

         Insets i = c.getInsets();
         viewRect.x += i.left;
         viewRect.y += i.top;
         viewRect.width -= (i.right + viewRect.x);
         viewRect.height -= (i.bottom + viewRect.y);

         Icon altIcon = b.getIcon();

         String text = SwingUtilities.layoutCompoundLabel(
             c, fm, b.getText(), altIcon != null ? altIcon : UIManager.getIcon("RadioButton.icon"),
             b.getVerticalAlignment(), b.getHorizontalAlignment(),
             b.getVerticalTextPosition(), b.getHorizontalTextPosition(),
             viewRect, iconRect, textRect, b.getIconTextGap());

         Graphics2D g2d = (Graphics2D)g;

         Paint paint = g2d.getPaint();
         Stroke stroke = g2d.getStroke();

         g2d.setPaint(new GradientPaint(0, 0, min, size.width, 0, max));
         g2d.setStroke(new BasicStroke(size.height));
         g2d.fillRect(0, 0, size.width, size.height);

         g2d.setPaint(paint);
         g2d.setStroke(stroke);

         super.paintComponent(g);

         g.setColor(Color.WHITE);
         g.fillRect(textRect.x - 1, textRect.y + 2, textRect.width + 2, textRect.height - 4);
         paintText(g, c, textRect, text);


      }

      protected void paintText(Graphics g, JComponent c, Rectangle textRect,
                               String text) {
         AbstractButton b = (AbstractButton)c;
         ButtonModel myModel = b.getModel(); // mes 04/18/2012
         
         FontMetrics fm = c.getFontMetrics(c.getFont()); // mes 04/18/2012
         int mnemonicIndex = b.getDisplayedMnemonicIndex();

         /* Draw the Text */
         if(myModel.isEnabled()) { // mes 04/18/2012
            /*** paint the text normally */
            g.setColor(b.getForeground());
            /*
            SwingUtilities2.drawStringUnderlineCharAt(c, g, text, mnemonicIndex,
               textRect.x + UIManager.getInt("Button.textShiftOffset"),
               textRect.y + fm.getAscent() + UIManager.getInt("Button.textShiftOffset"));*/ // mes 04/18/2012
         } else {
            /*** paint the text disabled ***/
            g.setColor(b.getBackground().brighter());
              // mes 04/18/2012             
            /*
            SwingUtilities2.drawStringUnderlineCharAt(c, g, text, mnemonicIndex,
               textRect.x, textRect.y + fm.getAscent());
            g.setColor(b.getBackground().darker());
            SwingUtilities2.drawStringUnderlineCharAt(c, g, text, mnemonicIndex,
               textRect.x - 1, textRect.y + fm.getAscent() - 1);
               */
         }
      }

   }

   private final class ItemHandler implements ItemListener {

      public void itemStateChanged(ItemEvent e) {
         if(e.getStateChange() == ItemEvent.DESELECTED) {
            for(int i = 0; i < 8; i++) {
               if(indicies[i] == e.getSource()) {
                  cachedMap[i][RED_INDEX][MIN_INDEX] = redP.getMin();
                  cachedMap[i][RED_INDEX][MAX_INDEX] = redP.getMax();
                  cachedMap[i][GREEN_INDEX][MIN_INDEX] = greenP.getMin();
                  cachedMap[i][GREEN_INDEX][MAX_INDEX] = greenP.getMax();
                  cachedMap[i][BLUE_INDEX][MIN_INDEX] = blueP.getMin();
                  cachedMap[i][BLUE_INDEX][MAX_INDEX] = blueP.getMax();
               }
            }

         }
      }

   }

   private static final class JSelectionPanel extends Box {

      public static final int RED_INDEX = 0;
      public static final int GREEN_INDEX = 1;
      public static final int BLUE_INDEX = 2;

      private JSlider minS;
      private JSlider maxS;
      private JColorLabel colorL;

      private int index;

      public JSelectionPanel(int index) {
         super(BoxLayout.Y_AXIS);

         this.index = index;

         minS = new JSlider(0, 16 * (AVRSystem.NUM_INTENSITIES - 1), 16);
         maxS = new JSlider(0, 16 * (AVRSystem.NUM_INTENSITIES - 1), 16);

         minS.setUI(new AVRSliderUI(AVRSliderUI.RIGHT));
         maxS.setUI(new AVRSliderUI(AVRSliderUI.LEFT));

         colorL = new JColorLabel(index);

         minS.setMajorTickSpacing(16);
         minS.setSnapToTicks(true);
         minS.setOpaque(false);

         maxS.setPaintTicks(true);
         maxS.setMajorTickSpacing(16);
         maxS.setSnapToTicks(true);
         maxS.setOpaque(false);
         maxS.setPaintLabels(true);

         minS.addChangeListener(new MinChangeHandler());
         maxS.addChangeListener(new MaxChangeHandler());

         minS.setForeground(Color.WHITE);
         maxS.setForeground(Color.WHITE);

         int sliderWidth = Math.max(minS.getPreferredSize().width,
                                    maxS.getPreferredSize().width);

         sliderWidth = (int)(sliderWidth * 1.75);

         minS.setBorder(new CompoundBorder(minS.getBorder(), new EmptyBorder(0, 3, 0, 3)));

         Dimension sliderDim = minS.getPreferredSize();
         sliderDim.width = sliderWidth;
         minS.setPreferredSize(sliderDim);

         sliderDim = maxS.getPreferredSize();
         sliderDim.width = sliderWidth;
         maxS.setPreferredSize(sliderDim);

         Enumeration labels = maxS.getLabelTable().elements();

         while(labels.hasMoreElements()) {
            ((JLabel)labels.nextElement()).setForeground(Color.WHITE);
         }

         add(minS);
         add(maxS);

      }

      public int getMin() {
         return minS.getValue();
      }

      public void setMin(int min) {
         minS.setValue(min);
      }

      public int getMax() {
         return maxS.getValue();
      }

      public void setMax(int max) {
         maxS.setValue(max);
      }

      public void paintComponent(Graphics g) {

         Insets insets = getInsets();
         Dimension size = getSize();

         Graphics2D g2d = (Graphics2D)g;

         int minRed = 0;
         int minGreen = 0;
         int minBlue = 0;

         int maxRed = 0;
         int maxGreen = 0;
         int maxBlue = 0;

         switch(index) {
            case RED_INDEX:
               maxRed = maxS.getValue();
               minRed = minS.getValue();
               break;
            case GREEN_INDEX:
               maxGreen = maxS.getValue();
               minGreen = minS.getValue();
               break;
            case BLUE_INDEX:
               maxBlue = maxS.getValue();
               minBlue = minS.getValue();
               break;
         }

         Color minColor = new Color(minRed, minGreen, minBlue);
         Color maxColor = new Color(maxRed, maxGreen, maxBlue);

         Paint paint = new GradientPaint(insets.left, insets.top, minColor,
                                         size.width - insets.right, insets.top, maxColor);
         g2d.setPaint(paint);
         g2d.fillRect(insets.left, insets.top,
                      size.width - insets.left - insets.right,
                      size.height - insets.top - insets.bottom);

      }

      private final class MinChangeHandler implements ChangeListener {

         public void stateChanged(ChangeEvent e) {
            if(maxS.getValue() < minS.getValue()) {
               maxS.setValue(minS.getValue());
            }
//            colorL.setMin(minS.getValue());
            repaint();
         }

      }

      private final class MaxChangeHandler implements ChangeListener {

         public void stateChanged(ChangeEvent e) {
            if(maxS.getValue() < minS.getValue()) {
               minS.setValue(maxS.getValue());
            }
//            colorL.setMax(maxS.getValue());
            repaint();
         }

      }

      private static final class JColorLabel extends JLabel {

         private int index;

         private int max;
         private int min;

         public JColorLabel(int index) {
            this.index = index;
            setBorder(new LineBorder(Color.BLACK));
            setOpaque(true);
            min = 0;
            max = 0;
         }

         public void setMin(int min) {
            this.min = min;
            repaint();
         }

         public void setMax(int max) {
            this.max = max;
            repaint();
         }

         public void paintComponent(Graphics g) {

            Insets insets = getInsets();
            Dimension size = getSize();

            Graphics2D g2d = (Graphics2D)g;

            int minRed = 0;
            int minGreen = 0;
            int minBlue = 0;

            int maxRed = 0;
            int maxGreen = 0;
            int maxBlue = 0;

            switch(index) {
               case RED_INDEX:
                  maxRed = max;
                  minRed = min;
                  break;
               case GREEN_INDEX:
                  maxGreen = max;
                  minGreen = min;
                  break;
               case BLUE_INDEX:
                  maxBlue = max;
                  minBlue = min;
                  break;
            }

            Color minColor = new Color(minRed, minGreen, minBlue);
            Color maxColor = new Color(maxRed, maxGreen, maxBlue);

            Paint paint = new GradientPaint(insets.left, insets.top, minColor,
                                            size.width - insets.right, insets.top, maxColor);
            g2d.setPaint(paint);
            g2d.fillRect(insets.left, insets.top,
                         size.width - insets.left - insets.right,
                         size.height - insets.top - insets.bottom);

         }

      }

      private static final class AVRSliderUI extends MetalSliderUI {

         public static final int LEFT = 0;
         public static final int RIGHT = 1;

         private int direction;

         public AVRSliderUI(int direction) {
            this.direction = direction;
            filledSlider = false;
         }

         // overridden to not fill in the track
         public void paintTrack(Graphics g) {

            boolean drawInverted = this.drawInverted();

            Rectangle paintRect = avrGetPaintTrackRect();
            g.translate(paintRect.x, paintRect.y);

            int w = paintRect.width;
            int h = paintRect.height;

            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawRect(0, 0, w - 1, h - 1);

            g.translate(-paintRect.x, -paintRect.y);

         }

         // overridden to point the thumb to the left or the right
         public void paintThumb(Graphics g) {
            Rectangle knobBounds = thumbRect;
            if(direction == LEFT) {
                g.translate( knobBounds.x, knobBounds.y );
                ((Graphics2D)g).rotate(Math.PI, knobBounds.width / 2, knobBounds.height / 2);

                vertThumbIcon.paintIcon( slider, g, 0, 0 );

                ((Graphics2D)g).rotate(-Math.PI, knobBounds.width / 2, knobBounds.height / 2);
                g.translate( -knobBounds.x, -knobBounds.y );
            } else {
                g.translate( knobBounds.x, knobBounds.y );

                vertThumbIcon.paintIcon( slider, g, 0, 0 );

                g.translate( -knobBounds.x, -knobBounds.y );
            }
         }

         // this method is private in the MetalSliderUI class, so I have recreated
         // the method so I can use it
         private Rectangle avrGetPaintTrackRect() {
            int trackLeft = 0, trackRight = 0, trackTop = 0, trackBottom = 0;
            trackBottom = (trackRect.height - 1) - getThumbOverhang();
            trackTop = trackBottom - (getTrackWidth() - 1);
            trackRight = trackRect.width - 1;
            return new Rectangle(trackRect.x + trackLeft, trackRect.y + trackTop,
                                 trackRight - trackLeft, trackBottom - trackTop);
      }
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



}
