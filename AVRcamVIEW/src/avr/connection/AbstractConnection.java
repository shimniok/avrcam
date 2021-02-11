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

import javax.swing.event.*;

import avr.connection.event.*;

/***********************************************************************
 * Defines a Connection that fires events describing if the connection
 * was opened or closed.
 */
public abstract class AbstractConnection implements Connection {

   /**
    * The list of registered event listeners.
    */
   private EventListenerList listenerList;

   /**
    * Boolean to test if the connection is connected (true) or
    * disconnected (false).
    */
   private boolean connected;

   /**
    * Initialize this object and set connected to false.
    */
   protected AbstractConnection() {
      listenerList = new EventListenerList();
      connected = false;
   }

   /**
    * Set connected to true and fire a connected event.
    */
   protected void setConnected() {
      setConnected(true);
      fireConnectedEvent(this);
   }

   /**
    * Set connected to false and fire a disconnected event.
    */
   protected void setDisconnected() {
      setConnected(false);
      fireDisconnectedEvent(this);
   }

   /**
    * Set this connected variable to the given value.
    * @param connected true if this connection is connected, false otherwise.
    */
   private void setConnected(boolean connected) {
      this.connected = connected;
   }

   /**
    * Tells whether or not this connection is connected.
    * @return true if, and only if, this connection is connected.
    */
   public boolean isConnected() {
      return connected;
   }

   /**
    * Adds a listener to the list that's notified each time a connect or
    * disconnect occurs.
    * @param cl the ConnectionListener
    */
   public void addConnectionListener(ConnectionListener cl) {
      listenerList.add(ConnectionListener.class, cl);
   }

   /**
    * Removes a listener to the list that's notified each time a connect or
    * disconnect occurs.
    * @param cl the ConnectionListener
    */
   public void removeConnectionListener(ConnectionListener cl) {
      listenerList.remove(ConnectionListener.class, cl);
   }

   public void removeAllConnectionListeners() {
      ConnectionListener[] listeners = getConnectionListeners();
      for(int i = 0; i < listeners.length; i++) {
         removeConnectionListener(listeners[i]);
      }
   }

   /**
    * Returns an array of all the connection listeners registered on
    * this connection.
    * @return all of this connection's ConnectionListeners or
    * an empty array if no connection listeners are currently registered
    */
   public ConnectionListener[] getConnectionListeners() {
      return (ConnectionListener[])
                listenerList.getListeners(ConnectionListener.class);
   }

   /**
    * Notifies all listeners that a connection has been established.
    * @param source This connection.
    */
   protected void fireConnectedEvent(Object source) {
      ConnectionListener[] listeners = getConnectionListeners();

      ConnectionEvent event = null;

      for(int i = 0; i < listeners.length; i++) {
         if(event == null) {
            event = new ConnectionEvent(this);
         }
         listeners[i].connected(event);
      }
   }

   /**
    * Notifies all listeners that a connection has been destroyed.
    * @param source This connection.
    */
   protected void fireDisconnectedEvent(Object source) {
      ConnectionListener[] listeners = getConnectionListeners();

      ConnectionEvent event = null;

      for(int i = 0; i < listeners.length; i++) {
         if(event == null) {
            event = new ConnectionEvent(this);
         }
         listeners[i].disconnected(event);
      }
   }

}
