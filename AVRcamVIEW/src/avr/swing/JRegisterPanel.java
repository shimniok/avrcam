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
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

public class JRegisterPanel extends JPanel {

   public static void main(String[] args) throws Exception {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      int option = new JRegisterPanel().showDialog(null);
      System.out.println(option);
   }

   private static final String WARNING_TEXT = "WARNING!!! Changing the registers may cause the AVRcam to no longer respond.  If this happens, simply power cycle the AVRcam.";
   private static final int UNKNOWN_OPTION = 0x00;
   public static final int OK_OPTION = 0x01;
   public static final int CANCEL_OPTION = 0x02;

   private JDialog dialog;
   private JRegister[] registers;
   private int option;

   private JTabbedPane tabs;

   private JRadioButton enableAutoWhiteBalanceRB;
   private JRadioButton disableAutoWhiteBalanceRB;
   private JRadioButton enableAutoAdjustModeRB;
   private JRadioButton disableAutoAdjustModeRB;
   private JRadioButton enableFlourescentLightFilterRB;
   private JRadioButton disableFlourescentLightFilterRB;

   public JRegisterPanel() {
      super(new BorderLayout());

      tabs = new JTabbedPane();

      enableAutoWhiteBalanceRB = new JRadioButton("Enable", true);
      disableAutoWhiteBalanceRB = new JRadioButton("Disable");
      enableAutoAdjustModeRB = new JRadioButton("Enable");
      disableAutoAdjustModeRB = new JRadioButton("Disable", true);
      enableFlourescentLightFilterRB = new JRadioButton("Enable");
      disableFlourescentLightFilterRB = new JRadioButton("Disable", true);

      ButtonGroup autoWhiteBalanceBG = new ButtonGroup();
      autoWhiteBalanceBG.add(enableAutoWhiteBalanceRB);
      autoWhiteBalanceBG.add(disableAutoWhiteBalanceRB);

      ButtonGroup autoAdjustModeBG = new ButtonGroup();
      autoAdjustModeBG.add(enableAutoAdjustModeRB);
      autoAdjustModeBG.add(disableAutoAdjustModeRB);

      ButtonGroup flourescentLightFilterBG = new ButtonGroup();
      flourescentLightFilterBG.add(enableFlourescentLightFilterRB);
      flourescentLightFilterBG.add(disableFlourescentLightFilterRB);

//      Box generalP = new Box(BoxLayout.Y_AXIS);
      JPanel generalP = new JPanel();
      generalP.setLayout(new BoxLayout(generalP, BoxLayout.Y_AXIS));

      Border emptyBorder = new EmptyBorder(5, 5, 5, 5);

      Box autoWhiteBalanceBox = new Box(BoxLayout.X_AXIS);
      autoWhiteBalanceBox.setBorder(emptyBorder);
      autoWhiteBalanceBox.add(new JLabel("Auto White Balance:"));
      autoWhiteBalanceBox.add(Box.createHorizontalGlue());
      autoWhiteBalanceBox.add(enableAutoWhiteBalanceRB);
      autoWhiteBalanceBox.add(disableAutoWhiteBalanceRB);

      Box autoAdjustModeBox = new Box(BoxLayout.X_AXIS);
      autoAdjustModeBox.setBorder(emptyBorder);
      autoAdjustModeBox.add(new JLabel("Auto Adjust Mode:"));
      autoAdjustModeBox.add(Box.createHorizontalGlue());
      autoAdjustModeBox.add(enableAutoAdjustModeRB);
      autoAdjustModeBox.add(disableAutoAdjustModeRB);

      Box flourescentLightFilterBox = new Box(BoxLayout.X_AXIS);
      flourescentLightFilterBox.setBorder(emptyBorder);
      flourescentLightFilterBox.add(new JLabel("Flourescent Light Filter:"));
      flourescentLightFilterBox.add(Box.createHorizontalGlue());
      flourescentLightFilterBox.add(enableFlourescentLightFilterRB);
      flourescentLightFilterBox.add(disableFlourescentLightFilterRB);

      generalP.add(autoWhiteBalanceBox);
      generalP.add(autoAdjustModeBox);
      generalP.add(flourescentLightFilterBox);

      JPanel advancedP = new JPanel();
      advancedP.setLayout(new BoxLayout(advancedP, BoxLayout.Y_AXIS));

      JTextArea warningTA = new JTextArea(WARNING_TEXT);
      warningTA.setEditable(false);
      warningTA.setWrapStyleWord(true);
      warningTA.setLineWrap(true);
      warningTA.setForeground(Color.RED);
      warningTA.setRows(4);
      warningTA.setFont(warningTA.getFont().deriveFont(16F));
      warningTA.setBackground(advancedP.getBackground());
      warningTA.setBorder(new EmptyBorder(0, 10, 0, 10));

      registers = new JRegister[8];

      for(int i = 0; i < registers.length; i++) {
         registers[i] = new JRegister();
         advancedP.add(registers[i]);
         advancedP.add(Box.createVerticalStrut(5));
      }
      advancedP.add(warningTA);

      tabs.addTab("General", generalP);
      tabs.addTab("Advanced", advancedP);

      add(tabs, BorderLayout.CENTER);

   }

