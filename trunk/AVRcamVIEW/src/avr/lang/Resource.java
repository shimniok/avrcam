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

package avr.lang;

import java.util.*;

/***************************************************************
 * This is a convience class wrapped around a ResourceBundle.
 */
public class Resource {

   /**
    * The ResourceBundle to retrieve the key=value mappings.
    */
   private final ResourceBundle BUNDLE;

   /**
    * Create an instance of this resource bundle to retrieve the
    * key=value pairs from the given file.
    * @param file The file containing the key=value pairs.
    */
   public Resource(String file) {
      BUNDLE = ResourceBundle.getBundle(file);
   }

   /**
    * Gets a String for the given key from this resource bundle
    * @param key The key for the desired string
    * @return The string for the given key
    */
   public String getString(String key) {
      return BUNDLE.getString(key);
   }

   /**
    * Convience method to convert the returned value for the key into
    * an integer.
    * @param key The key for the desired integer
    * @return The integer for the given key.
    */
   public int getInt(String key) {
      return Integer.parseInt(getString(key));
   }

   /**
    * Convience method to convert the returned value for the key into
    * an character.
    * @param key The key for the desired character
    * @return The character for the given key.
    */
   public char getChar(String key) {
      return getString(key).charAt(0);
   }

   /**
    * Convience method to convert the returned value for the key into
    * an boolean.
    * @param key The key for the desired boolean
    * @return The boolean for the given key.
    */
   public boolean getBoolean(String key) {
      return new Boolean(getString(key)).booleanValue();
   }

   /**
    * Convience method to convert the returned value for the key into
    * an array of Strings.
    * @param key The key for the desired array of Strings
    * @return The array of Strings for the given key.
    */
   public String[] getStrings(String key) {
      // split the value string on every "," or ", "
      return getString(key).split(",\\s*");
   }

   /**
    * Convience method to convert the returned value for the key into
    * an array of Integers.
    * @param key The key for the desired array of Integers
    * @return The array of Integers for the given key.
    */
   public Integer[] getIntegers(String key) {

      String[] strValues = getStrings(key);

      Integer[] values = new Integer[strValues.length];

      for(int i = 0; i < strValues.length; i++) {
         values[i] = new Integer(strValues[i]);
      }

      return values;

   }

}
