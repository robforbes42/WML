/**
 *  Author: Rob Forbes Nov 2024
 */

package forbesflutes.wml.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.PrintGraphics;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import forbesflutes.boreptb.core.XYCurve;
import forbesflutes.wml.ui.Pt;
import forbesflutes.wml.ui.Rectangle;
import forbesflutes.wml.ui.Transform;
import forbesflutes.wml.ui.UIUtil;
import forbesflutes.wml.ui.WMLController;
import forbesflutes.wml.ui.WMLPanel;
import forbesflutes.wml.ui.Transform.Origin;
import forbesflutes.wml.WMLData;
import forbesflutes.wml.WMLFileModel;
import forbesflutes.wml.TextFileModel.TextModelListener;
import forbesflutes.wml.WMLData.AssembledSectionInfo;
import forbesflutes.wml.WMLData.BlowHole;
import forbesflutes.wml.WMLData.Joint;
import forbesflutes.wml.WMLData.Part;
import forbesflutes.wml.WMLData.Section;
import forbesflutes.wml.WMLData.ToneHole;
import forbesflutes.wml.WMLData.Joint.TenonDirection;
import forbesflutes.wml.ui.GraphicsPanel.TextPlacement;

public class WMLSketchPanel extends GraphicsPanel implements WMLPanel, TextModelListener, Printable {
   private WMLFileModel model; 
   private static final double dimensionLineLength = .15; // paper coords
   private static final double paperStandoff = .06; // paper coords
   private static final double endDimensionSpace = dimensionLineLength + 2. * paperStandoff;
   private static final double basicSectionMargin = .4;
   private static final double pageMargin = 1.25;
   private static final Pt toneHoleDimDir = Pt.polarPt(70,  1.);


   private WMLController controller;
   
   public static class SectionTransforms {
      public final Rectangle regionBounds;
      public final Rectangle sectionBounds;
      public final Transform worldToSection;
      public SectionTransforms(Rectangle regionBox, Rectangle sectionBounds,
            Transform worldToSection) {
         this.regionBounds = regionBox;
         this.sectionBounds = sectionBounds;
         this.worldToSection = worldToSection;
      }      
   }
   
   private void drawProfile(XYCurve curve, Transform worldToSection) {
      // body profile
      Pt paperPrevPt = null;
      Pt paperMidPt = worldToSection.transform(Pt.origin);
      for(int i = 0; i < curve.getNumPoints(); i++) {
         Pt worldPt = new Pt(curve.getX(i), .5 * curve.getY(i));
         Pt paperPt = worldToSection.transform(worldPt);
         if(paperPrevPt != null) {
            drawLinesReflectedInY(paperPrevPt, paperPt, paperMidPt.getY());
         }
         paperPrevPt = paperPt;
      }      
   }
    
   private void drawSection(Section section, SectionTransforms sectionTransforms,
         boolean isAssembled, boolean showBore) {
      // use half the box for the world bounds
      double textHeadroom = .2;
      Pt textPt = new Pt(endDimensionSpace, sectionTransforms.regionBounds.getMaxY() - textHeadroom);
      setH2Font();
      //drawText(section.name, textPt, TextPlacement.RIGHT);
      setDefaultFont();
      //drawRectangle(sectionTransforms.regionBox);
      drawBody(section, sectionTransforms.worldToSection, showBore && !isAssembled);
      drawHoles(section, sectionTransforms.worldToSection);
      if(isAssembled) {
         drawEmbouchureCoords(section, sectionTransforms.worldToSection, isAssembled);
         if(showBore)          
            drawBore(section, sectionTransforms.worldToSection);
      }
      else {
         drawSectionCoords(section, sectionTransforms.worldToSection, isAssembled);
         drawToneHoleDiams(section, sectionTransforms.worldToSection, isAssembled);
         drawBore(section, sectionTransforms.worldToSection);
         drawEndDimensions(section, sectionTransforms.worldToSection);
      }
   }
   
