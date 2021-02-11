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

import avr.connection.SerialConnection;
import avr.connection.SerialParams;
import avr.connection.event.ConnectionEvent;
import avr.connection.event.ConnectionListener;
import avr.device.event.DataAdapter;
import avr.device.event.DataListener;
import avr.lang.AVRSystem;
import avr.swing.filechooser.LogFileFilter;
import avr.util.LogHandler;
import gnu.io.UnsupportedCommOperationException;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyVetoException;
import java.io.*;
import java.lang.reflect.Constructor;
import java.util.prefs.BackingStoreException;
import javax.swing.*;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

public class JAVRCamFrame extends JFrame {

   public static void main(String[] args) {
      /*
      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch(Exception e) {
      }
      */

      try {
         if(args.length == 1) {
            if(args[0].equals("-r")) {
               AVRSystem.PREFS.clear();
            }
         }
      } catch(BackingStoreException bse) {
         System.out.println("Could not clear preferences: " + bse.getMessage());
      }
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            JFrame frame = new JAVRCamFrame();
            frame.setVisible(true);
         }
      });
   }

   private static final String COLOR_MAP_BOUNDS_KEY  = "avr.colormap.bounds";
   private static final String COLOR_MAP_SHOWING_KEY = "avr.colormap.showing";
   private static final String COM_PORT_KEY          = "avr.com.port";
   private static final String COM_PARAMS_KEY        = "avr.com.params";
   private static final String DEVICE_CONNECTED_KEY  = "avr.device.connected";
   private static final String FRAME_BOUNDS_KEY      = "avr.frame.bounds";
   private static final String LOG_BOUNDS_KEY        = "avr.log.bounds";
   private static final String LOG_SHOWING_KEY       = "avr.log.showing";
   private static final String MESSAGE_SHOWING_KEY   = "avr.message.showing";

   private JFrame logF;

   private JSerialPanel serialP;
   private JRegisterPanel registersP;
   private JMessagePanel messageP;
   private JColorMapInterface colorMapP;
   private JFrame colorMapF;

   private JMenu windowM;

   private JDesktopPane desktop;
   private JTrackingInternalFrame trackingF;

   private CaptureInternalFrameHandler captureInternalFrameHandler;

   private AbstractButton viewLogB;
   private AbstractButton viewMessagesB;
   private AbstractButton viewColorMapB;
   private AbstractButton trackingB;
   private AbstractButton passiveTrackingB;

   private ProxyAction connectAction;
   private ProxyAction disconnectAction;
   private ProxyAction serialParamsAction;
   private ProxyAction setRegistersAction;
   private ProxyAction pingAction;
   private ProxyAction resetAction;
   private ProxyAction captureAction;
   private ProxyAction trackingAction;
   private ProxyAction passiveTrackingAction;

   private ProxyAction cascadeAction;
   private ProxyAction tileHorizontalAction;
   private ProxyAction tileVerticalAction;
   private ProxyAction resetAllAction;
   private ProxyAction closeAllAction;

   private ButtonGroup windowBG;

   public JAVRCamFrame() {
      super("AVRcamVIEW");

      createActions();
      windowBG = new ButtonGroup();
      captureInternalFrameHandler = new CaptureInternalFrameHandler();

//      ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("avr/resource/AVRcam.gif"));
//      if(icon != null) {
//         this.setIconImage(icon.getImage());
//      }

      serialP = new JSerialPanel();
      registersP = new JRegisterPanel();

      messageP = new JMessagePanel();

      String classVersionString = System.getProperty("java.class.version","44.0");
      int classVersion = (int)(Double.parseDouble(classVersionString) * 10.0);

      // JDK 5.0 class version is 49.0.  Since the JNewColorMapPanel requires JDK 5.0
      // this will check to make sure that the JVM is 5.0 or better, Otherwise, use
      // the old color map.
      if(System.getProperty("avr.old.color.map") != null || classVersion < 490) {
         colorMapP = new JColorMapPanel(messageP);
      } else {
         try {
            // must load the class using reflection so that the 1.4 compiler does not try
            // to compile the JNewColorMapPanel class
            Constructor constructor = Class.forName("avr.swing.JNewColorMapPanel").getConstructor(new Class[] { JMessagePanel.class });
            colorMapP = (JColorMapInterface)constructor.newInstance(new Object[] {messageP});
//         colorMapP = new JNewColorMapPanel(messageP);
         } catch(Exception ex) {
            // we can't load the new color map, so default to the old one.
            colorMapP = new JColorMapPanel(messageP);
            ex.printStackTrace(System.err);
         }
      }

      messageP.addComponentListener(new MessagePanelHandler());

      Dimension screen = getToolkit().getScreenSize();
      Rectangle bounds = null;

      JLogApplet logA = new JLogApplet();
      logA.init(false);
      AVRSystem.LOG.addHandler(new LogHandler(logA.getTableModel(), AVRSystem.LOG.getLevel()));

      logF = logA.createFrame();
      bounds = getBounds(LOG_BOUNDS_KEY);
      if(bounds == null) {
         bounds = new Rectangle(0, 0, screen.width * 3 / 4, screen.height / 2);
      }
      logF.setBounds(bounds);


      desktop = new JDesktopPane();
      desktop.setBackground(new Color(0x005C5C));
//      desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);

      desktop.addMouseListener(new PopupHandler());

      setJMenuBar(createMenuBar());
      getContentPane().add(createToolBar(), BorderLayout.NORTH);
      getContentPane().add(desktop, BorderLayout.CENTER);
      getContentPane().add(messageP, BorderLayout.EAST);

      bounds = getBounds(FRAME_BOUNDS_KEY);
      if(bounds == null) {
         bounds = new Rectangle(screen.width / 8, screen.height / 8,
                                 screen.width * 3 / 4, screen.height * 3 / 4);
      }
      setBounds(bounds);

      colorMapF = new JFrame("AVRcamVIEW - Color Map");
      colorMapF.addWindowListener(new ColorMapWindowHandler(colorMapF, viewColorMapB));
      colorMapF.getContentPane().add(colorMapP, BorderLayout.CENTER);
      colorMapF.pack();
      colorMapF.setResizable(false);

      bounds = getBounds(COLOR_MAP_BOUNDS_KEY);
      if(bounds == null) {
         Dimension dim = colorMapF.getSize();
         bounds = new Rectangle((screen.width - dim.width) / 2,
                                (screen.height - dim.height) / 2,
                                dim.width, dim.height);
      }

      colorMapF.setLocation(bounds.x, bounds.y);
      colorMapF.pack();

      addWindowListener(new WindowHandler(this));
      logF.addWindowListener(new LogWindowHandler(viewLogB));

      Action action = new ProxyAction(AVRSystem.DEVICE, "simulateNCK", "Simulate NCK");
      registerKeyStrokeAction(
         KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK),
         action);

      action = new ProxyAction(AVRSystem.DEVICE, "simulateACK", "Simulate ACK");
      registerKeyStrokeAction(
         KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK),
         action);

      resetAction = new ProxyAction(this, "reset", "Reset");
