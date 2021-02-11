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

import java.awt.event.*;
import java.lang.reflect.*;
import javax.swing.*;

/**
 * Utility class for defining Actions that can be attached as an ActionListener.
 * It also provides many convience methods for setting the Icon, Keystroke,
 * and Mnemonic.
 */
public class ProxyAction extends AbstractAction {

   /**
    * The object to which the action is to be invoked upon.
    */
   private Object target;

   /**
    * The method of the target to call when the action is invoked.
    */
   private Method method;

   /**
    * Create a Proxy Action.
    * @param target The object to which the action is to be invoked upon.
    * @param methodName The method to call.
    * @param icon The icon to display on the control this action is attached to.
    * in the object "target".
    */
   public ProxyAction(Object target,
                      String methodName,
                      Icon icon) {
      this(target, methodName, null, icon);
   }

   /**
    * Create a Proxy Action.
    * @param target The object to which the action is to be invoked upon.
    * @param methodName The method to call.
    * @param name The text string to display on the control this action is
    * attached to.
    * in the object "target".
    */
   public ProxyAction(Object target,
                      String methodName,
                      String name) {
      this(target, methodName, name, null);
   }

   /**
    * Create a Proxy Action.
    * @param target The object to which the action is to be invoked upon.
    * @param methodName The method to call.
    * @param name The text string to display on the control this action is
    * attached to.
    * @param key The mnemonic key used to invoke this action. (Alt + key)
    * in the object "target".
    */
   public ProxyAction(Object target,
                      String methodName,
                      String name,
                      int key) {
      this(target, methodName, name, key, null);
   }

   /**
    * Create a Proxy Action.
    * @param target The object to which the action is to be invoked upon.
    * @param methodName The method to call.
    * @param name The text string to display on the control this action is
    * attached to.
    * @param icon The icon to display on the control this action is attached to.
    * in the object "target".
    */
   public ProxyAction(Object target,
                      String methodName,
                      String name,
                      Icon icon) {
      this(target, methodName, false, name, -1, icon);
   }

   /**
    * Create a Proxy Action.
    * @param target The object to which the action is to be invoked upon.
    * @param methodName The method to call.
    * @param name The text string to display on the control this action is
    * attached to.
    * @param key The mnemonic key used to invoke this action. (Alt + key)
    * @param icon The icon to display on the control this action is attached to.
    * in the object "target".
    */
   public ProxyAction(Object target,
                      String methodName,
                      String name,
                      int key,
                      Icon icon) {
      this(target, methodName, false, name, key, icon);
   }

   /**
    * Create a Proxy Action.
    * @param target The object to which the action is to be invoked upon.
    * @param methodName The method to call.
    * @param passEvent True if the ActionEvent is to be passed to the method
    * defined by "methodName"
    * @param name The text string to display on the control this action is
    * attached to.
    */
   public ProxyAction(Object target,
                      String methodName,
                      boolean passEvent,
                      String name) {
      this(target, methodName, passEvent, name, -1, null);
   }

   /**
    * Create a Proxy Action.
    * @param target The object to which the action is to be invoked upon.
    * @param methodName The method to call.
    * @param passEvent True if the ActionEvent is to be passed to the method
    * defined by "methodName"
    * @param name The text string to display on the control this action is
    * attached to.
    * @param key The mnemonic key used to invoke this action. (Alt + key)
    * in the object "target".
    */
   public ProxyAction(Object target,
                      String methodName,
                      boolean passEvent,
                      String name,
                      int key) {
      this(target, methodName, passEvent, name, key, null);
   }

   /**
    * Create a Proxy Action.
    * @param target The object to which the action is to be invoked upon.
    * @param methodName The method to call.
    * @param passEvent True if the ActionEvent is to be passed to the method
    * defined by "methodName"
    * @param name The text string to display on the control this action is
    * attached to.
    * @param key The mnemonic key used to invoke this action. (Alt + key)
    * @param icon The icon to display on the control this action is attached to.
    * in the object "target".
    */
   public ProxyAction(Object target,
                      String methodName,
                      boolean passEvent,
                      String name,
                      int key,
                      Icon icon) {
      super(name);
//      this(target,
//           target.getClass().getMethod(methodName,
//                                       (passEvent) ? new Class[] { ActionEvent.class }
//                                                   : null),
//           name,
//           key,
//           icon);

      Method method = null;

      try {
         method = target.getClass().getMethod(methodName,
                                              (passEvent) ? new Class[] {ActionEvent.class}
                                              : null);
      } catch(NoSuchMethodException ex) {
         throw new NoSuchMethodError(ex.getMessage());
      }

      if(method.getParameterTypes().length > 1) {
         throw new IllegalArgumentException(
            "Method can have only one ActionEvent argument.");

      } else if(method.getParameterTypes().length == 1) {
         if(!method.getParameterTypes()[0].getName().equals(
               ActionEvent.class.getName())) {

            throw new IllegalArgumentException(
               method.getParameterTypes()[0].getName() + " != " +
               ActionEvent.class.getName() +
               " Parameter must be an ActionEvent.");
         }
      }

      this.target = target;
      this.method = method;

      if(key != -1) {
         setMnemonic(key);
      }

      if(icon != null) {
         setIcon(icon);
      }

   }

   /**
    * Set the mnemonic key to be the given key.
    * @param key The key identifier used to set the Mnemonic to
    * @see KeyEvent
    */
   public void setMnemonic(int key) {
      putValue(MNEMONIC_KEY, new Integer(key));
   }

   /**
    * Set the Shortcut KeyStroke to attach to this action.
    * @param keyCode The key identifier to use.
    * @param modifiers Any modifiers that need to be used along with the key.
    * @see KeyStroke
    * @see KeyEvent
    */
   public void setKeyStroke(int keyCode, int modifiers) {
      setKeyStroke(KeyStroke.getKeyStroke(keyCode, modifiers));
   }

   /**
    * Set the Shortcut KeyStroke to attach to this action.
    * @param keystroke The KeyStroke to use
    */
   public void setKeyStroke(KeyStroke keystroke) {
      putValue(ACCELERATOR_KEY, keystroke);
   }

   /**
    * Set the Icon of this Action
    * @param icon The icon to use.
    */
   public void setIcon(Icon icon) {
      putValue(SMALL_ICON, icon);

   }

   /**
    * Sets the Tool Tip Text for this Action
    *
    * @param text String
    */
   public void setToolTipText(String text) {
      putValue(SHORT_DESCRIPTION, text);
   }

   /**
    * This method is called when an Action Event occurs on the control this
    * action is attached to.
    * @param ae The ActionEvent created.
    */
   public void actionPerformed(ActionEvent ae) {

      try {
         if(method.getParameterTypes().length == 0) {
            // invoke the given method on the given target with no parameters
            method.invoke(target, null);
         } else {
            // invoke the given method on the given target with the ActionEvent
            // as a parameter
            method.invoke(target, new Object[] {ae});
         }
      } catch(Exception e) {
         // should never happen
         e.printStackTrace(System.err);
      }

   }

}