   private TextPlacement getTextPlacementForDir(Pt dir) {
      if(dir.equals(Pt.NORTH))
         return TextPlacement.TOP;
      else if(dir.equals(Pt.SOUTH))
         return TextPlacement.BOTTOM;
      else if(dir.getX() > 0)
         return TextPlacement.RIGHT;
      else
         return TextPlacement.LEFT;
   }
   
   /** Draw dimension with the default line length in the given direction. */
   private void drawDimension(double value, boolean isDiam, Pt rootPt, Pt dir, boolean verticalText) {
      Pt paperDimPt = rootPt.add(dir.scale(dimensionLineLength));
      drawDimension(value, isDiam, rootPt, paperDimPt, dir, verticalText);
   }
   
   /** Draw dimension to a particular point (custom line length). */
   private void drawDimension(double value, boolean isDiam, Pt rootPt, Pt paperDimPt, 
      Pt dir, boolean verticalText) {
      Pt standoff = dir.scale(paperStandoff);
      drawLine(rootPt.add(standoff), paperDimPt.add(standoff));
      String sValue = isDiam ? 
         UIUtil.formatDiameter(value, model.getUnits()) : WMLData.formatDouble(value, model.getUnits());
      Pt paperTextPt = paperDimPt.add(standoff).add(standoff);
      double rotationAngle = 0.;
      if(verticalText)
         rotationAngle = dir.getY() > 0. ? -90. : 90;
      drawText(sValue, paperTextPt, getTextPlacementForDir(dir), rotationAngle);      
   }
   
   private void drawEndDimensions(Section section, Section.End end, Transform worldToRegion) {
      double worldEndX, bodyDiam, boreDiam;
      Pt dir;
      double tenonDiam = 0.;
      double tenonLength = 0.;
      boolean isSocket = false;
      boolean isTenon = false;
      if(end == Section.End.TOP) {
         worldEndX = 0.;
         bodyDiam = section.bodyCurve.getFirstY();
         if(section.topJoint != null) {
            tenonDiam = section.topJoint.tenonDiam;
            tenonLength = section.topJoint.tenonLength;
            isSocket = section.topJoint.tenonDirection == TenonDirection.DOWN;
            isTenon = !isSocket;
         }
         boreDiam = section.boreCurve.getFirstY();
         dir = Pt.WEST;
      }
      else {
         worldEndX = section.length;
         bodyDiam = section.bodyCurve.getLastY();
         if(section.bottomJoint != null) {
            tenonDiam = section.bottomJoint.tenonDiam;
            tenonLength = section.bottomJoint.tenonLength;
            isSocket = section.bottomJoint.tenonDirection == TenonDirection.UP;
            isTenon = !isSocket;
         }
         boreDiam = section.boreCurve.getLastY();  
         dir = Pt.EAST;
      }
      
      // body
      double maxBodyDiam = section.getMaxBodyDiam();
      Pt  worldEnd = new Pt(worldEndX, .5 * maxBodyDiam);
      // move the body dimension over to tenon root if needed:
      Pt worldBodyRoot = isTenon ? worldEnd.add(dir.scale(-tenonLength)) : worldEnd;
      // Body OD:
      drawDimension(bodyDiam, true, worldToRegion.transform(worldBodyRoot), Pt.NORTH, false);
      // Tenon OD:
      if(tenonDiam > 0.) {
         Pt tenonRoot = worldToRegion.transform(new Pt(worldEndX, .5 * tenonDiam)); 
         drawDimension(tenonDiam, true, tenonRoot, dir, false);         
      }
      // Bore ID, allowing for possible socket inset:
      double boreRootX = 0.;
      if(isSocket) 
         boreRootX = worldEndX + dir.getX() * -tenonLength;
      else
         boreRootX = worldEndX;
      double boreDimY = 0.;
      if(tenonDiam > 0)   
         boreDimY = 0.;
      else
         boreDimY = .5 * boreDiam;
      Pt boreRoot = worldToRegion.transform(new Pt(boreRootX, .5 * boreDiam)); 
      Pt paperDimOffset = dir.scale(dimensionLineLength);
      Pt boreDimPt = worldToRegion.transform(new Pt(worldEndX, boreDimY)
         .add(paperDimOffset)); 
      drawDimension(boreDiam, true, boreRoot, boreDimPt, dir, false);           
   }
   
