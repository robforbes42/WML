package forbesflutes.wml.ui;

import javax.swing.*;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;

/** A generic panel supporting drawing in an XY space where coords are doubles and Y+ is upwards. */
public class GraphicsPanel extends JPanel {
   private static final long serialVersionUID = 1L;
   public static enum TextPlacement {TOP, BOTTOM, LEFT, RIGHT, CENTER};
   private boolean preserveAspectRatio;
   // Region bounds are set dynamically
   protected Rectangle panelBounds; // current pixel size of the panel
   protected Rectangle paperBounds; // Required, Size of imaginary piece of paper
   // Transforms between regions, set in response to region changes
   Transform paperToPanel;
   private int pixelMargin = 10;
   protected Graphics2D graphics;
   final static float[] dashLengths = {10.0f};
   final public static BasicStroke dashedStroke =
         new BasicStroke(1.0f,
                         BasicStroke.CAP_BUTT,
                         BasicStroke.JOIN_MITER,
                         10.0f, dashLengths, 0.f);
   final static float[] dottedLengths = {4.0f};
   final public static BasicStroke dottedStroke =
         new BasicStroke(1.0f,
                         BasicStroke.CAP_BUTT,
                         BasicStroke.JOIN_MITER,
                         10.0f, dottedLengths, 0.f);
   final public static BasicStroke solidStroke = new BasicStroke(1.0f);
   final public static Font h1Font = new Font(Font.SERIF, Font.BOLD, 20);
   final public static Font h2Font = new Font(Font.SERIF, Font.BOLD, 16);
   final public static Font defaultFont = new Font(Font.SERIF, Font.PLAIN, 12);
   
   public void setPaperBounds(Rectangle paperBounds) {
      this.paperBounds = paperBounds;
   }
      
   public Rectangle getPaperBounds() {return paperBounds;}
   
   // Constructor defining the Cartesian bounds
   public GraphicsPanel(boolean preserveAspectRatio) {
      this.preserveAspectRatio = preserveAspectRatio;
   }

   protected void setSolidLine() {graphics.setStroke(solidStroke);}

   protected void setDashedLine() {graphics.setStroke(dashedStroke);}

   protected void setDottedLine() {graphics.setStroke(dottedStroke);}

   protected Pt paperToPanel(Pt p) {
      return paperToPanel.transform(p);
   }
      
   public Rectangle paperToPanel(Rectangle paperRect) {
      Pt paperLLPt = new Pt(paperRect.getMinX(), paperRect.getMinY());
      Pt paperURPt = new Pt(paperRect.getMaxX(), paperRect.getMaxY());
      Pt panelLLPt = paperToPanel(paperLLPt);
      Pt panelURPt = paperToPanel(paperURPt);
      int panelWidth = (int)(panelURPt.getX() - panelLLPt.getX());
      int panelHeight = (int)(panelLLPt.getY() - panelURPt.getY());
      // max in paper is upper right, but in pixel coords is lower right, so we use maxPt.getY():
      return new Rectangle(panelLLPt.getX(), panelURPt.getY(), panelWidth, panelHeight);
   }   

   public void setH1Font() {
      graphics.setFont(h1Font);
   }
   public void setH2Font() {
      graphics.setFont(h2Font);
   }
   
   public void setDefaultFont() {
      graphics.setFont(defaultFont);
   }
   
   public void drawCircle(Pt center, double diam) {
      double panelDiam = paperToPanel.transformYLength(diam);
      drawEllipticalArc(center, diam, diam, 0., 360.);
   }

   public void drawEllipticalArc(Pt center, double width, double height, 
      double startAngle, double angleExtent) {
      Pt panelCenter = paperToPanel(center);
      double panelWidth = paperToPanel.transformXLength(height);
      double panelHeight = paperToPanel.transformYLength(width);
      Arc2D arc = new Arc2D.Double(
         (panelCenter.getX() - .5 * panelWidth), 
         (panelCenter.getY() - .5 * panelHeight),
         panelWidth, panelHeight, startAngle, angleExtent, Arc2D.OPEN);
      graphics.draw(arc);
   }

   public void drawEllipticalArcsReflectedInX(Pt center, double width, double height, 
         double startAngle, double angleExtent, double x) {
      drawEllipticalArc(center, width, height, startAngle, angleExtent);
      Pt reflectedCenter = center.reflectInX(x);
      double reflectedStartAngle = startAngle + angleExtent;
      drawEllipticalArc(reflectedCenter, width, height, reflectedStartAngle, angleExtent);
   }
      
   // Draw Cartesian line
   public void drawLine(Pt p1, Pt p2) {
      Pt tp1 = paperToPanel(p1);
      Pt tp2 = paperToPanel(p2);
      Line2D line = new Line2D.Double(tp1.getX(), tp1.getY(), tp2.getX(), tp2.getY());
      graphics.draw(line);
   }

