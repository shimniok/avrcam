package avr.swing;

import java.awt.*;
import javax.swing.*;

public abstract class JColorMapInterface extends JPanel {

   public JColorMapInterface(LayoutManager lm) {
      super(lm);
   }

   public abstract boolean isColumnClear(int column);
   public abstract void setColor(int index, int color);

}