   private void drawEndDimensions(Section section, Transform worldToRegion) {
      drawEndDimensions(section, Section.End.TOP, worldToRegion);
      drawEndDimensions(section, Section.End.BOTTOM, worldToRegion);
   }
   
   private void drawSectionCoords(Section section, Transform worldToRegion, boolean isAssembled) {
      double maxBodyDiam = section.getMaxBodyDiam();
      drawSectionX(0., 0., maxBodyDiam, worldToRegion, isAssembled, isAssembled);
      if(section.blowHole != null) {
         double corkCoord = section.blowHole.getCorkSectionCoord();
         if(corkCoord > 0.)
            drawSectionX(corkCoord, corkCoord, maxBodyDiam, worldToRegion, isAssembled, isAssembled);
         double xCoord = section.blowHole.xCoord;
         drawSectionX(xCoord, xCoord, maxBodyDiam, worldToRegion, isAssembled, isAssembled);
      }
      for(ToneHole toneHole: section.toneHoles) {
         double xCoord = toneHole.xCoord;
         drawSectionX(xCoord, xCoord, maxBodyDiam, worldToRegion, isAssembled, isAssembled);
      }
      drawSectionX(section.length, section.length, maxBodyDiam, worldToRegion, isAssembled, isAssembled);
   }

   private void drawEmbouchureCoords(Section section, Transform worldToRegion, boolean isAssembled) {      
      if(section.blowHole == null) return;
      double maxBodyDiam = section.getMaxBodyDiam();
      double embCoord = section.blowHole.xCoord;
      drawSectionX(embCoord, 0., maxBodyDiam, worldToRegion, false, isAssembled);
      for(ToneHole toneHole: section.toneHoles) {
         drawSectionX(toneHole.xCoord, toneHole.xCoord - embCoord, maxBodyDiam, worldToRegion, false, isAssembled);
      }
   }

   private void drawToneHoleDiams(Section section, Transform worldToRegion, boolean isAssembled) {
      double maxBodyDiam = section.getMaxBodyDiam();
      if(section.blowHole != null)
         drawBlowHoleSize(section.blowHole, maxBodyDiam, worldToRegion);
      for(ToneHole toneHole: section.toneHoles) {
         drawToneHoleDiameter(toneHole, maxBodyDiam, worldToRegion);
         //double od = section.bodyCurve.interpolateY(toneHole.xCoord);
      }
   }

   private void drawBlowHoleSize(BlowHole blowHole, double maxBodyDiam,
         Transform worldToRegion) {
      Pt center = new Pt(blowHole.xCoord, 0.);
      String svalue = String.format("%s x %s", WMLData.formatDouble(blowHole.length, model.getUnits()),
         WMLData.formatDouble(blowHole.width, model.getUnits()));
      drawHoleDimension(blowHole.width, maxBodyDiam, worldToRegion, center, svalue);
   }

   private void drawToneHoleDiameter(ToneHole toneHole, double maxBodyDiam, Transform worldToRegion) {
      Pt center = new Pt(toneHole.xCoord, 0.);
      String svalue = UIUtil.formatDiameter(toneHole.diam, model.getUnits());
      drawHoleDimension(toneHole.diam, maxBodyDiam, worldToRegion, center, svalue);
   }

   private void drawHoleDimension(double diam, double maxBodyDiam,
         Transform worldToRegion, Pt center, String svalue) {
      double worldDimLineYProj = (dimensionLineLength + paperStandoff) / worldToRegion.transformYLength(1.);
      double worldLineEndY = .5 * maxBodyDiam + worldDimLineYProj;
      Pt standoff = toneHoleDimDir.scale(paperStandoff);
      Pt worldRim = center.add(toneHoleDimDir.scale(.5 * diam));
      double worldInverseSlope = toneHoleDimDir.getX() / toneHoleDimDir.getY();
      double worldEndOfLineX = worldRim.getX() + worldInverseSlope * (worldLineEndY - center.getY());
      Pt paperEndOfLine = worldToRegion.transform(new Pt(worldEndOfLineX, worldLineEndY));
      drawLine(worldToRegion.transform(worldRim).add(standoff), paperEndOfLine);
      Pt paperTextLoc = paperEndOfLine.add(standoff);
      drawText(svalue, paperTextLoc, TextPlacement.TOP);
   }

