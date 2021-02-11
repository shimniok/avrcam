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

package avr.util;

import java.util.logging.*;

import avr.swing.table.*;

/***********************************************************************
 * Defines a Handler for the Log to publish the log records to a
 * Table Model.
 */
public class LogHandler extends Handler {

   /**
    * The Table Model to publish the log records to.
    */
   private LogTableModel model;

   /**
    * Create a Log Handler that will publish the log records to a
    * Table Model
    * @param model The table model to publish the log records to.
    * @param level The initial log level this handler will log.
    */
   public LogHandler(LogTableModel model, Level level) {
      super();

      setLevel(level);

      this.model = model;
   }

   /**
    * Publish the given record to the table model.
    * @param record The log record to publish.
    */
   public synchronized void publish(LogRecord record) {

      if(!isLoggable(record)) {
         return;
      }

      // pass the record to the table.
      model.addRecord(record);

   }

   /**
    * Flush the contents of this Handler.  Nothing is done here since
    * the record is automatically displayed in the table.
    */
   public void flush() {
      // nothing needs to be done here
   }

   /**
    * Releases any resources taken up by this Handler.  Nothing is done here
    * since there are no resources allocated.
    * @throws java.lang.SecurityException
    */
   public void close() throws java.lang.SecurityException {
      // nothing needs to be done here
   }

}
