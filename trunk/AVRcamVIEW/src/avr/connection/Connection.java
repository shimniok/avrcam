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

package avr.connection;

import java.io.*;
import java.nio.channels.*;

/***********************************************************************
 * Defines methods for some type of I/O Connection.
 */
public interface Connection {

   /**
    * Connects this connection.
    * @throws IOException If the connection could not be connected.
    */
   public void connect() throws Exception;

   /**
    * Disconnects this connection.
    * @throws IOException If the connection could not be disconnected.
    */
   public void disconnect() throws IOException;

   /**
    * Returns the Object to which this connection is wrapped around.
    * @return The Object to which this connection is wrapped around.
    */
   public Object getConnectionObject();

   /**
    * Tells whether or not this connection is connected.
    * @return true if, and only if, this connection is connected.
    */
   public boolean isConnected();

   /**
    * Returns the InputStream associated with this connection.
    * @return The InputStream associated with this connection.
    * @throws IOException If the InputStream could not be returned.
    */
   public InputStream getInputStream() throws IOException;

   /**
    * Returns the OutputStream associated with this connection.
    * @return The OutputStream associated with this connection.
    * @throws IOException If the OutputStream could not be returned.
    */
   public OutputStream getOutputStream() throws IOException;

   /**
    * Returns the ByteChannel associated with this connection.
    * @return The ByteChannel associated with this connection.
    */
   public ByteChannel getChannel();

}