   private void drawSectionX(double worldX, double value, double maxBodyDiam, 
         Transform worldToRegion, boolean top, boolean verticalText) {
      double baseY =  (top ? 1. : -1) * .5 * maxBodyDiam - paperStandoff;
      Pt rootPt = worldToRegion.transform(new Pt(worldX, baseY));
      Pt dir = top ? Pt.NORTH : Pt.SOUTH;
      drawDimension(value, false, rootPt, dir, verticalText);
   }

   private void drawHoles(Section section, Transform worldToRegion) {
      if(section.blowHole != null) {
         // in this case, "width" is in Y and "length" is in X
         Pt paperCenter = worldToRegion.transform(new Pt(section.blowHole.xCoord, 0.));
         double paperLength = worldToRegion.transformXLength(section.blowHole.length);
         double paperWidth = worldToRegion.transformYLength(section.blowHole.width);
         double straightLength = paperLength - paperWidth;
         // p1, p2 are the endpoints of the upper straight line
         Pt p1 = paperCenter.add(new Pt(.5 * straightLength, .5 * paperWidth));
         Pt p2 = p1.reflectInX(paperCenter.getX());
         drawLinesReflectedInY(p1, p2, paperCenter.getY());
         Pt arcCenter = paperCenter.add(new Pt(.5 * straightLength, 0.));
         drawEllipticalArcsReflectedInX(arcCenter, paperWidth, paperWidth, -90., 180., paperCenter.getX());
      }
      for(ToneHole toneHole: section.toneHoles) {
         Pt worldPt = new Pt(toneHole.xCoord, 0.);
         double paperDiam = worldToRegion.transformXLength(toneHole.diam);
         drawCircle(worldToRegion.transform(worldPt), paperDiam);
      }
   }

   private void drawBore(Section section, Transform worldToSection) {
      setDottedLine();
      drawProfile(section.boreCurve, worldToSection);
      drawCork(section, worldToSection);
      setSolidLine();
   }

   private void drawCork(Section section, Transform worldToSection) {
      if(section.blowHole != null && section.blowHole.corkDist > 0.) {
         double corkCoord = section.blowHole.getCorkSectionCoord();
         double boreDiam = section.boreCurve.interpolateY(corkCoord);
         Pt world1 = new Pt(corkCoord, .5 * boreDiam);
         Pt world2 = new Pt(corkCoord, -.5 * boreDiam);
         drawLine(worldToSection.transform(world1), worldToSection.transform(world2)); 
      }
   }

   private void drawBody(Section section, Transform worldToSection, boolean showBore) {
      drawProfile(section.bodyCurve, worldToSection);
      double topBodyX = section.bodyCurve.getXMin();
      double topBodyDiam = section.bodyCurve.getFirstY();
      double bottomBodyX = section.bodyCurve.getXMax();
      double bottomBodyDiam = section.bodyCurve.getLastY();
      
      if(section.topJoint != null) {
         Joint joint = section.topJoint;
         double tenonDiam = joint.tenonDiam;
         Pt bp = new Pt(topBodyX, .5 * topBodyDiam);  
         Pt tp1 = new Pt(topBodyX, .5 * tenonDiam);
         Pt tp2;
         boolean isTenon = joint.tenonDirection == Joint.TenonDirection.UP;
         if(isTenon) 
            tp2 = new Pt(0., .5 * joint.tenonDiam);
         else // socket
            tp2 = new Pt(joint.tenonLength, .5 * joint.tenonDiam);
         drawTenonOrSocket(worldToSection, bp, tp1, tp2, isTenon, showBore);
      }
      else {
         Pt p1 = new Pt(0, .5 * topBodyDiam);
         Pt p2 = p1.reflectInY(0.);
         drawLine(worldToSection.transform(p1), worldToSection.transform(p2));
      }
      
      if(section.bottomJoint != null) {
         Joint joint = section.bottomJoint;
         double tenonDiam = joint.tenonDiam;
         Pt bp = new Pt(bottomBodyX, .5 * bottomBodyDiam);
         Pt tp1 = new Pt(bottomBodyX, .5 * tenonDiam);
         Pt tp2;
         boolean isTenon = joint.tenonDirection == Joint.TenonDirection.DOWN;
         if(isTenon) 
            tp2 = new Pt(section.length, .5 * joint.tenonDiam);
         else// socket
            tp2 = new Pt(section.length - joint.tenonLength, .5 * joint.tenonDiam);
         drawTenonOrSocket(worldToSection, bp, tp1, tp2, isTenon, showBore);
      }
      else {
         Pt p1 = new Pt(section.length, .5 * bottomBodyDiam);
         Pt p2 = p1.reflectInY(0.);
         drawLine(worldToSection.transform(p1), worldToSection.transform(p2));
      }
   }

