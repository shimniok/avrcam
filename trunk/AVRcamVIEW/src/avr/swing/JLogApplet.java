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

import avr.lang.AVRSystem;
import avr.swing.filechooser.LogFileFilter;
import avr.swing.table.LogTableCellRenderer;
import avr.swing.table.LogTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;

public class JLogApplet extends JApplet {

   public static void main(String[] args) throws Exception {

      final JLogApplet log = new JLogApplet();

      // The JFileChooser takes about 2 seconds to fully load.
      // to keep the user from waiting, load the JFileChooser
      // in a background thread while the JLogFrame is loading.
      Thread loadFileChooser = new Thread(new Runnable() {
         public void run() {
            log.createJFileChooser();
         }
      });

      loadFileChooser.setName("Load File Chooser");
      loadFileChooser.start();

      JFrame frame = new JFrame("System Log");


      log.init();

      Dimension dim = log.getToolkit().getScreenSize();
      // set the log size to 3/4 the screen size and
      // center the log window to the middle of the screen.
      frame.setBounds(dim.width / 8, dim.height / 8,
                      dim.width * 3 / 4, dim.height * 3 / 4);

      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.getContentPane().add(log);
      frame.setVisible(true);

   }

   private static final LogFileFilter XML_FILE_FILTER;
   private static final LogFileFilter TXT_FILE_FILTER;
   private static final DateFormat DATE_FORMAT;

   static {

      DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss.SSS");
      XML_FILE_FILTER = new LogFileFilter("XML Format (*.xml)", ".xml");
      TXT_FILE_FILTER = new LogFileFilter("Text Format (*.txt)", ".txt");

   }

   private LogTableModel model;
   private boolean isStandAlone;
   private JFileChooser fileChooser;

   public String getParameter(String key) {
      return isStandAlone ? System.getProperty(key) : super.getParameter(key);
   }

   public void init() {
      init(true);
   }

   public void init(boolean isStandAlone) {
      this.isStandAlone = isStandAlone;

      String selectedLevel = AVRSystem.PREFS.get("avr.log.level", Level.CONFIG.getLocalizedName());
      boolean onlyShowLevel = AVRSystem.PREFS.getBoolean("avr.log.onlyshowlevel.selected", false);

      JPanel logLevelP = new JPanel();

      JRadioButton severeRB = new JRadioButton(Level.SEVERE.getLocalizedName(), Level.SEVERE.getLocalizedName().equals(selectedLevel));
      JRadioButton warningRB = new JRadioButton(Level.WARNING.getLocalizedName(), Level.WARNING.getLocalizedName().equals(selectedLevel));
      JRadioButton infoRB = new JRadioButton(Level.INFO.getLocalizedName(), Level.INFO.getLocalizedName().equals(selectedLevel));
      JRadioButton configRB = new JRadioButton(Level.CONFIG.getLocalizedName(), Level.CONFIG.getLocalizedName().equals(selectedLevel));
      JRadioButton fineRB = new JRadioButton(Level.FINE.getLocalizedName(), Level.FINE.getLocalizedName().equals(selectedLevel));
      JRadioButton finerRB = new JRadioButton(Level.FINER.getLocalizedName(), Level.FINER.getLocalizedName().equals(selectedLevel));
      JRadioButton finestRB = new JRadioButton(Level.FINEST.getLocalizedName(), Level.FINEST.getLocalizedName().equals(selectedLevel));

      JCheckBox onlyShowLevelCB = new JCheckBox("Show Only Level", onlyShowLevel);
      onlyShowLevelCB.addActionListener(new OnlyShowLevelAction());

      ButtonGroup bg = new ButtonGroup();
      bg.add(severeRB);
      bg.add(warningRB);
      bg.add(infoRB);
      bg.add(configRB);
      bg.add(fineRB);
      bg.add(finerRB);
      bg.add(finestRB);

      LogAction logAction = new LogAction();

      severeRB.addActionListener(logAction);
      warningRB.addActionListener(logAction);
      infoRB.addActionListener(logAction);
      configRB.addActionListener(logAction);
      fineRB.addActionListener(logAction);
      finerRB.addActionListener(logAction);
      finestRB.addActionListener(logAction);

      logLevelP.add(new JLabel("Level: "));
      logLevelP.add(severeRB);
      logLevelP.add(warningRB);
      logLevelP.add(infoRB);
      logLevelP.add(configRB);
      logLevelP.add(fineRB);
      logLevelP.add(finerRB);
      logLevelP.add(finestRB);
      logLevelP.add(Box.createHorizontalStrut(20));
      logLevelP.add(onlyShowLevelCB);

      getContentPane().add(logLevelP, BorderLayout.NORTH);

      model = new LogTableModel(Level.parse(selectedLevel));
      model.setOnlyShowSelectedLevel(onlyShowLevel);

      JTable table = new JTable(model);
      table.setFont(new Font("Courier New", Font.PLAIN, 12));

      table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
      table.setDefaultRenderer(String.class, new LogTableCellRenderer());
      table.setRowHeight(table.getRowHeight() + 5);
      table.getTableHeader().setReorderingAllowed(false);

      model.addTableModelListener(new LogModelHandler(table));

      TableColumn column = table.getColumnModel().getColumn(0);
      column.setPreferredWidth(200);
      column.setMaxWidth(200);

      column = table.getColumnModel().getColumn(1);
      column.setPreferredWidth(75);
      column.setMaxWidth(75);

      column = table.getColumnModel().getColumn(2);
      column.setPreferredWidth(800);

      JScrollPane tableScroll = new JScrollPane(table);
      tableScroll.getViewport().setBackground(table.getBackground());

      setJMenuBar(createMenuBar());

      getContentPane().add(tableScroll, BorderLayout.CENTER);

   }

