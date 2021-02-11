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

import java.util.logging.*;
import java.util.prefs.*;
import javax.swing.*;

import avr.device.*;

public class AVRSystem {

   public static final Resource RES;

   public static final Logger LOG;

   public static final String RELEASE_MINOR;
   public static final String RELEASE_MAJOR;

   public static final String BUILD_DATE;

   public static final Preferences PREFS;

   public static final JFileChooser FILE_CHOOSER;
   public static final String BAYER_FILE_EXT;
   public static final String TRACK_FILE_EXT;

   public static final int IMAGE_WIDTH;
   public static final int IMAGE_HEIGHT;

   public static final int NUM_INTENSITIES;

   public static final Device DEVICE;

   static {

      LOG = Logger.getLogger("AVRcamVIEW");
      LOG.setLevel(Level.ALL);

      PREFS = Preferences.userRoot().node("AVRcamVIEW");

      RES = new Resource("avr.resource.avr");

      FILE_CHOOSER = new JFileChooser(".");

      BAYER_FILE_EXT = RES.getString("file.ext.byr");
      TRACK_FILE_EXT = RES.getString("file.ext.trk");

      IMAGE_WIDTH = RES.getInt("image.width");
      IMAGE_HEIGHT = RES.getInt("image.height");

      NUM_INTENSITIES = RES.getInt("number.intensities");

      BUILD_DATE = RES.getString("build.date");
      RELEASE_MAJOR = RES.getString("build.major");
      RELEASE_MINOR = RES.getString("build.minor");

      DEVICE = new Device();

   }

   private AVRSystem() {
   }

}