//         registerKeyStrokeAction(
//            KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK),
//            resetAction);

      SwingUtilities.invokeLater(new Startup());

   }

   private void registerKeyStrokeAction(KeyStroke keyStroke, Action action) {
      // attach the action to the Ctrl+A key
      getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                   .put(keyStroke, action.getValue(Action.NAME));
      getRootPane().getActionMap().put(action.getValue(Action.NAME), action);
   }

   private void createActions() {
      serialParamsAction = new ProxyAction(this, "showSerialDialog", "Configure Serial", 'S');
      setRegistersAction = new ProxyAction(this, "showRegisterDialog", "Set Registers", 'R');

      connectAction = new ProxyAction(this, "connect", "Connect", 'C');
      disconnectAction = new ProxyAction(this, "disconnect", "Disconnect", 'D');

      pingAction = new ProxyAction(this, "ping", "Ping", 'P');
      pingAction.setEnabled(false);

//      resetAction = new ProxyAction(this, "reset", "Reset", 'R');

      captureAction = new ProxyAction(this, "capture", "Capture", 'C');
      trackingAction = new ProxyAction(this, "tracking", "Enable Tracking", 'E');

      passiveTrackingAction = new ProxyAction(this, "passiveTracking", "Enable Passive Tracking", 'V');

      cascadeAction = new ProxyAction(this, "cascadeWindows", "Cascade", 'C');
      tileHorizontalAction = new ProxyAction(this, "tileHorizontal", "Tile Horizontal", 'H');
      tileVerticalAction = new ProxyAction(this, "tileVertical", "Tile Vertical", 'V');
      resetAllAction = new ProxyAction(this, "resetAllWindows", "Reset All", 'R');
      closeAllAction = new ProxyAction(this, "closeAllWindows", "Close All", 'L');

//      resetAction.setEnabled(false);
      setRegistersAction.setEnabled(false);
      captureAction.setEnabled(false);
      disconnectAction.setEnabled(false);
      trackingAction.setEnabled(false);
      passiveTrackingAction.setEnabled(false);

      trackingB = new JToggleButton(trackingAction);
      passiveTrackingB = new JToggleButton(passiveTrackingAction);

      serialParamsAction.setToolTipText("Modify the Serial Port Parameters");
      setRegistersAction.setToolTipText("Modify the AVRcam Register values");
      connectAction.setToolTipText("Open the Serial Connection to the AVRcam");
      disconnectAction.setToolTipText("Close the Serial Connection to the AVRcam");
      pingAction.setToolTipText("Send a Ping ccommand to the AVRcam");
      captureAction.setToolTipText("Capture a image from the AVRcam");
      trackingAction.setToolTipText("Command the AVRcam to start tracking");
      passiveTrackingAction.setToolTipText("Receive tracking packets from the AVRcam");

   }

   private JMenuBar createMenuBar() {
      JMenuBar menubar = new JMenuBar();

      JMenu fileM = new JMenu("File");
      JMenu viewM = new JMenu("View");
      JMenu deviceM = new JMenu("Device");
      JMenu helpM = new JMenu("Help");

      windowM = new JMenu("Window");

      fileM.setMnemonic('f');
      viewM.setMnemonic('v');
      deviceM.setMnemonic('d');
      windowM.setMnemonic('w');
      helpM.setMnemonic('h');

      fileM.add(new ProxyAction(this, "openBayer", "Open Bayer Image", 'B'));
      fileM.add(new ProxyAction(this, "openTracking", "Open Tracking Data", 'T'));
      fileM.addSeparator();
      fileM.add(serialParamsAction);
      fileM.add(setRegistersAction);
      fileM.addSeparator();
      fileM.add(new ProxyAction(this, "close", "Exit", 'X'));

      viewColorMapB = new JCheckBoxMenuItem(new ProxyAction(this, "viewColorMap", true, "Show Color Map", 'C'));
      viewM.add(viewColorMapB);

      viewMessagesB = new JCheckBoxMenuItem(new ProxyAction(this, "viewMessages", true, "Show Messages", 'M'));
      viewM.add(viewMessagesB);

      viewLogB = new JCheckBoxMenuItem(new ProxyAction(this, "viewLog", true, "Show Log", 'L'));
      viewM.add(viewLogB);

      deviceM.add(connectAction);
      deviceM.add(disconnectAction);

      windowM.add(cascadeAction);
      windowM.add(tileHorizontalAction);
      windowM.add(tileVerticalAction);
      windowM.add(resetAllAction);
      windowM.add(closeAllAction);
      windowM.addSeparator();

      helpM.add(new ProxyAction(this, "about", "About", 'A'));

      menubar.add(fileM);
      menubar.add(viewM);
      menubar.add(deviceM);
      menubar.add(windowM);
      menubar.add(helpM);

      return menubar;
   }

   private JToolBar createToolBar() {
      JToolBar toolbar = new JToolBar();

      toolbar.add(connectAction);
      toolbar.add(disconnectAction);
      toolbar.addSeparator();
      toolbar.add(serialParamsAction);
      toolbar.add(setRegistersAction);
      toolbar.addSeparator();
      toolbar.add(pingAction);
//      toolbar.add(resetAction);
      toolbar.add(captureAction);
      toolbar.add(trackingB);
      toolbar.add(passiveTrackingB);

      toolbar.setFloatable(false);

      return toolbar;
   }

   private static Rectangle getBounds(String key) {

      byte[] boundsArray = AVRSystem.PREFS.getByteArray(key, null);
      Rectangle bounds = null;

      if(boundsArray != null) {

         try {
            ObjectInputStream in = new ObjectInputStream(
               new ByteArrayInputStream(boundsArray));

            bounds = (Rectangle)in.readObject();

            in.close();
         } catch(ClassNotFoundException ex) {
         } catch(IOException ex) {
         }
      }

      return bounds;

   }

   protected JRootPane createRootPane() {
      // create a new JRootPane
      JRootPane rootPane = new JRootPane() {

         /**
          * Create a new Glass Pane Glass Pane that when visible it captures
          * all the mouse events, plays a "beep" when the mouse is clicked
          * when the Glass Pane is visible, and changes the mouse depending
          * on if the Glass Pane is visible or not.
          * @return The Glass Pane
          */
         protected Component createGlassPane() {
            JComponent c = new JPanel() {

               /**
                * Overridden to change the cursor to a busy cursor when visible
                * and a default cursor when not visible.
                * @param visible True to show the Glass Pane, false to not
                * show the Glass Pane.
                */
               public void setVisible(boolean visible) {
                  super.setVisible(visible);
                  if(visible) {
                     // this is possible if it this panel has not been added
                     // to a JRootPane yet
                     if(getRootPane() != null) {
                        // change the cursor to a wait/busy cursor (hour glass)
                        getRootPane().setCursor(
                           Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        AVRSystem.LOG.finest("Glass Pane SHOWING... ");
                     }
                  } else {
                     // this is possible if it this panel has not been added
                     // to a JRootPane yet
                     if(getRootPane() != null) {
                        // chage the cursor back to the default cursor
                        getRootPane().setCursor(Cursor.getDefaultCursor());
                        AVRSystem.LOG.finest("Glass Pane HIDDEN... ");
                     }
                  }
               }
            };
            c.setName(this.getName() + ".glassPane");
            c.setVisible(false);
            ((JPanel)c).setOpaque(false);
            // add a mouse listener to capture all the mouse events and play
            // a "beep" when the mouse is pressed.
            c.addMouseListener(new MouseAdapter() {
               public void mousePressed(MouseEvent me) {
                  Toolkit.getDefaultToolkit().beep();
               }
            });
            return c;
         }
      };
      rootPane.setOpaque(true);
      return rootPane;
   }

   public void openBayer() {

      javax.swing.filechooser.FileFilter[] filters = AVRSystem.FILE_CHOOSER.getChoosableFileFilters();
      for(int i = 0; i < filters.length; i++) {
         AVRSystem.FILE_CHOOSER.removeChoosableFileFilter(filters[i]);
      }

      AVRSystem.FILE_CHOOSER.addChoosableFileFilter(new LogFileFilter("Bayer Image File (*.byr)", ".byr"));

      int option = AVRSystem.FILE_CHOOSER.showOpenDialog(getRootPane());
      if(option == JFileChooser.APPROVE_OPTION) {
         JInternalFrame iFrame = new JCaptureInternalFrame(messageP, colorMapP, AVRSystem.FILE_CHOOSER.getSelectedFile());
         iFrame.pack();
         iFrame.setLocation(10, 10);
         desktop.add(iFrame);
         iFrame.setVisible(true);
      }
   }

   public void openTracking() {

      javax.swing.filechooser.FileFilter[] filters = AVRSystem.FILE_CHOOSER.getChoosableFileFilters();
      for(int i = 0; i < filters.length; i++) {
         AVRSystem.FILE_CHOOSER.removeChoosableFileFilter(filters[i]);
      }

      AVRSystem.FILE_CHOOSER.addChoosableFileFilter(new LogFileFilter("AVR Tracking File (*.trk)", ".trk"));

      int option = AVRSystem.FILE_CHOOSER.showOpenDialog(getRootPane());
      if(option == JFileChooser.APPROVE_OPTION) {
         try {
            JInternalFrame iFrame = new JTrackingInternalFrame(AVRSystem.FILE_CHOOSER.getSelectedFile());
            iFrame.pack();
            iFrame.setLocation(10, 10);
            desktop.add(iFrame);
            iFrame.setVisible(true);
         } catch(FileNotFoundException fnfe) {
            JOptionPane.showMessageDialog(getRootPane(), "File Not Found", fnfe.getMessage(), JOptionPane.ERROR_MESSAGE);
         } catch(IOException ioe) {
            JOptionPane.showMessageDialog(getRootPane(), "I/O Exception", ioe.getMessage(), JOptionPane.ERROR_MESSAGE);
         }
      }
   }

   public void showSerialDialog() {
      if(AVRSystem.DEVICE.getConnection() != null) {
         showSerialDialog(((SerialConnection)AVRSystem.DEVICE.getConnection()).getSerialParams());
      } else {
         showSerialDialog(null);
      }
   }

   public void showSerialDialog(SerialParams params) {
      int option = serialP.showDialog(this, params);
      if(option == JSerialPanel.OK_OPTION) {
         try {
            if(AVRSystem.DEVICE.isConnected()) {
               ((SerialConnection)AVRSystem.DEVICE.getConnection()).setSerialParams(serialP.getSerialParameters());
            }
            saveConnectionPrefs(null, serialP.getSerialParameters());
         } catch(UnsupportedCommOperationException ucoe) {
            AVRSystem.LOG.severe(ucoe.getMessage());
         }
      }
   }

   public void showRegisterDialog() {
      int option = registersP.showDialog(this);
      if(option == JRegisterPanel.OK_OPTION && registersP.getRegisters().size() > 0) {

         DataListener handler = null;
         try {
            handler = new SetRegistersHandler();
            AVRSystem.DEVICE.addDataListener(handler);
            getRootPane().getGlassPane().setVisible(true);
            AVRSystem.DEVICE.sendSetRegisters(registersP.getRegisters());
            messageP.append("Set Registers");
         } catch(IOException ioe) {
            AVRSystem.DEVICE.removeDataListener(handler);
            getRootPane().getGlassPane().setVisible(false);
            AVRSystem.LOG.severe(ioe.getMessage());
            messageP.append("Set Registers not sent");
         }
      }
   }

   public void close() {

      AVRSystem.PREFS.putBoolean(DEVICE_CONNECTED_KEY, AVRSystem.DEVICE.isConnected());

      if(AVRSystem.DEVICE.isConnected()) {
         saveConnectionPrefs((SerialConnection)AVRSystem.DEVICE.getConnection());
         disconnect();
      }

      AVRSystem.PREFS.putBoolean(LOG_SHOWING_KEY, logF.isVisible());
      AVRSystem.PREFS.putBoolean(MESSAGE_SHOWING_KEY, messageP.isVisible());
      AVRSystem.PREFS.putBoolean(COLOR_MAP_SHOWING_KEY, colorMapF.isVisible());

      saveBounds(FRAME_BOUNDS_KEY, getBounds());
      saveBounds(COLOR_MAP_BOUNDS_KEY, colorMapF.getBounds());
      saveBounds(LOG_BOUNDS_KEY, logF.getBounds());

      System.exit(0);
   }

   private static void saveConnectionPrefs(SerialConnection con) {
      saveConnectionPrefs(con.getComPort(), con.getSerialParams());
   }

   private static void saveConnectionPrefs(String comPort, SerialParams params) {
      if(comPort != null) {
         AVRSystem.PREFS.put(COM_PORT_KEY, comPort);
      }
      try {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         ObjectOutputStream oOut = new ObjectOutputStream(out);

         oOut.writeObject(params);
         oOut.close();

         byte[] serialParams = out.toByteArray();

         AVRSystem.PREFS.putByteArray(COM_PARAMS_KEY, serialParams);
      } catch(IOException ioe) {
         ioe.printStackTrace();
         AVRSystem.LOG.warning("Could not save serial parameters: " + ioe.getMessage());
      }
   }

   private static void saveBounds(String key, Rectangle rect) {

      try {
         ByteArrayOutputStream arrayOut = new ByteArrayOutputStream();
         ObjectOutputStream out = new ObjectOutputStream(arrayOut);

         out.writeObject(rect);

         out.close();

         AVRSystem.PREFS.putByteArray(key, arrayOut.toByteArray());
      } catch(IOException ex) {
      }

   }

   public void viewColorMap(ActionEvent ae) {
      colorMapF.setVisible(((AbstractButton)ae.getSource()).isSelected());
   }

   public void viewLog(ActionEvent ae) {
      logF.setVisible(((AbstractButton)ae.getSource()).isSelected());
   }

   public void viewMessages(ActionEvent ae) {
      messageP.setVisible(((AbstractButton)ae.getSource()).isSelected());
   }

   public void connect() {
      String[] ports = SerialConnection.getSerialPorts();
      if(ports.length > 0) {
         Object option = JOptionPane.showInputDialog(this, "Select a COM Port:", "Serial Port",
                                                  JOptionPane.QUESTION_MESSAGE,
                                                  null,
                                                  ports,
                                                  AVRSystem.PREFS.get(COM_PORT_KEY, ports[0]));
         if(option != null) {
            connect(option.toString(), serialP.getSerialParameters());
         }
      } else {
         JOptionPane.showMessageDialog(this, "No Serial Ports Available", "No Serial Ports",
                                       JOptionPane.ERROR_MESSAGE);
      }
   }

   public void connect(String comPort, SerialParams params) {

      SerialConnection con = new SerialConnection(comPort, params);
      AVRSystem.DEVICE.setConnection(con);
      AVRSystem.DEVICE.addConnectionListener(new ConnectionHandler());
      try {
         AVRSystem.DEVICE.connect();
      } catch(Exception ioe) {
         JOptionPane.showMessageDialog(getRootPane(),
                                       ioe.getMessage(),
                                       "Connect Error",
                                       JOptionPane.ERROR_MESSAGE);
         AVRSystem.LOG.severe(ioe.getMessage());
      }
   }

   public void disconnect() {
      AVRSystem.DEVICE.disconnect();
   }

   public void ping() {
      DataListener handler = null;
      try {
         handler = new PingHandler();
         AVRSystem.DEVICE.addDataListener(handler);
         getRootPane().getGlassPane().setVisible(true);
         AVRSystem.DEVICE.sendPing();
         messageP.append("Ping");
      } catch(IOException ioe) {
         AVRSystem.DEVICE.removeDataListener(handler);
         getRootPane().getGlassPane().setVisible(false);
         AVRSystem.LOG.severe(ioe.getMessage());
         messageP.append("Ping not sent: " + ioe.getMessage());
      }
   }

   public void reset() {
      DataListener handler = null;
      try {
         handler = new ResetHandler();
         AVRSystem.DEVICE.addDataListener(handler);
         getRootPane().getGlassPane().setVisible(true);
         AVRSystem.DEVICE.sendReset();
         messageP.append("Reset");
      } catch(IOException ioe) {
         AVRSystem.DEVICE.removeDataListener(handler);
         getRootPane().getGlassPane().setVisible(false);
         AVRSystem.LOG.severe(ioe.getMessage());
         messageP.append("Reset not sent");
      }
   }

   public void capture() {
      JCaptureInternalFrame captureF = new JCaptureInternalFrame(messageP, colorMapP);
      Insets insets = captureF.getInsets();
      captureF.pack();
      int frameCount = desktop.getAllFrames().length;
      captureF.setLocation(10 * frameCount, 10 * frameCount);

      captureF.addInternalFrameListener(captureInternalFrameHandler);

      desktop.add(captureF);

      captureF.setVisible(true);

   }

   public void tracking() {
      if(trackingB.isSelected()) {
         enableTracking();
      } else {
         disableTracking();
      }
   }

   private void enableTracking() {
      DataListener handler = new TrackingHandler();
      try {
         AVRSystem.DEVICE.addDataListener(handler);
         AVRSystem.DEVICE.sendEnableTracking();
         messageP.append("Enable Tracking");
      } catch(IOException ioe) {
         AVRSystem.DEVICE.removeDataListener(handler);
         AVRSystem.LOG.severe(ioe.getMessage());
         messageP.append("Enable Tracking not sent");
      }
   }

   private void disableTracking() {
      DataListener handler = new TrackingHandler();
      try {
         AVRSystem.DEVICE.addDataListener(handler);
         AVRSystem.DEVICE.sendDisableTracking();
         messageP.append("Disable Tracking");
      } catch(IOException ioe) {
         AVRSystem.DEVICE.removeDataListener(handler);
         AVRSystem.LOG.severe(ioe.getMessage());
         messageP.append("Disable Tracking not sent");
      }
   }

   public void passiveTracking() {

      if(passiveTrackingB.isSelected()) {
         passiveTrackingB.setText("Disable Passive Tracking");
         disconnectAction.setEnabled(false);
         serialParamsAction.setEnabled(false);
         setRegistersAction.setEnabled(false);
         pingAction.setEnabled(false);
         resetAction.setEnabled(false);
         captureAction.setEnabled(false);
         trackingAction.setEnabled(false);

         trackingF = new JTrackingInternalFrame();
         Insets insets = trackingF.getInsets();
         trackingF.pack();
         trackingF.setLocation(10, 10);

         desktop.add(trackingF);

         trackingF.setVisible(true);
         trackingF.startTracking();
         messageP.append("Enable Passive Tracking");
      } else {
         passiveTrackingB.setText("Enable Passive Tracking");
         disconnectAction.setEnabled(true);
         serialParamsAction.setEnabled(true);
         setRegistersAction.setEnabled(true);
         pingAction.setEnabled(true);
         resetAction.setEnabled(true);
         captureAction.setEnabled(true);
         trackingAction.setEnabled(true);

         trackingF.stopTracking();
         desktop.getDesktopManager().closeFrame(trackingF);

         messageP.append("Disable Passive Tracking");
      }
   }

   public void activateWindow(ActionEvent event) {

      JInternalFrame[] frames = desktop.getAllFrames();
      for(int i = 0; i < frames.length; i++) {
         if(frames[i].getTitle().equals(event.getActionCommand())) {
            desktop.getDesktopManager().activateFrame(frames[i]);
            desktop.setSelectedFrame(frames[i]);
            frames[i].toFront();
            try {
               frames[i].setSelected(true);
            } catch(PropertyVetoException ex) {
               ex.printStackTrace(System.out);
               AVRSystem.LOG.severe(ex.getMessage());
            }
            break;
         }
      }

   }

   public void cascadeWindows() {

      int x = 0;
      int y = 0;

      JInternalFrame[] frames = desktop.getAllFrames();
      for(int i = 0; i < frames.length; i++) {
         frames[i].setLocation(x, y);
         x += 30;
         y += 30;
      }

   }

   public void tileHorizontal() {

      JInternalFrame[] frames = desktop.getAllFrames();

      int frameCount = frames.length;
      int nrows = 1;
      int ncols = (frameCount + nrows - 1) / nrows;

      if(frameCount == 0) {
         return;
      }

      int w = desktop.getWidth();
      int h = desktop.getHeight();
      w = (w - (ncols - 1)) / ncols;
      h = (h - (nrows - 1)) / nrows;

      for(int c = 0, x = 0; c < ncols; c++, x += w) {
         for(int r = 0, y = 0; r < nrows; r++, y += h) {
            int i = r * ncols + c;
            if(i < frameCount) {
               frames[i].setBounds(x, y, w, h);
            }
         }
      }

   }

   public void tileVertical() {

      JInternalFrame[] frames = desktop.getAllFrames();

      int frameCount = frames.length;
      int ncols = 1;
      int nrows = (frameCount + ncols - 1) / ncols;;

      if(frameCount == 0) {
         return;
      }

      int w = desktop.getWidth();
      int h = desktop.getHeight();
      w = (w - (ncols - 1)) / ncols;
      h = (h - (nrows - 1)) / nrows;

      for(int c = 0, x = 0; c < ncols; c++, x += w) {
         for(int r = 0, y = 0; r < nrows; r++, y += h) {
            int i = r * ncols + c;
            if(i < frameCount) {
               frames[i].setBounds(x, y, w, h);
            }
         }
      }

   }

   public void resetAllWindows() {

      JInternalFrame[] frames = desktop.getAllFrames();
      for(int i = 0; i < frames.length; i++) {
         frames[i].pack();
      }

   }

   public void closeAllWindows() {

      JInternalFrame[] frames = desktop.getAllFrames();
      for(int i = 0; i < frames.length; i++) {
         if(frames[i] != trackingF) {
            desktop.getDesktopManager().closeFrame(frames[i]);
         }
      }

   }

   public void about() {
      new JAboutDialog(this, messageP).showDialog();
   }

   private final class SetRegistersHandler extends DataAdapter {

      public void ack() {
         AVRSystem.DEVICE.removeDataListener(this);
         getRootPane().getGlassPane().setVisible(false);
      }

      public void nck() {
         AVRSystem.DEVICE.removeDataListener(this);
         getRootPane().getGlassPane().setVisible(false);
         JOptionPane.showMessageDialog(JAVRCamFrame.this, "Set Register NCK Received", "NCK Received", JOptionPane.ERROR_MESSAGE);
      }

      public void responseTimerExpired() {
         AVRSystem.DEVICE.removeDataListener(this);
         getRootPane().getGlassPane().setVisible(false);
         JOptionPane.showMessageDialog(JAVRCamFrame.this, "Response Timer Expired", "Timer Expired", JOptionPane.ERROR_MESSAGE);
      }

   }

   private final class CaptureInternalFrameHandler implements InternalFrameListener {
      public void internalFrameOpened(InternalFrameEvent e) {

         String title = e.getInternalFrame().getTitle();

         JCheckBoxMenuItem windowMI = new JCheckBoxMenuItem(new ProxyAction(JAVRCamFrame.this, "activateWindow", true, title));
         windowMI.setActionCommand(title);

         windowBG.add(windowMI);

         windowMI.setSelected(true);

         windowM.add(windowMI);
      }

      public void internalFrameClosing(InternalFrameEvent e) {
      }

      public void internalFrameClosed(InternalFrameEvent e) {

         String text = null;
         String title = e.getInternalFrame().getTitle();

         for(int i = 0; i < windowM.getItemCount(); i++) {
            // need to check for the separator
            if(windowM.getItem(i) != null) {
               text = windowM.getItem(i).getText();
               if((text != null) && (title != null) &&
                  (text.equals(title))) {
                  windowM.remove(i);
                  break;
               }
            }
         }
      }

      public void internalFrameIconified(InternalFrameEvent e) {
      }

      public void internalFrameDeiconified(InternalFrameEvent e) {
      }

      public void internalFrameActivated(InternalFrameEvent e) {
      }

      public void internalFrameDeactivated(InternalFrameEvent e) {
      }

   }

   private final class PingHandler extends DataAdapter {

      public void ack() {
         AVRSystem.DEVICE.removeDataListener(this);
         getRootPane().getGlassPane().setVisible(false);
      }

      public void nck() {
         AVRSystem.DEVICE.removeDataListener(this);
         getRootPane().getGlassPane().setVisible(false);
         JOptionPane.showMessageDialog(JAVRCamFrame.this, "Ping NCK Received", "NCK Received", JOptionPane.ERROR_MESSAGE);
      }

      public void responseTimerExpired() {
         AVRSystem.DEVICE.removeDataListener(this);
         getRootPane().getGlassPane().setVisible(false);
         JOptionPane.showMessageDialog(JAVRCamFrame.this, "Response Timer Expired", "Timer Expired", JOptionPane.ERROR_MESSAGE);
      }

   }

   private final class ResetHandler extends DataAdapter {

      public void ack() {
         AVRSystem.DEVICE.removeDataListener(this);
         getRootPane().getGlassPane().setVisible(false);
      }

      public void nck() {
         AVRSystem.DEVICE.removeDataListener(this);
         getRootPane().getGlassPane().setVisible(false);
         JOptionPane.showMessageDialog(JAVRCamFrame.this, "Reset NCK Received", "NCK Received", JOptionPane.ERROR_MESSAGE);
      }

      public void responseTimerExpired() {
         AVRSystem.DEVICE.removeDataListener(this);
         getRootPane().getGlassPane().setVisible(false);
         JOptionPane.showMessageDialog(JAVRCamFrame.this, "Response Timer Expired", "Timer Expired", JOptionPane.ERROR_MESSAGE);
      }

   }

   private final class TrackingHandler extends DataAdapter {
      public void ack() {
         AVRSystem.DEVICE.removeDataListener(this);
         if(trackingB.isSelected()) {
            trackingB.setText("Disable Tracking");
            disconnectAction.setEnabled(false);
            serialParamsAction.setEnabled(false);
            setRegistersAction.setEnabled(false);
            pingAction.setEnabled(false);
            resetAction.setEnabled(false);
            captureAction.setEnabled(false);
            passiveTrackingAction.setEnabled(false);

            trackingF = new JTrackingInternalFrame();
            Insets insets = trackingF.getInsets();
            trackingF.pack();
            trackingF.setLocation(10, 10);

            desktop.add(trackingF);

            trackingF.setVisible(true);
            trackingF.startTracking();

         } else {
            trackingB.setText("Enable Tracking");
            disconnectAction.setEnabled(true);
            serialParamsAction.setEnabled(true);
            setRegistersAction.setEnabled(true);
            pingAction.setEnabled(true);
            resetAction.setEnabled(true);
            captureAction.setEnabled(true);
            passiveTrackingAction.setEnabled(true);

            trackingF.stopTracking();
            desktop.getDesktopManager().closeFrame(trackingF);
         }
      }

      public void nck() {
         AVRSystem.DEVICE.removeDataListener(this);
         if(trackingB.isSelected()) {
            trackingB.setSelected(false);
         } else {
            trackingB.setSelected(true);
         }
         JOptionPane.showMessageDialog(JAVRCamFrame.this, "Disable Tracking NCK Received", "NCK Received", JOptionPane.ERROR_MESSAGE);
      }

      public void responseTimerExpired() {
         AVRSystem.DEVICE.removeDataListener(this);
         getRootPane().getGlassPane().setVisible(false);
         if(trackingB.isSelected()) {
            trackingB.setSelected(false);
         } else {
            trackingB.setSelected(true);
         }
         JOptionPane.showMessageDialog(JAVRCamFrame.this, "Response Timer Expired", "Timer Expired", JOptionPane.ERROR_MESSAGE);

      }

   }

   private final class Startup implements Runnable {

      public void run() {

         boolean logVisible = AVRSystem.PREFS.getBoolean(LOG_SHOWING_KEY, false);
         logF.setVisible(logVisible);
         viewLogB.setSelected(logVisible);

         boolean messagesVisible = AVRSystem.PREFS.getBoolean(MESSAGE_SHOWING_KEY, true);
         messageP.setVisible(messagesVisible);
         viewMessagesB.setSelected(messagesVisible);

         boolean colorMapVisible = AVRSystem.PREFS.getBoolean(COLOR_MAP_SHOWING_KEY, false);
         viewColorMapB.setSelected(colorMapVisible);
         if(colorMapVisible) {
            viewColorMap(new ActionEvent(viewColorMapB, ActionEvent.ACTION_PERFORMED, viewColorMapB.getText()));
         }

         try {

            String comPort = AVRSystem.PREFS.get(COM_PORT_KEY, "COM1");
            byte[] serialParams = AVRSystem.PREFS.getByteArray(COM_PARAMS_KEY, null);

            AVRSystem.LOG.config("Prefs Com Port: " + comPort);
            SerialParams params = null;
            if(serialParams != null) {
               ObjectInputStream in = new ObjectInputStream(
                                         new ByteArrayInputStream(serialParams));
               params = (SerialParams)in.readObject();
               in.close();

               AVRSystem.LOG.config("Prefs Com Params: " + params);

               if(AVRSystem.PREFS.getBoolean(DEVICE_CONNECTED_KEY, false)) {
                  connect(comPort, params);
               }
            }
         } catch(Exception e) {
            e.printStackTrace();
            AVRSystem.LOG.warning("Could not read serial params: " + e.getMessage());
         }

      }

   }

   private final class ConnectionHandler extends DataAdapter implements ConnectionListener {

      public void connected(ConnectionEvent ce) {

         SerialConnection con = (SerialConnection)ce.getSource();

         saveConnectionPrefs(con.getComPort(), con.getSerialParams());

         try {
            AVRSystem.DEVICE.addDataListener(this);
            getRootPane().getGlassPane().setVisible(true);
            AVRSystem.DEVICE.sendPing();
            messageP.append("Ping");
         } catch(IOException ioe) {
            AVRSystem.DEVICE.removeDataListener(this);
            getRootPane().getGlassPane().setVisible(false);
            AVRSystem.LOG.severe(ioe.getMessage());
            messageP.append("Ping not sent: " + ioe.getMessage());
         }
      }

      public void ack() {
         AVRSystem.DEVICE.removeDataListener(this);
         getRootPane().getGlassPane().setVisible(false);

         enableButtons(true);
      }

      public void nck() {
         AVRSystem.DEVICE.removeDataListener(this);
         getRootPane().getGlassPane().setVisible(false);
         enableButtons(true);

         JOptionPane.showMessageDialog(JAVRCamFrame.this, "Ping NCK Received", "NCK Received", JOptionPane.ERROR_MESSAGE);
      }

      public void responseTimerExpired() {
         AVRSystem.DEVICE.removeDataListener(this);
         getRootPane().getGlassPane().setVisible(false);
         disconnect();
         JOptionPane.showMessageDialog(JAVRCamFrame.this, "Response Timer Expired: Please connect the serial port to the computer and make sure the AVRcam is powered.", "Timer Expired", JOptionPane.ERROR_MESSAGE);
      }

      public void disconnected(ConnectionEvent ce) {
         AVRSystem.DEVICE.removeConnectionListener(this);

         enableButtons(false);
      }

      private void enableButtons(boolean enabled) {
         connectAction.setEnabled(!enabled);
         disconnectAction.setEnabled(enabled);
         setRegistersAction.setEnabled(enabled);
         pingAction.setEnabled(enabled);
         resetAction.setEnabled(enabled);
         captureAction.setEnabled(enabled);
         trackingAction.setEnabled(enabled);
         passiveTrackingAction.setEnabled(enabled);
      }

   }

   private final class MessagePanelHandler extends ComponentAdapter {

      public void componentHidden(ComponentEvent e) {
         viewMessagesB.setSelected(false);
      }

   }

   private final class PopupHandler extends MouseAdapter {

      private JPopupMenu popup;

      public void mouseReleased(MouseEvent event) {
         if(SwingUtilities.isRightMouseButton(event)) {
            if(popup == null) {
               popup = new JPopupMenu();
               popup.add(cascadeAction);
               popup.add(tileHorizontalAction);
               popup.add(tileVerticalAction);
               popup.add(resetAllAction);
               popup.add(closeAllAction);
               popup.setInvoker(event.getComponent());
            }
            popup.show(event.getComponent(), event.getX(), event.getY());
         }
      }

   }

   private final static class ColorMapWindowHandler extends WindowAdapter {

      private JFrame frame;
      private AbstractButton viewB;

      public ColorMapWindowHandler(JFrame frame, AbstractButton viewB) {
         this.frame = frame;
         this.viewB = viewB;
      }

      public void windowClosed(WindowEvent we) {
         viewB.setSelected(false);
      }

      public void windowClosing(WindowEvent we) {
         frame.dispose();
      }
   }

   private final static class WindowHandler extends WindowAdapter {

      private JAVRCamFrame frame;

      public WindowHandler(JAVRCamFrame frame) {
         this.frame = frame;
      }

      public void windowClosing(WindowEvent we) {
         frame.dispose();
      }

      public void windowClosed(WindowEvent we) {
         frame.close();
      }

   }

   private final static class LogWindowHandler extends WindowAdapter {

      private AbstractButton button;

      public LogWindowHandler(AbstractButton button) {
         this.button = button;
      }

      public void windowClosing(WindowEvent we) {
         button.setSelected(false);
      }

   }

}
