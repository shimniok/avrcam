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

package avr.connection.event;

import java.util.*;

/***********************************************************************
 * This class defines methods that are invoked when a Connection is
 * either opened or closed.
 */
public interface ConnectionListener extends EventListener {

   /**
    * Invoked when a Connection is opened.
    * @param ce A ConnectionEvent encapsulating the event information.
    */
   public void connected(ConnectionEvent ce);

   /**
    * Invoked when a Connection is closed.
    * @param ce A ConnectionEvent encapsulating the event information.
    */
   public void disconnected(ConnectionEvent ce);

}
