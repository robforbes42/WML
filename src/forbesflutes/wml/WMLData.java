/**
 * Data and core operations for WML files.
 * 
 * Author: Rob Forbes Nov 2024
 */
package forbesflutes.wml;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import forbesflutes.boreptb.core.XYCurve;
import forbesflutes.wml.WMLData.Joint.TenonDirection;

/** Data associated with Woodwind Measurement Language (.wml) files. */
public class WMLData implements java.lang.Cloneable {
   public enum Units {MM, INCH};
   public static final double mmPerInch = 25.4;
   public static final double inchesPerMM = 1. / mmPerInch;
   public static double getConversionFactor(Units units) {return units == Units.INCH ? inchesPerMM : mmPerInch;}
   public static final String MM_UNITS = "mm";
   public static final String INCH_UNITS = "inch";
   
   String filepath;
   public String id;       // nullable
   public String name;       // nullable
   public String type;       // nullable
   public String subType;       // nullable
   public String owner;       // nullable
   public String maker;       // nullable
   public String serial;       // nullable
   public String submittedBy;       // nullable
   public String measuredBy;        // nullable
   public String url;       // nullable
   public String comments;       // nullable
   public double pitchStandard;       // nullable
   public String keyOf;       // nullable
   public Units units; // required
   public List<Part> parts = new ArrayList<Part>();

   /* Convert measurements to the given unit. */
   public void convertUnits(Units newUnits) {
      if(newUnits.equals(units)) return;
      double factor = newUnits.equals(Units.MM) ? mmPerInch : inchesPerMM;
      for(Part part: parts)
         part.convertUnits(factor);     
      units = newUnits;
   }


   /** Make a deep copy of this object. */
   public WMLData copy() {
      // First make a shallow copy:
      WMLData copy = null;
      try {
         copy = (WMLData) this.clone();
      } catch (CloneNotSupportedException e) {}
      // now deep copy any mutable objects:
      List<Part> oldParts = parts;
      parts = new ArrayList<Part>();
      for(Part oldPart: oldParts)
         parts.add(oldPart.copy());
      return copy;
   }
   
   private static void exportXYCurve(XYCurve curve, Units units, PrintWriter out) {
      int numPoints = curve.getNumPoints();
      for(int i = 0; i < numPoints; i++) {
         out.printf("%s\t%s\n", 
            WMLUtil.formatDouble(curve.getX(i), units),
            WMLUtil.formatDouble(curve.getY(i), units));
      }
   }
   
   private static void printSingleString(String key, String value, PrintWriter out) {
      if(value == null) return;
      out.printf("%s\t%s\n", key, value);
   }
   
   @Override
   public boolean equals(Object o) {
      return WMLUtil.equals(this, o);
   }

   /** Export TSV. CAUTION: no comments will appear in the file. */
   public void exportTSV(String exportFilepath)  throws IOException {
      PrintWriter out = new PrintWriter(new FileWriter(exportFilepath));
      exportTSV(out, units);
      out.close();
   }

   private void exportTSV(PrintWriter out, Units units) {
      printSingleString(WMLParser.id, id, out);
      printSingleString(WMLParser.name, name, out);
      printSingleString(WMLParser.type, subType, out);
      printSingleString(WMLParser.subType, subType, out);
      printSingleString(WMLParser.owner, owner, out);
      printSingleString(WMLParser.maker, maker, out);
      printSingleString(WMLParser.serial, serial, out);
      printSingleString(WMLParser.submittedBy, submittedBy, out);
      printSingleString(WMLParser.measuredBy, measuredBy, out);
      printSingleString(WMLParser.url, url, out);
      printSingleString(WMLParser.pitchStandard, comments, out);
      printSingleString(WMLParser.keyOf, keyOf, out);
      printSingleString(WMLParser.units, units.toString().toLowerCase(), out);
      for(Part part:parts)
         part.exportTSV(out, units);      
   }

   /** Specification of how to assemble the sections. */
   public static class AssemblyInfo {
      double[] jointExtensions;  // nullable
      List<String> altSectionNames;  // nullable
   }

   /** Information about the assembled sections; the section itself and
    *  some derived info about section breaks etc.
    */
   public static class AssembledSectionInfo {
      public final Section section;
      public final int[] bodySectionBreakIndices;
      public final int[] boreSectionBreakIndices;
      
      public AssembledSectionInfo(Section section, int[] bodySectionBreakIndices,
            int[] boreSectionBreakIndices) {
         this.section = section;
         this.bodySectionBreakIndices = bodySectionBreakIndices;
         this.boreSectionBreakIndices = boreSectionBreakIndices;
      }      
   }
   
   /** A physically separate part of an instrument. Instruments like flutes have only
    *  one, but bagpipes (e.g.) can have many (chanter, drones, etc.).
    */
   public static class Part implements java.lang.Cloneable {
      public String name;  // nullable
      public String noteRangeStart; // nullable
      public String noteRangeEnd;   // nullable
      