   private void drawTenonOrSocket(Transform worldToRegion, Pt bp, Pt tp1,
      Pt tp2, boolean isTenon, boolean showBore) {
      Pt paperBoreCenter = worldToRegion.transform(new Pt(tp2.getX(), 0.));
      double centerY = paperBoreCenter.getY();
      if(isTenon) {
         drawLinesReflectedInY(worldToRegion.transform(bp), worldToRegion.transform(tp1), centerY);
      }
      else {  // socket
         if(!showBore) return;
         drawLine(worldToRegion.transform(bp), worldToRegion.transform(bp.reflectInY(0.)));
         setDottedLine();        
      }
      drawLinesReflectedInY(worldToRegion.transform(tp1), worldToRegion.transform(tp2), centerY);
      drawLinesReflectedInY(worldToRegion.transform(tp2), paperBoreCenter, centerY);
      setSolidLine();
   }
   
   public void paintComponent(Graphics g) {
      super.paintComponent(g);
      if(!model.isValid()) {
         Pt pt = new Pt(0., paperBounds.getHeight() - .5);
         drawText("Model is not valid.", pt);
         return;
      }      
      if(g instanceof PrintGraphics)
         setBackground(Color.white);
      // TODO: make this a check box ?
      boolean showBore = true;
      drawSections(showBore);
   }

   private SectionTransforms createSectionTransforms(Section section, double minBoxY, double sectionBoxHeight, double margin) {
      double maxBodyDiam = section.getMaxBodyDiam();
      // world bounds are in flute xCoord and diameter units
      Rectangle worldBounds = new Rectangle(0., -.5 * maxBodyDiam, 
            section.length, maxBodyDiam);
      // region bounds is the area dedicated to this section
      Rectangle regionBounds = new Rectangle(margin, minBoxY, 
         paperBounds.getWidth() - 2. * margin, sectionBoxHeight);
      //drawRectangle(regionBounds);
      // section bounds is the area inside the region for drawing the instrument body
      // transforms will put the world box in the lower left corner of the section box
      double sectionInset = .25 * sectionBoxHeight;
      Rectangle sectionBounds = new Rectangle(margin, minBoxY + sectionInset, 
         paperBounds.getWidth() - 2. * margin, 2. * sectionInset);
      Transform worldToSectionBox = 
         new Transform(worldBounds, sectionBounds, Transform.Origin.CENTER, true);  
      return new SectionTransforms(regionBounds, sectionBounds, worldToSectionBox);
   }
   