   // overwritten so this applet can be resized using javascript
   public void setSize(int width, int height) {
      Dimension size = getSize();
      if(size.width != width || size.height != height) {
         super.setSize(width, height);
         validate();
      }
   }

   public JFrame createFrame() {
      JFrame frame = new JFrame("AVRcamVIEW - System Log");

      frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
      frame.getContentPane().add(this);

      return frame;
   }

   public JDialog createDialog(Frame owner) {

      JDialog dialog = new JDialog(owner,
                                   "AVRcamVIEW - System Log",
                                   false);

      dialog.getContentPane().add(this);

      return dialog;

   }

   private JMenuBar createMenuBar() {

      JMenuBar menubar = new JMenuBar();

      JMenu fileM = new JMenu("File");
      fileM.setMnemonic('f');

      if(isStandAlone) {
         // only allow user to open a log file if the log is running stand-alone
         fileM.add(new ProxyAction(this, "open",
                                   "Open",
                                   'o'));
      } else {
         // only allow the user to clear the log if the log is NOT running stand-alone
         fileM.add(new ProxyAction(this, "clear",
                                   "Clear",
                                   'c'));
         fileM.addSeparator();
         fileM.add(new ProxyAction(this, "save",
                                   "Save",
                                   's'));
      }

      menubar.add(fileM);

      return menubar;
   }

   public LogTableModel getTableModel() {
      return model;
   }

//   public void open() {
//
//      if(fileChooser == null) {
//         createJFileChooser();
//      }
//
//      int option = fileChooser.showOpenDialog(getRootPane());
//      if(option == JFileChooser.APPROVE_OPTION) {
//
//         clear();
//
//         try {
//
//            getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//            File logFile = fileChooser.getSelectedFile();
//
//            SAXBuilder builder = new SAXBuilder();
//            Document doc = builder.build(new GZIPInputStream(new FileInputStream(logFile)));
//
//            Element root = doc.getRootElement();
//
//            Iterator records = root.getChildren("record").iterator();
//            Element recordElement;
//            LogRecord record;
//
//            while(records.hasNext()) {
//
//               recordElement = (Element)records.next();
//
//               record = new LogRecord(Level.parse(recordElement.getChildText("level")), recordElement.getChildText("message"));
//               record.setMillis(Long.parseLong(recordElement.getChildText("millis")));
//               record.setSequenceNumber(Long.parseLong(recordElement.getChildText("sequence")));
//               record.setLoggerName(recordElement.getChildText("logger"));
//               record.setSourceClassName(recordElement.getChildText("class"));
//               record.setSourceMethodName(recordElement.getChildText("method"));
//               record.setThreadID(Integer.parseInt(recordElement.getChildText("thread")));
//
//               model.addRecord(record);
//
//            }
//
//
//         } catch(Exception e) {
//            e.printStackTrace(System.err);
//         }
//
//         getRootPane().setCursor(Cursor.getDefaultCursor());
//
//      }
//
//   }
//
   public void save() {

      if(model.getRowCount() == 0) {
         return;
      }

      if(fileChooser == null) {
         createJFileChooser();
      }

      int option = fileChooser.showSaveDialog(getRootPane());
      if(option == JFileChooser.APPROVE_OPTION) {
         try {
            File logFile = fileChooser.getSelectedFile();

            if(fileChooser.getFileFilter().equals(TXT_FILE_FILTER)) {
               if(!logFile.getName().endsWith(TXT_FILE_FILTER.getExtension())) {
                  logFile = new File(logFile.getAbsolutePath() + TXT_FILE_FILTER.getExtension());
               }

               saveTXT(logFile);

            } else if(fileChooser.getFileFilter().equals(XML_FILE_FILTER)) {
               if(!logFile.getName().endsWith(XML_FILE_FILTER.getExtension())) {
                  logFile = new File(logFile.getAbsolutePath() + XML_FILE_FILTER.getExtension());
               }

               saveXML(logFile);

            }

//            FileOutputStream outStream = new FileOutputStream(logFile);
//            ObjectOutputStream out = new ObjectOutputStream(outStream);
//
//            int rows = model.getRowCount();
//            out.writeInt(rows);
//
//            for(int i = 0; i < rows; i++) {
//               out.writeObject(model.getValueAt(i, 0));
//            }
//
//            out.close();

         } catch(Exception e) {
            e.printStackTrace(System.err);
         }
      }

   }