      public List<Section> sections = new ArrayList<Section>();
      
      /* Multiply measurements by the given factor. */
      private void convertUnits(double factor) {
         for(Section section: sections)
            section.convertUnits(factor);
      }
      
      private void exportTSV(PrintWriter out, Units units) {
         out.printf("%s\t%s\n", WMLParser.part, name);         
         for(Section section: sections)
            section.exportTSV(out, units);
      }
      
      /** Do any post-processing needed after part is constructed. */
      public void endPart() {
         stitchSectionJoints();
      }
            
      // make each section's top joint the preceding one's bottom joint
      public void stitchSectionJoints() {
         Section prevSection = null;
         for(Section section: sections) {
            if(prevSection != null)
               prevSection.bottomJoint = section.topJoint;
            prevSection = section;
         }         
      }
      
      /** Return a deep copy of this object. */
      public Part copy() {
         // First make a shallow copy:
         Part copy = null;
         try {
            copy = (Part) this.clone();
         } catch (CloneNotSupportedException e) {}
         // now deep copy any mutable objects:
         List<Section> oldSections = sections;
         copy.sections = new ArrayList<Section>();
         for(Section oldSection: oldSections)
            copy.sections.add(oldSection.copy());
         copy.stitchSectionJoints();
         return copy;
      }
      
      @Override
      public boolean equals(Object o) {
         return WMLUtil.equals(this, o);
      }

      /** Make a section (and associated info) representing an assembled Part. */
      public AssembledSectionInfo createAssembledSection() {
         List<Double> assBoreXCoords = new ArrayList<Double>();
         List<Double> assBoreDiams = new ArrayList<Double>();
         List<Double> assBodyXCoords = new ArrayList<Double>();
         List<Double> assBodyDiams = new ArrayList<Double>();
         List<Integer> boreSectionBreakIndices = new ArrayList<Integer>();
         List<Integer> bodySectionBreakIndices = new ArrayList<Integer>();
         Section assSection = new Section();
         assSection.name = "Assembled";
         double sectionStartX = 0.;  // cumulative start of next section; joints considered
         boolean isFirstSection = true;
         for(Section section: sections) {
            // tone holes:
            for(ToneHole toneHole: section.toneHoles) {
               ToneHole assToneHole = toneHole.copy();
               assToneHole.xCoord = toneHole.xCoord + sectionStartX;
               assSection.toneHoles.add(assToneHole);
            }
            double boreCoordOffset = 0.;
            
            // If section has a top socket, correct for bore measurement offset:
            if(section.hasTopSocket()) 
               boreCoordOffset = -section.topJoint.tenonLength;
            
            // bore/body breaks
            if(!isFirstSection) {
               boreSectionBreakIndices.add(assBoreDiams.size() - 1);
               bodySectionBreakIndices.add(assBodyDiams.size() - 1);
            }
            // bore
            for(double d: section.boreCurve.getYValues())
               assBoreDiams.add(d);
            addProfileCoords(section.boreCurve.getXValues(), sectionStartX, boreCoordOffset, 
                assBoreXCoords, assBoreDiams);
            // body
            for(double d: section.bodyCurve.getYValues())
               assBodyDiams.add(d);
            addProfileCoords(section.bodyCurve.getXValues(), sectionStartX, boreCoordOffset, 
                assBodyXCoords, assBodyDiams);
            sectionStartX += section.length;
            if(section.bottomJoint != null) {
               // section coords start at socket root
               sectionStartX -= section.bottomJoint.tenonLength;
            }
            
            if(section.bottomJoint != null && section.bottomJoint.extension > 0.) {
               double extension = section.bottomJoint.extension;
               double tenonLength = section.bottomJoint.tenonLength;
               double tenonDiam = section.bottomJoint.tenonDiam;
               TenonDirection tenonDirection = section.bottomJoint.tenonDirection;
               double bodyExtensionStart = (tenonDirection == TenonDirection.UP) ?
                  sectionStartX + tenonLength: 
                  sectionStartX;
               assBodyXCoords.add(bodyExtensionStart);
               assBodyDiams.add(tenonDiam);
               assBodyXCoords.add(bodyExtensionStart + extension);
               assBodyDiams.add(tenonDiam);
               double boreExtensionStart = (tenonDirection == TenonDirection.UP) ?
                     sectionStartX: 
                     sectionStartX + tenonLength;
               assBoreXCoords.add(boreExtensionStart);
               assBoreDiams.add(tenonDiam);
               assBoreXCoords.add(boreExtensionStart + extension);
               assBoreDiams.add(tenonDiam);
               sectionStartX += extension;
            }
            isFirstSection = false;
         }
         double[] boreXCoords = WMLUtil.toDoubleArray(assBoreXCoords);
         double[] boreDiams = WMLUtil.toDoubleArray(assBoreDiams);
         assSection.boreCurve = new XYCurve(boreXCoords, boreDiams);
         double[] bodyXCoords = WMLUtil.toDoubleArray(assBodyXCoords);
         double[] bodyDiams = WMLUtil.toDoubleArray(assBodyDiams);
         assSection.bodyCurve = new XYCurve(bodyXCoords, bodyDiams);
         assSection.length = sectionStartX;
         assSection.blowHole = sections.get(0).blowHole;
         // return new AssembledPartData(profile, WMLData.toArray(toneHoleCoords), WMLData.toArray(sectionBreakCoords));
         return new AssembledSectionInfo(assSection, WMLUtil.toIntArray(bodySectionBreakIndices), 
               WMLUtil.toIntArray(boreSectionBreakIndices));
      }