   /** Draw a line, and also draw it reflected around a line parallel to the X axis. */
   public void drawLinesReflectedInY(Pt p1, Pt p2, double lineY) {
      drawLine(p1, p2);
      drawLine(p1.reflectInY(lineY), p2.reflectInY(lineY));
   }
   
   /** Draw a line, and also draw it reflected around a line parallel to the Y axis. */
   public void drawLinesReflectedInX(Pt p1, Pt p2, double lineX) {
      drawLine(p1, p2);
      drawLine(p1.reflectInX(lineX), p2.reflectInX(lineX));
   }
   
   // Draw paper rectangle
   public void drawRectangle(Rectangle paperRect) {
      Rectangle panelRect = paperToPanel(paperRect);
      graphics.drawRect((int)panelRect.getMinX(), (int)panelRect.getMinY(), 
         (int)panelRect.getWidth(), (int)panelRect.getHeight());
   }

   private Font getRotatedFont(double degrees) {
      AffineTransform fontAT = new AffineTransform();
      Font theFont = graphics.getFont();
      fontAT.rotate(degrees * Pt.radiansPerDegree);
      return theFont.deriveFont(fontAT);
   }

   public void drawText(String text, Pt paperLoc, TextPlacement placement) {
      drawText(text, paperLoc, placement, 0.);
   }
   
   // Draw text at paper location
   public void drawText(String text, Pt paperLoc, TextPlacement placement, double rotationAngle) {
      //FontMetrics fm = graphics.getFontMetrics();
      //Rectangle2D stringBounds = fm.getStringBounds(text, graphics);
      FontRenderContext frc = graphics.getFontRenderContext();
      TextLayout layout = new TextLayout(text, graphics.getFont(), frc);
      
      int stringWidth = (int) layout.getBounds().getWidth();
      int halfStringWidth = stringWidth / 2;
      int stringHeight = (int) layout.getBounds().getHeight();
      int halfStringHeight = stringHeight / 2;
      // Swing text origin point is the lower left, so offset is relative to that.
      // offsets are in pixel units, but have Y+ up
      int pxOff = 0;
      int pyOff = 0;
      if(placement == TextPlacement.TOP) {
         pxOff = -halfStringWidth;
      }
      else if(placement == TextPlacement.BOTTOM) {
         pxOff = -halfStringWidth;
         pyOff = -stringHeight;         
      }
      else if(placement == TextPlacement.LEFT) {
         pxOff = -stringWidth;
         pyOff = -halfStringHeight;
      }
      else if(placement == TextPlacement.RIGHT) {
         pyOff = -halfStringHeight;
      }
      else if(placement == TextPlacement.CENTER) {
         pxOff = -halfStringWidth;
         pyOff = -halfStringHeight;
      }
      Pt panelLoc = paperToPanel(paperLoc);
      if(rotationAngle == 0.) {
         // note we negate y offset for Y+ down pixel convention:
         int px = (int)(panelLoc.getX() + pxOff);
         int py = (int)(panelLoc.getY() - pyOff);
         graphics.drawString(text, px, py);
      }
      else {
         int xOffset = (rotationAngle > 0. ? -halfStringHeight : halfStringHeight);
         int px = (int)panelLoc.getX() + xOffset;
         int py = (int)panelLoc.getY();
         Font currentFont = graphics.getFont();
         Font rotatedFont = getRotatedFont(rotationAngle);
         graphics.setFont(rotatedFont);
         graphics.drawString(text, px, py);
         graphics.setFont(currentFont);
      }
      //graphics.drawRect(px, py - stringHeight, stringWidth, stringHeight);
   }

   /** Draw text at the default location (RIGHT). */
   public void drawText(String text, Pt loc) {
      drawText(text, loc, TextPlacement.RIGHT);
   }
   
   // Draw the panel. Subclasses should extend this.
   public void paintComponent(Graphics g) {
      super.paintComponent(g);
      this.graphics = (Graphics2D)g;
      Dimension size = this.getSize();
      panelBounds = new Rectangle(pixelMargin, pixelMargin, 
         size.getWidth() - 2 * pixelMargin, size.getHeight() - 2 * pixelMargin);
      paperToPanel = new Transform(paperBounds, panelBounds, 
         Transform.Origin.LL, preserveAspectRatio, true, size.getHeight());      
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setColor(Color.black);
      setDefaultFont();
      setSolidLine();
      drawRectangle(paperBounds);
   }

   protected void setColor(Color color) {
      graphics.setColor(color);
   }
   
   public static void main(String[] args) {
      JFrame frame = new JFrame();
      frame.setSize(800, 600);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      Rectangle paperBounds = new Rectangle(0., 0., 10., 10.);
      GraphicsPanel gp = new GraphicsPanel(true);
      gp.setPaperBounds(paperBounds);
      frame.getContentPane().add(gp);
      frame.setVisible(true);
      Rectangle rect = new Rectangle(1, 1, 2, 3);
      System.out.println(rect.getMinY());
   }
}