   private void drawSections(boolean showBore) {
      // part data will be for a section or assembled sections
      // coords are paper coords unless noted
      Pt titlePt = new Pt(paperBounds.getCenterX(), paperBounds.getMaxY() - .5 * pageMargin);
      setH1Font();
      drawText("A Sample Flute", titlePt, TextPlacement.CENTER);
      setDefaultFont();
      Part part = model.getData().parts.get(0);
      List<Section> sections = new ArrayList<Section>(part.sections);
      int numSectionBoxes = sections.size() > 1 ? part.sections.size() + 1 : part.sections.size();
      double sectionBoxHeight = (paperBounds.getHeight() - 2. * pageMargin) / numSectionBoxes;
      double minBoxY = paperBounds.getHeight() - pageMargin - sectionBoxHeight;    
      for(Section section: sections) {
         SectionTransforms sectionTransforms = 
            createSectionTransforms(section, minBoxY, sectionBoxHeight, pageMargin);
         drawSection(section, sectionTransforms, false, showBore) ;
         // optionally, draw bore in dotted line
         // draw tone holes
         //drawLine(new Pt(basicSectionMargin, minBoxY), 
         //   new Pt(paperBounds.getWidth() - basicSectionMargin, minBoxY));
         minBoxY -= sectionBoxHeight;
      }
      if(sections.size()> 1) {
         AssembledSectionInfo assSectionInfo = part.createAssembledSection();
         SectionTransforms sectionTransforms = 
            createSectionTransforms(assSectionInfo.section, minBoxY, sectionBoxHeight, basicSectionMargin);
          drawSection(assSectionInfo.section, sectionTransforms, true, false);
          drawSectionBoundaries(assSectionInfo, sectionTransforms, false);
      }
   }
   
   private void drawSectionBoundaries(AssembledSectionInfo assSectionInfo, 
         SectionTransforms sectionTransforms, boolean showBore) {
      Section section = assSectionInfo.section;
      Transform worldToSection = sectionTransforms.worldToSection;
      for(int bodyBreakIndex: assSectionInfo.bodySectionBreakIndices) {
         double bodyX = section.bodyCurve.getX(bodyBreakIndex);
         double bodyDiam = section.bodyCurve.getY(bodyBreakIndex);
         Pt p1 = new Pt(bodyX, .5 * bodyDiam);
         Pt p2 = p1.reflectInY(0.);
         drawLine(worldToSection.transform(p1), worldToSection.transform(p2));
      }
      if(showBore) {
         setDottedLine();
         for(int boreBreakIndex: assSectionInfo.boreSectionBreakIndices) {
            double boreX = section.boreCurve.getX(boreBreakIndex);
            double boreDiam = section.boreCurve.getY(boreBreakIndex);
            Pt p1 = new Pt(boreX, .5 * boreDiam);
            Pt p2 = p1.reflectInY(0.);
            drawLine(worldToSection.transform(p1), worldToSection.transform(p2));         
         }
         setSolidLine();
      }
   }

   public WMLSketchPanel(WMLFileModel model, WMLController controller) {
      super(true);
      this.model = model;
      this.controller = controller;
      model.addListener(this);
      //setLayout(new BorderLayout());
      setPaperBounds(new Rectangle(0., 0., 8.5, 11));  // landscape
    }

   public void modelChanged() {
      repaint();
   }

   public void tabOpened() {repaint();}
   
   public void tabClosed() {}
   
   public void onExit() {}

   public void doSaveImage() {
      // TODO: be smarter about paper aspect, and query for image size
      int imageWidth = 600;
      int imageHeight = (int) (imageWidth * paperBounds.getHeight() / paperBounds.getWidth());
      File file = controller.chooseFile();
      if(file == null) return;
      BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_BYTE_GRAY);
      Graphics2D g = image.createGraphics();
      setSize(imageWidth, imageHeight);
      printAll(g);
      g.dispose();
      try { 
          ImageIO.write(image, "png", file); 
      } catch (IOException e) {
         JOptionPane.showMessageDialog(this, "Error saving image: " + e.getMessage());
      }      
   }

   public void doPrint() {
      PrinterJob job = PrinterJob.getPrinterJob();
      job.setPrintable(this);
      try {
         boolean doPrint = job.printDialog();
         if(doPrint) job.print();
     } catch (PrinterException e) {
         JOptionPane.showMessageDialog(this, "Print error: " + e.getMessage());
     }
   }

   @Override
   public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
      if(pageIndex > 0) return NO_SUCH_PAGE;
      paintComponent(graphics);
      return PAGE_EXISTS;
   }
   
}