      private void addProfileCoords(double[] sectionXCoords, double sectionStartX,
          double coordOffset, List<Double> assBoreXCoords, List<Double> assBoreYCoords) {
          for(int i = 0; i < sectionXCoords.length; i++) {
            double x = sectionStartX + sectionXCoords[i] + coordOffset;
            assBoreXCoords.add(x);
         }
      }
   }
   
   /** A tenon/socket joint joining two Sections. */
   public static class Joint  implements java.lang.Cloneable {
      public enum TenonDirection {UP, DOWN} 
      public TenonDirection tenonDirection;  // required
      public double tenonLength; // required
      public double tenonDiam;  // required
      public double extension; // optional
      
      /** Return a deep copy of this object. */
      public Joint copy() {
         // First make a shallow copy:
         Joint copy = null;
         try {
            copy = (Joint) this.clone();
         } catch (CloneNotSupportedException e) {}
         // now deep copy any mutable objects:
         return copy;
      }

      public void convertUnits(double factor) {
         tenonLength *= factor;
         tenonDiam *= factor;     
         extension *= factor;
      }
      
      @Override
      public boolean equals(Object o) {
         return WMLUtil.equals(this, o);
      }

      private void exportTSV(PrintWriter out, Units units) {
         out.printf("%s\t%s\t%s\t%s\t%s\n", WMLParser.joint, tenonDirection.toString().toLowerCase(),
           WMLUtil.formatDouble(tenonLength, units),
           WMLUtil.formatDouble(tenonDiam, units),
           WMLUtil.formatDouble(extension, units));
      }
   }
   
   /** A piece (possibly the only one) of a Part. If there is more than one in
    *  a Part, they are connected by Joints.
    */
   public static class Section implements java.lang.Cloneable {
      public enum End {TOP, BOTTOM} 
      public String name; // nullable
      public double length;
      public BlowHole blowHole; // nullable
      public List<ToneHole> toneHoles = new ArrayList<ToneHole>();
      public XYCurve bodyCurve;
      public XYCurve boreCurve;
      public Joint topJoint; // nullable
      public Joint bottomJoint; // nullable

      private double getMax(double[] array) {
         double max = Double.MIN_VALUE;
         for(double d: array) 
            if(d > max) max = d;
         return max;
      }
      
      public double getMaxBodyDiam() {
         return  bodyCurve.getYMax();
      }
      
      public double getMaxBoreDiam() {
         return boreCurve.getYMax();
      }

      public boolean hasTopSocket() {
         return topJoint != null && topJoint.tenonDirection == Joint.TenonDirection.DOWN;
      }

      public boolean hasBottomSocket() {
         return bottomJoint != null && bottomJoint.tenonDirection == Joint.TenonDirection.UP;
      }

      public boolean hasTopTenon() {
         return topJoint != null && topJoint.tenonDirection == Joint.TenonDirection.UP;
      }

      public boolean hasBottomTenon() {
         return bottomJoint != null && bottomJoint.tenonDirection == Joint.TenonDirection.DOWN;
      }

      /** Calculate tone hole depth as best we can, otherwise return 0. */
      public double getToneHoleDepth(int i) {
         if(boreCurve != null) return 0.; // can't do anything without the bore diam
         ToneHole toneHole = toneHoles.get(i);
         double bodyDiam;
         if(toneHole.bodyDiam != 0.)
            bodyDiam = toneHole.bodyDiam;         
         else if(bodyCurve != null)  // if no measured body OD @ tone hole, interpolated it
            bodyDiam = bodyCurve.interpolateY(toneHole.xCoord);
         else
            return 0.;       
         double boreDiam = boreCurve.interpolateY(toneHole.xCoord);
         return bodyDiam - boreDiam;
      }

