package forbesflutes.wml.ui;

import java.awt.geom.Rectangle2D;

public class Rectangle  extends Rectangle2D.Double {
   
   public Rectangle(double x, double y, double width, double height) {
      super(x, y, width, height);
   }
   
   public Pt getCenter() {
      return new Pt(getCenterX(), getCenterY());
   }
   
   public double getMinX() {
      return x;
   }

   public double getMinY() {
      return y;
   }

   public double getMaxX() {
      return x + width;
   }

   public double getMaxY() {
      return y + height;
   }

   public Pt getLL() {return new Pt(getMinX(), getMinY());}
   
   public Pt getLR() {return new Pt(getMaxX(), getMinY());}
   
   public Pt getUL() {return new Pt(getMinX(), getMaxY());}
   
   public Pt getUR() {return new Pt(getMaxX(), getMaxY());}
   
}