   public int showDialog(Frame owner) {
      reset();

      option = UNKNOWN_OPTION;
      if(dialog == null) {
         dialog = new JDialog(owner, "AVRcamVIEW - Set Registers", true);

         dialog.getContentPane().add(this, BorderLayout.CENTER);
         dialog.getContentPane().add(createButtonPanel(), BorderLayout.SOUTH);
         dialog.pack();
         dialog.setResizable(false);
      }

      dialog.setLocationRelativeTo(owner);
      dialog.setVisible(true);

      return option;

   }

   public void reset() {
      for(int i = 0; i < registers.length; i++) {
         registers[i].reset();
      }
   }

   public Map getRegisters() {
      Map info = new HashMap();

      if(tabs.getSelectedIndex() == 0) {

         if(enableAutoWhiteBalanceRB.isSelected()) {
            info.put(new Integer(0x12), new Integer(0x2C));
         } else {
            info.put(new Integer(0x12), new Integer(0x28));
         }

         if(enableAutoAdjustModeRB.isSelected()) {
            info.put(new Integer(0x13), new Integer(0x01));
         } else {
            info.put(new Integer(0x13), new Integer(0x00));
         }

         if(enableFlourescentLightFilterRB.isSelected()) {
            info.put(new Integer(0x2D), new Integer(0x07));
         } else {
            info.put(new Integer(0x2D), new Integer(0x03));
         }

      } else {
         for(int i = 0; i < registers.length; i++) {
         JRegister r = registers[i];
            if(r.isChecked()) {
               info.put(r.getRegister(), r.getValue());
            }
         }
      }

      return Collections.unmodifiableMap(info);
   }

   public void ok() {
      option = OK_OPTION;
      dialog.setVisible(false);
   }

   public void cancel() {
      option = CANCEL_OPTION;
      dialog.setVisible(false);
   }

   private JComponent createButtonPanel() {

      JPanel buttonP = new JPanel(new FlowLayout(FlowLayout.RIGHT));

      buttonP.setBorder(new EtchedBorder());

      buttonP.add(new JButton(new ProxyAction(this, "ok", "OK", 'o')));
      buttonP.add(new JButton(new ProxyAction(this, "cancel", "Cancel", 'c')));

      return buttonP;
   }

   private static final class JRegister extends JPanel {

      private JCheckBox enableCB;
      private JSpinner registerS;
      private JSpinner valueS;

      public JRegister() {
         super(new FlowLayout(FlowLayout.CENTER));

         // remove the default insets of the JPanel
         setBorder(new EmptyBorder(-5, -5, -5, -5));

         enableCB = new JCheckBox(new ProxyAction(this, "setEnabled", "Register"));

         registerS = new JSpinner(new SpinnerNumberModel(0, 0, 0x90, 1));
         valueS = new JSpinner(new SpinnerNumberModel(0, 0, 0xFF, 1));

         reset();

         add(enableCB);
         add(registerS);
         add(new JLabel(" = "));
         add(valueS);
      }

      public void reset() {
         enableCB.setSelected(false);
         registerS.setEnabled(false);
         valueS.setEnabled(false);
         registerS.setValue(new Integer(0));
         valueS.setValue(new Integer(0));
      }

      public boolean isChecked() {
         return enableCB.isSelected();
      }

      public String getRegister() {
         return registerS.getValue().toString();
      }

      public String getValue() {
         return valueS.getValue().toString();
      }

      public void setEnabled() {
         boolean enabled = enableCB.isSelected();
         registerS.setEnabled(enabled);
         valueS.setEnabled(enabled);
      }

   }


}
