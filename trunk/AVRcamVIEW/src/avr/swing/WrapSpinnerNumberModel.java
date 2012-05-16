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

import javax.swing.SpinnerNumberModel;

public class WrapSpinnerNumberModel extends SpinnerNumberModel {

   public WrapSpinnerNumberModel(Number value, Comparable minimum,
                                 Comparable maximum, Number stepSize) {
      super(value, minimum, maximum, stepSize);
   }

   public WrapSpinnerNumberModel(int value, int minimum,
                                 int maximum, int stepSize) {
      super(new Integer(value), new Integer(minimum),
            new Integer(maximum), new Integer(stepSize));
   }

   public Object getNextValue() {
      return increment(+1);
   }

   public Object getPreviousValue() {
      return increment(-1);
   }

   private Number increment(int dir) {
      Number newValue;
      if ((getValue() instanceof Float) || (getValue() instanceof Double)) {
         double v = ((Number)getValue()).doubleValue() +
            (((Number)getStepSize()).doubleValue() * (double)dir);
         if (getValue() instanceof Double) {
            newValue = new Double(v);
         } else {
            newValue = new Float(v);
         }
      } else {
         long v = ((Number)getValue()).longValue() +
                  (((Number)getStepSize()).longValue() * (long)dir);

         if (getValue() instanceof Long) {
            newValue = new Long(v);
         } else if (getValue() instanceof Integer) {
            newValue = new Integer((int)v);
         } else if (getValue() instanceof Short) {
            newValue = new Short((short)v);
         } else {
            newValue = new Byte((byte)v);
         }
      }

      if ((getMaximum() != null) && (((Comparable)getMaximum()).compareTo(newValue) < 0)) {
         return (Number)getMinimum();
      }

      if ((getMinimum() != null) && (((Comparable)getMinimum()).compareTo(newValue) > 0)) {
         return (Number)getMaximum();
      }

      return newValue;
   }

}