      /* Multiply measurements by the given factor. */
      private void convertUnits(double factor) {
         if(blowHole != null) 
            blowHole.convertUnits(factor);
         if(topJoint != null) 
            topJoint.convertUnits(factor);
         length *= factor;
         boreCurve.scale(factor);
         bodyCurve.scale(factor);
         for(ToneHole toneHole: toneHoles)
            toneHole.convertUnits(factor);
      }
      
      @Override
      public boolean equals(Object o) {
         return WMLUtil.equals(this, o);
      }

      private void exportTSV(PrintWriter out, Units units) {
         out.printf("%s\t%s\t%s\n", WMLParser.section, name, WMLUtil.formatDouble(length, units));
         if(blowHole != null) 
            blowHole.exportTSV(out, units);
         if(topJoint != null) 
            topJoint.exportTSV(out, units);
         for(ToneHole toneHole: toneHoles)
            toneHole.exportTSV(out, units);
         out.println(WMLParser.bore);
         exportXYCurve(boreCurve, units, out);
         out.println(WMLParser.body);
         exportXYCurve(bodyCurve, units, out);
      }
      
      /** Return a deep copy of this object. */
      public Section copy() {
         // First make a shallow copy:
         Section copy = null;
         try {
            copy = (Section) this.clone();
         } catch (CloneNotSupportedException e) {}
         // now deep copy any mutable objects:
         List<ToneHole> oldToneHoles = toneHoles;
         copy.toneHoles = new ArrayList<ToneHole>();
         for(ToneHole oldToneHole: oldToneHoles)
            copy.toneHoles.add(oldToneHole.copy());
         if(topJoint != null)
            copy.topJoint = topJoint.copy();
         copy.bodyCurve = bodyCurve.copy();
         copy.boreCurve = boreCurve.copy();
         return copy;
      }      
   }

   public static class BlowHole implements java.lang.Cloneable {
      public double xCoord; // location in section; required
      public double length;  // required
      public double width;  // required
      public double bodyDiam;  // optional
      public double corkDist;  // optional, distance to face of cork
      
      public BlowHole(double xCoord, double length, double width, double bodyDiam, double corkDist) {
         this.xCoord = xCoord;
         this.length = length;
         this.width = width;
         this.bodyDiam = bodyDiam;
         this.corkDist = corkDist;
      }
      
      public double getCorkSectionCoord() {
         return xCoord - corkDist;
      }

      /* Multiply measurements by the given factor. */
      private void convertUnits(double factor) {
         xCoord *= factor;
         length *= factor;
         width *= factor;
         bodyDiam *= factor;
         corkDist *= factor;
      }
      
      @Override
      public boolean equals(Object o) {
         return WMLUtil.equals(this, o);
      }

      private void exportTSV(PrintWriter out, Units units) {
         out.printf("%s\t%s\t%s\t%s\t%s\t%s\n", WMLParser.blowHole,
            WMLUtil.formatDouble(xCoord, units),
            WMLUtil.formatDouble(length, units),
            WMLUtil.formatDouble(width, units),
            WMLUtil.formatDouble(bodyDiam, units),
            WMLUtil.formatDouble(corkDist, units));         
      }
      
      /** Return a deep copy of this object. */
      public ToneHole copy() {
         // First make a shallow copy:
         ToneHole copy = null;
         try {
            copy = (ToneHole) this.clone();
         } catch (CloneNotSupportedException e) {}
         // now deep copy any mutable objects:
         return copy;
      }      
   }

   /** A tone hole. */
   public static class ToneHole implements java.lang.Cloneable {
      public double xCoord; // location in section; required
      public double diam;  // required
      public double bodyDiam;  // optional
      public double bodyToTop;  // optional. For inset tone holes
      
      public ToneHole(double xCoord, double diam, double bodyDiam, double bodyToTop) {
         this.xCoord = xCoord;
         this.diam = diam;
         this.bodyDiam = bodyDiam;
         this.bodyToTop = bodyToTop;
      }

      /* Multiply measurements by the given factor. */
      private void convertUnits(double factor) {
         xCoord *= factor;
         diam *= factor;
         bodyDiam *= factor;
         bodyToTop *= factor;
      }

      private void exportTSV(PrintWriter out, Units units) {
         out.printf("%s\t%s\t%s\t%s\n", WMLParser.toneHole,
            WMLUtil.formatDouble(xCoord, units),
            WMLUtil.formatDouble(diam, units),
            WMLUtil.formatDouble(bodyDiam, units));
            WMLUtil.formatDouble(bodyToTop, units));
      }
      
      /** Return a deep copy of this object. */
      public ToneHole copy() {
         // First make a shallow copy:
         ToneHole copy = null;
         try {
            copy = (ToneHole) this.clone();
         } catch (CloneNotSupportedException e) {}
         // now deep copy any mutable objects:
         return copy;
      }      
      
      @Override
      public boolean equals(Object o) {
         return WMLUtil.equals(this, o);
      }
      
   }
      
   

}