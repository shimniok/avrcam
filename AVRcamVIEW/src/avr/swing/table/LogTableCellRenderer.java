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

package avr.swing.table;

import java.awt.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

public class LogTableCellRenderer extends DefaultTableCellRenderer {

   private Color defaultBackground;
   private Color defaultForeground;

   private static final DateFormat DATE_FORMAT;

   static {

      DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss.SSS");

   }

   public LogTableCellRenderer() {
      super();
      defaultBackground = getBackground();
      defaultForeground = getForeground();
   }

   public Component getTableCellRendererComponent(JTable table, Object value,
      boolean isSelected, boolean hasFocus, int row, int column) {

      String text = null;

      switch(column) {
         case 0:
            text = DATE_FORMAT.format(new Date(((LogRecord)value).getMillis()));
            break;
         case 1:
            text = ((LogRecord)value).getLevel().toString();
            break;
         case 2:
            LogRecord record = (LogRecord)value;

            if(record.getMessage() == null) {
               text =  "No Message...";
            } else {
               text = record.getMessage();
            }

            if(!(text.startsWith("Sending") || text.startsWith("Received"))) {
               StringBuffer buffer = new StringBuffer(text);

               buffer.append(" ");
               if(record.getSourceClassName() != null) {
                  buffer.append(record.getSourceClassName());
               }

               if(record.getSourceMethodName() != null) {
                  buffer.append(":");
                  buffer.append(record.getSourceMethodName());
               }
               buffer.append(" ");

               text = buffer.toString();
            }

            break;
      }

      super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);

      if(!isSelected) {
         if(((LogRecord)value).getLevel().equals(Level.SEVERE)) {
            setBackground(Color.RED.darker());
            setForeground(Color.WHITE);
         } else if(((LogRecord)value).getLevel().equals(Level.WARNING)) {
            setBackground(Color.ORANGE);
            setForeground(Color.BLACK);
         } else if(((LogRecord)value).getLevel().equals(Level.INFO)) {
            setBackground(Color.YELLOW);
            setForeground(Color.BLACK);
         } else if(((LogRecord)value).getLevel().equals(Level.CONFIG)) {
            setBackground(Color.CYAN);
            setForeground(Color.BLACK);
         } else {
            setBackground(defaultBackground);
            setForeground(defaultForeground);
         }
      }

      if(column != 2) {
         setHorizontalAlignment(JLabel.CENTER);
      } else {
         setHorizontalAlignment(JLabel.LEFT);
      }

      return this;

   }
}
