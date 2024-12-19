package forbesflutes.wml.ui;

import java.awt.geom.Point2D;

/** A 2D point/vector. */
public class Pt  extends Point2D.Double {
   
   public static final Pt origin = new Pt(0., 0.);
   public static final Pt NORTH = new Pt(0., 1.);
   public static final Pt SOUTH = new Pt(0., -1.);
   public static final Pt EAST = new Pt(1., 0.);
   public static final Pt WEST = new Pt(-1., 0.);
   public static final Pt NW = new Pt(-1., 1.).normalize();
   public static final Pt NE = new Pt(1., 1.).normalize();
   public static final Pt SW = new Pt(-1., -1.).normalize();
   public static final Pt SE = new Pt(1., -1.).normalize();
   
   public static final double radiansPerDegree = 2. * Math.PI / 360.;
   
   public Pt(double x, double y) {
      this.x = x;
      this.y = y;
   }
   
   public double getX() {return x;}
   
   public double getY() {return y;}
   
   public Pt() {this(0,0);}
   
   /** Add other pt to this. */
   public Pt add(Pt other) {
      return new Pt(x + other.x, y + other.y);
   }
   
   /** Subtract other pt from this. */
   public Pt subtract(Pt other) {
      return new Pt(x - other.x, y - other.y);
   }

   /** Return with length scaled. */
   public Pt scale(double d) {
      return new Pt(x * d, y * d);
   }

   /** Return vector with same direction but length 1. */
   public Pt normalize() {
      double length = Math.sqrt(x * x + y * y);
      return new Pt(x / length, y/length);
   }
   
   /** Return pt reflected around a line parallel to Y axis) . */
   public Pt reflectInX(double lineX) {
      return new Pt(2. * lineX - x, y);
   }

   /** Return pt reflected around a line parallel to the X axis. */
   public Pt reflectInY(double lineY) {
      return new Pt(x, 2. * lineY - y);
   }
   
   /** Return a pt in polar coords, angle 0 is at 3 o'clock */
   public static Pt polarPt(double deg, double len) {
      return new Pt(
         len * Math.cos(radiansPerDegree * deg), 
         len * Math.sin(radiansPerDegree * deg));
   }
   
};
