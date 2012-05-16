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

import java.util.*;
import java.util.logging.*;
import javax.swing.table.*;

public class LogTableModel extends AbstractTableModel {

   public static final String[] COLUMN_NAMES = {
      "Time Stamp",
      "Level",
      "Message"
   };

   private List allRecords;
   private List filteredRecords;

   private Level level;
   private boolean selectedLabel;

   public LogTableModel(Level filterLevel) {
      this(filterLevel, false);
   }

   public LogTableModel(Level filterLevel, boolean selectedLabel) {
      super();
      allRecords = new ArrayList(10);
      filteredRecords = new ArrayList(10);
      this.level = filterLevel;
   }

   public void setFilter(Level level) {
      this.level = level;
      filteredRecords = new ArrayList(allRecords.size());
      Iterator i = allRecords.iterator();
      LogRecord record = null;
      while(i.hasNext()) {
         record = (LogRecord)i.next();
         if(selectedLabel) {
            if(level.intValue() == record.getLevel().intValue()) {
               filteredRecords.add(record);
            }
         } else {
            if(level.intValue() <= record.getLevel().intValue()) {
               filteredRecords.add(record);
            }
         }
      }
      fireTableDataChanged();
   }

   public void setOnlyShowSelectedLevel(boolean selectedLabel) {
      this.selectedLabel = selectedLabel;
      setFilter(level);
   }

   public void addRecord(LogRecord record) {
      allRecords.add(record);
      if(selectedLabel) {
         if(level.intValue() == record.getLevel().intValue()) {
            filteredRecords.add(record);
         }
      } else {
         if(level.intValue() <= record.getLevel().intValue()) {
            filteredRecords.add(record);
         }
      }
      this.fireTableRowsInserted(filteredRecords.size() - 1, filteredRecords.size() - 1);
   }

   public void clear() {
      allRecords.clear();
      filteredRecords.clear();
      fireTableDataChanged();
   }

   public int getRowCount() {
      return filteredRecords.size();
   }

   public int getColumnCount() {
      return COLUMN_NAMES.length;
   }

   public String getColumnName(int col) {
      return COLUMN_NAMES[col];
   }

   public Class getColumnClass(int col) {
      return String.class;
   }

   public Object getValueAt(int rowIndex, int columnIndex) {
      return filteredRecords.get(rowIndex);
   }

   public LogRecord getRecord(int row) {
      return (LogRecord)allRecords.get(row);
   }

   public int getRecordCount() {
      return allRecords.size();
   }

}
