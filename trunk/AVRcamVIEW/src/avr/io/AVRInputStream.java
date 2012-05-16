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

package avr.io;

import java.io.*;

public class AVRInputStream extends FilterInputStream {

   private static final int NEW_LINE = '\r';
   private static final int END_DUMP_DATA = 0x0F;
   private static final int END_TRACK_DATA = 0xFF;

   private static final int SEARCHING_STATE = 0x00;
   private static final int DUMP_FRAME_STATE = 0x01;
   private static final int ACCEPTING_STATE = 0x02;

   public AVRInputStream(InputStream in) {
      super(in);
   }

   public int read(byte[] data, int off, int len) throws IOException {

      int terminator = NEW_LINE;
      int state = SEARCHING_STATE;

      int value = -1;
      int bytesRead = 0;
      boolean cont = false;

      do {

         value = read();

         if(value != -1) {

            switch(state) {
               case SEARCHING_STATE:
                  //System.out.println("(" + Integer.toHexString(value & 0xFF) + ") ");
                  data[off] = (byte)value;
                  if(value == 0x0A) {
                     terminator = END_TRACK_DATA;
                     state = ACCEPTING_STATE;
                     bytesRead++;
                  } else if(value == 0x0B) {
                     terminator = END_DUMP_DATA;
                     state = DUMP_FRAME_STATE;
                     bytesRead++;
                  } else if(value == 'A' || value == 'N') {
                     terminator = NEW_LINE;
                     state = ACCEPTING_STATE;
                     bytesRead++;
                  }
                  cont = false;
                  break;
               case DUMP_FRAME_STATE:
                  data[off + bytesRead++] = (byte)value;
                  state = ACCEPTING_STATE;
                  cont = true;
                  break;
               case ACCEPTING_STATE:
                  //System.out.println(Integer.toHexString(value & 0xFF) + " ");
                  data[off + bytesRead++] = (byte)value;
                  cont = false;
                  break;
            }

         }


      } while(cont || value != terminator);

      return bytesRead;
   }

}