   private void saveTXT(File file) throws Exception {

      PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));

      for(int i = 0; i < model.getRecordCount(); i++) {
         LogRecord record = model.getRecord(i);
         writer.print(DATE_FORMAT.format(new Date(record.getMillis())));
         writer.print('\t');
         writer.print(record.getLevel().toString());
         writer.print('\t');
         writer.println(record.getMessage());
      }

      writer.close();

   }

   private void saveXML(File file) throws Exception {
       // mes 04/18/2012
       
       /*
      Element root = new Element("Log");

      for(int i = 0; i < model.getRecordCount(); i++) {
         LogRecord record = model.getRecord(i);

         Element recordE = new Element("Record");
         recordE.addContent(new Element("TimeStamp").addContent(DATE_FORMAT.format(new Date(record.getMillis()))));
         recordE.addContent(new Element("Level").addContent(record.getLevel().toString()));
         recordE.addContent(new Element("Message").addContent(record.getMessage()));

         root.addContent(recordE);
      }

      FileOutputStream writer = new FileOutputStream(file);

      new XMLOutputter().output(new Document(root), writer);

      writer.close();
*/
       
   }

   private void createJFileChooser() {

      getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      System.out.println("Loading File Chooser... " + Thread.currentThread().getName());

      if(fileChooser != null) {
         return;
      }

      fileChooser = new JFileChooser();
      fileChooser.removeChoosableFileFilter(fileChooser.getFileFilter());
      fileChooser.addChoosableFileFilter(TXT_FILE_FILTER);
      fileChooser.addChoosableFileFilter(XML_FILE_FILTER);

      fileChooser.setFileFilter(TXT_FILE_FILTER);

      System.out.println("File Chooser Loaded... " + Thread.currentThread().getName());
      getRootPane().setCursor(Cursor.getDefaultCursor());

   }

   public void clear() {
      model.clear();
   }

   private final class LogAction implements ActionListener {

      public void actionPerformed(ActionEvent ae) {
         String level = ae.getActionCommand();
         AVRSystem.PREFS.put("avr.log.level", level);
         model.setFilter(Level.parse(ae.getActionCommand()));
      }

   }

   private final class OnlyShowLevelAction implements ActionListener {

      public void actionPerformed(ActionEvent event) {
         boolean selected = ((JCheckBox)event.getSource()).isSelected();
         AVRSystem.PREFS.putBoolean("avr.log.onlyshowlevel.selected", selected);
         model.setOnlyShowSelectedLevel(selected);
      }

   }

   private final static class LogModelHandler implements TableModelListener {

      private JTable table;

      public LogModelHandler(JTable table) {
         this.table = table;
      }

      public void tableChanged(TableModelEvent tableModelEvent) {
         table.scrollRectToVisible(table.getCellRect(table.getRowCount() - 1, 0, true));
      }

   }

}
