/**
 *  Author: Rob Forbes Dec 2024
 */
package forbesflutes.wml.ui;

/** Transform coordinates in one window to another. Preserves the relative position
 *  of the point within the source window (optionally constrained by aspect ratio).
 *  Origin is defined as either the LL corner or center of the window.
 * Yes, I know I should really use theJava AffineTransform...
 * */
public class Transform {
   public enum Origin {LL, CENTER};
   private final Rectangle fromSpace;
   private final Rectangle toSpace;
   private final boolean invertY;
   private final double yHeight;
   // cached derived values:
   private final double xFactor;
   private final double yFactor;
   private final Pt fromOrigin;
   private final Pt toOrigin;

   public Transform(Rectangle fromSpace, Rectangle toSpace,
      Origin origin, boolean preserveAspect, boolean invertY, double yHeight) {
      this.fromSpace = fromSpace;
      this.toSpace = toSpace;
      this.invertY = invertY;
      this.yHeight = yHeight;
      double usableWidth;
      double usableHeight;
      fromOrigin = (origin == Origin.LL) ? fromSpace.getLL() : fromSpace.getCenter();
      toOrigin = (origin == Origin.LL) ? toSpace.getLL() : toSpace.getCenter();
      
      if (preserveAspect) {
         double fromAspect = fromSpace.getWidth() / fromSpace.getHeight();
         double toAspect = toSpace.getWidth() / toSpace.getHeight();
         if(fromAspect > toAspect) {
            // long and skinny, use full width of toSpace
            usableWidth = toSpace.getWidth();
            usableHeight = toSpace.getWidth() / fromAspect;
         }
         else {
            // short and fat; use full height of toSpace
            usableHeight = toSpace.getHeight();
            usableWidth = toSpace.getHeight() * fromAspect;
         }
      } else { 
         usableWidth = toSpace.getWidth();
         usableHeight = toSpace.getHeight();
      }
      xFactor = usableWidth / fromSpace.getWidth();
      yFactor = usableHeight / fromSpace.getHeight();
    }

   public Transform(Rectangle fromSpace, Rectangle toSpace,
         Origin origin, boolean preserveAspect) {
      this(fromSpace, toSpace, origin, preserveAspect, false, 0.);
   }
   
   public double transformXLength(double length) {
      return xFactor * length;
   }
   
   public double transformYLength(double length) {
      return yFactor * length;
   }
   
   public Pt transform(Pt fromPt) {
      if(!fromSpace.contains(fromPt))
         System.out.println("WARNING: Point not in source space.");
      double x = toOrigin.getX() + xFactor * (fromPt.getX() - fromOrigin.getX());
      double y = toOrigin.getY() + yFactor * (fromPt.getY() - fromOrigin.getY());
      if(invertY)
         y = yHeight - y;
      Pt toPt = new Pt(x, y);
      // sanity check:
      if(!toSpace.contains(toPt))
         System.out.println("WARNING:Transformed point not in target space.");
      return toPt;
   }
   
}
