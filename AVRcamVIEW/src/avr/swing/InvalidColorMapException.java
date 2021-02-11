package avr.swing;

public class InvalidColorMapException extends Exception {

   private int[] indicies;
   private int red;
   private int green;
   private int blue;

   public InvalidColorMapException(String message, int[] indicies, int red, int green, int blue) {
      super(message);
      this.indicies = indicies;
      this.red = red;
      this.green = green;
      this.blue = blue;
   }

   public int[] getIndicies() {
      return indicies;
   }

   public int getRed() {
      return red;
   }

   public int getGreen() {
      return green;
   }

   public int getBlue() {
      return blue;
   }

}
