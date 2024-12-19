/**
 * Parser for Woodwind Measurement Format (.WML)
 * 
 * Author: Rob Forbes Nov 2024
 */
package forbesflutes.wml;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import forbesflutes.wml.WMLData.Section;
import forbesflutes.wml.WMLData.Units;
import forbesflutes.wml.WMLData.Joint.TenonDirection;
import forbesflutes.boreptb.core.XYCurve;

/** Parses a WML file and creates WMLData. 
 *  CAUTION: WMLData does not hold any comments that may be in the file. */
public class WMLParser {
   
   /** This should be thrown when any error is encountered in parsing. 
    *  Handling is tied into the error logging mechanism. */
   protected static class WMLParseException extends Exception {
      protected static final long serialVersionUID = 1L;

      public WMLParseException(String msg) {
         super(msg);
      }
   }
   
   // line types (first token of line):
   public static final String POUND = "#";
   public static final String id = "id";
   public static final String name = "name";
   public static final String type = "type";
   public static final String subType = "subtype";
   public static final String part = "part";
   public static final String section = "section";
   public static final String owner = "owner";
   public static final String maker = "maker";
   public static final String serial = "serial"; 
   public static final String submittedBy = "submittedby";
   public static final String measuredBy = "measuredby"; 
   public static final String url = "url";
   public static final String pitchStandard = "pitchstandard";
   public static final String keyOf = "keyof";
   public static final String units = "units";
   public static final String toneHole = "tonehole";
   public static final String blowHole = "blowhole";
   public static final String joint = "joint";
   public static final String noteRange = "noterange";
   public static final String bore = "bore";
   public static final String body = "body";     
   
   // other constants
   public static final String DEFAULT_PART_NAME = "default";     
   
   // parsing state:
   protected String wimText;
   protected List<String> messages = new ArrayList<String>();
   protected String line;
   protected int lineNum = 0;
   protected List<String> tokens;
   protected List<Double> xCoords = new ArrayList<Double>();
   protected List<Double> yCoords = new ArrayList<Double>();
   protected boolean inBore = false;
   protected boolean inBody = false;
   protected WMLData data = new WMLData();
   protected WMLData.Part currentPart;
   protected WMLData.Section currentSection;
   protected WMLData.Joint currentJoint;
   protected boolean dataIsValid = false;  // indicates last parse succeeded and data is present + valid
   
   /** Get all the messages from parsing. This may include errors, warnings, and suggestions. */
   public List<String> getMessages() {
      return messages;
   }
   
   /** Get all the messages as a single string. */
   public String getAllMessages() {
      ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
      PrintStream out = new PrintStream(byteStream);
      for(String line: messages)
         out.println(line);
      String allMessages = byteStream.toString();
      out.close();
      return allMessages;
   }

   /** Clear the state to prepare for parsing another file. */
   public void reset() {
      wimText = null;
      lineNum = 0;
      data = new WMLData();
      messages.clear();      
      dataIsValid = false;
   }
   
   /** Returns if the parse succeeded and data was created. */
   public boolean dataIsValid() {
      return dataIsValid;
   }

   /** Get the data from the last parse. */
   public WMLData getData() {
      return data;
   };
   
   /** Utility method that formats the error message and adds the line number. */
   protected void throwParseError(String msg) throws WMLParseException {
      throw new WMLParseException(msg);
   }

   /** Add the line number to the error message. */
   protected String formatErrorMessage(String msg) {
      return String.format("Error on line %d: %s", lineNum, msg);
   }
   
   /** Parse a file. */
   public boolean parseFile(String filePath) throws IOException {
      data.filepath = filePath;
      String fileText = Files.readString(Path.of(filePath));
      return parse(fileText);
   }
   
   /** Parse in-memory text.  */
   public boolean parse(String wimText) throws IOException {
      System.out.println("Parsing...");
      reset();
      this.wimText = wimText;
      dataIsValid = true;
      BufferedReader in = new BufferedReader(new StringReader(wimText));
      while ((line = in.readLine()) != null) {
         ++lineNum;
         try {
            parseLine(line);
         }
         catch(WMLParseException pe) {
            processParseError(pe.getMessage());
         }
         catch(Exception e) {
            e.printStackTrace();
         }
      }
      in.close();
      endFile();
      if(dataIsValid)
         messages.add("Check succeeded; file is valid.");
      else
         messages.add("Check failed; errors were found.");
      return dataIsValid;
   }

   /** Take appropriate actions in response to a parse error. */
   private void processParseError(String errMsg) {
      dataIsValid = false;
      messages.add(formatErrorMessage(errMsg));
   }

   /** Break a non-comment line into tokens. */
   protected static List<String> tokenize(String line) {
      line = line.trim();
      int poundIndex = line.indexOf(POUND);
      if(poundIndex >= 0) 
          line = line.substring(0, poundIndex);
      StringTokenizer tokenizer = new StringTokenizer(line);
      return tokenizer.getTokens();
   }

   /** Parse a single line. */
   protected void parseLine(String line) throws WMLParseException {
      tokens = tokenize(line);
      if(tokens.size() == 0) return;
      String tag = tokens.get(0).toLowerCase();
      if(currentPart == null && !tag.equals(part))
          addPart(DEFAULT_PART_NAME); // create a default part
      if(isNumeric(tag)) {
         if(inBore || inBody)
            parseXY();
         else  
            throwParseError("Numeric line out of context: " + tag);
      }
      else {
         endXYSequence();
         switch(tag) {
            case id: data.id = getTextToken(1); break;
            case name: data.name = getTextToken(1); break;
            case type: data.type = getTextToken(1); break;
            case subType: data.subType = getTextToken(1); break;
            case serial: data.serial = getTextToken(1); break;
            case maker: data.maker = getTextToken(1); break;
            case owner: data.owner = getTextToken(1); break;
            case submittedBy: data.submittedBy = getTextToken(1); break;
            case measuredBy: data.measuredBy = getTextToken(1); break;
            case url: data.url = getTextToken(1); break;
            case pitchStandard: data.pitchStandard = getDoubleToken(1); break;
            case keyOf: data.keyOf = getTextToken(1); break;
            case units: parseUnits(); break;
            case noteRange: parseNoteRange(); break;            
            case section: parseSection(); break;
            case part: parsePart(); break;
            case toneHole: parseToneHole(); break;
            case blowHole: parseBlowHole(); break;
            case joint: parseJoint(); break;
            case bore: inBore = true; break;
            case body: inBody = true; break;
            default: throwParseError("Unknown tag " + tag);
         }
      }
   }

   /** Parse a pair of doubles, e.g. a point in a bore or body profile. */
   protected void parseXY() throws WMLParseException {
      xCoords.add(Double.valueOf(getDoubleToken(0)));
      yCoords.add(Double.valueOf(getDoubleToken(1)));      
   }

   /** Return if the string looks like a number. */
   protected static boolean isNumeric(String tag) {
      if(tag.isEmpty()) return false;
      char firstChar = tag.charAt(0);
      return Character.isDigit(firstChar) || firstChar == '.';
   }

   /** Check that a string is a legal note name (e.g. "C4" or "Bb6") and throw if it isn't. */
   protected void assertLegalNote(String note) throws WMLParseException {
      if(note.length() < 2 || note.length() > 3)
         throwParseError("Note name must be two or three characters, e.g. C4 or Bb5");
      char noteName = Character.toLowerCase(note.charAt(0));
      if(noteName < 'a' || noteName > 'g')
         throwParseError("Illegal note: " + noteName);
      if(note.length() == 3) {
         char accidental = note.charAt(1);
         if(!(accidental == '#' || accidental == 'b'))
            throwParseError("Illegal accidental: " + accidental);
      }
      char octaveChar = note.charAt(note.length() - 1);
      if(octaveChar < '0' || octaveChar > '9')
         throwParseError("Illegal octave number: " + octaveChar);      
   }
   
   /** Check that a section has been defined and throw if it hasn't. */
   private void assertSection() throws WMLParseException {
      if(currentSection == null)
         throwParseError("section must appear before " + tokens.get(0));
   }
   
   /** Parse a floating-point value from the token list. 
    *  Throws if string is missing or an invalid format. */
   protected double getDoubleToken(int index) throws WMLParseException {
      if(tokens.size() <= index)
         throwParseError(String.format("Missing value for %s", tokens.get(0)));
      String sValue = tokens.get(index);
      double dValue = 0.;
      try {
         dValue = Double.parseDouble(sValue);
      }
      catch(NumberFormatException e) {
         throwParseError(String.format("Illegal floating point format: %s", sValue));
      }
      return dValue;
   }

   /** Get a text token from the token list. */
   protected String getTextToken(int index) throws WMLParseException {
      if(tokens.size() > index) {
         return tokens.get(1);
      }
      else {
         throwParseError(String.format("No value for %s", tokens.get(0)));
         return null; // fake return for the compiler
      }
   }
   
   /** Take necessary actions on the end of file. 
    * @throws WMLParseException */
   protected void endFile() {
      endXYSequence();
      ++lineNum;
      endPart();  // this will call endSection
   }
   
   /** If an XY sequence is in progress, end it. */
   protected void endXYSequence() {
      if(inBore) endBore();
      if(inBody) endBody();
   }

   /** End anything in progress for a bore definition.  */
   protected void endBore()  {
      if(currentSection != null && inBore) {
         currentSection.boreCurve = new XYCurve(WMLUtil.toDoubleArray(xCoords), WMLUtil.toDoubleArray(yCoords));
         xCoords = new ArrayList<Double>();
         yCoords = new ArrayList<Double>();
      }
      inBore = false;
   }

   /** End anything in progress for a body definition. */
   protected void endBody() {
      if(currentSection != null && inBody) {
         currentSection.bodyCurve = new XYCurve(WMLUtil.toDoubleArray(xCoords), WMLUtil.toDoubleArray(yCoords));
         xCoords = new ArrayList<Double>();
         yCoords = new ArrayList<Double>();
      }
      inBody = false;
   }

   /** End anything in progress for a part definition. */
   protected void endPart() {
      endSection();
      currentPart.endPart();
      currentPart = null;
   }
      
   /** End anything in progress for a section definition. 
    * @throws WMLParseException */
   protected void endSection() {      
      currentSection = null;
   }
   
   /** Create a new Part object. */
   protected void addPart(String name) {
      currentPart = new WMLData.Part();
      data.parts.add(currentPart);
      currentPart.name = name;
   }

   /** Parse the line type included in the method name. */
   protected void parsePart() throws WMLParseException {
      String name = tokens.size() > 1 ? name = getTextToken(1) : null;
      addPart(name);
   }

   /** Parse the line type included in the method name. */
   protected void parseSection() throws WMLParseException {
      if(currentSection != null && currentJoint == null) {
         throwParseError("No joint defined between sections");
         endSection();
      }
      currentSection = new WMLData.Section();
      currentSection.topJoint = currentJoint;
      currentJoint = null;
      currentPart.sections.add(currentSection);
      currentSection.name = getTextToken(1);
      currentSection.length = getDoubleToken(2);
   }

   /** Parse the line type included in the method name. */
   protected void parseNoteRange() throws WMLParseException {
      String startNote = getTextToken(1);
      assertLegalNote(startNote);
      currentPart.noteRangeStart = startNote;
      
      String endNote = getTextToken(2);
      assertLegalNote(endNote);
      currentPart.noteRangeEnd = endNote;
   }

   /** Parse the line type included in the method name. */
   protected void parseUnits() throws WMLParseException {
      String unitName = getTextToken(1).toUpperCase();
      if(unitName.equals(Units.MM.toString()))
         data.units = Units.MM;
      else if(unitName.equals(Units.INCH.toString()))
         data.units = Units.INCH;
      else
         throwParseError("Unknown value for units: " + unitName);
   }
   
   /** Get the most recently added section. */
   protected Section getMostRecentSection() {
      if(currentPart.sections.size() == 0)
         return null;
      else
         return currentPart.sections.get(currentPart.sections.size() - 1);
   }
   
   /** Parse/validate a tenonDirection field. */
   protected TenonDirection getTenonDirection(String str) throws WMLParseException {
      try {
         TenonDirection dir = TenonDirection.valueOf(str.toUpperCase());
         return dir;
      }
      catch(IllegalArgumentException e) {
         throwParseError("Unknown tenon direction: " + str);
         return null; // fake for compiler
      }
   }
   
   /** Parse the line type included in the method name. */
   protected void parseJoint() throws WMLParseException {
      if(currentPart.sections.size() == 0)
         throwParseError("No section for joint");      
      WMLData.Joint joint = new WMLData.Joint();
      joint.tenonDirection = getTenonDirection(getTextToken(1));
      joint.tenonLength = getDoubleToken(2);
      joint.tenonDiam = getDoubleToken(3);
      if(tokens.size() > 4)
         joint.extension = getDoubleToken(4);
      currentJoint = joint;
   }

   /** Parse the line type included in the method name. */
   protected void parseToneHole() throws WMLParseException {
      assertSection();
      double xCoord = getDoubleToken(1);
      double diam = getDoubleToken(2);
      double bodyDiam = 0.;
      if(tokens.size() > 3)
         bodyDiam = getDoubleToken(3);
      currentSection.toneHoles.add(new WMLData.ToneHole(xCoord, diam, bodyDiam));
   }

   /** Parse the line type included in the method name. */
   protected void parseBlowHole() throws WMLParseException {
      assertSection();
      double xCoord = getDoubleToken(1);
      double length = getDoubleToken(2);
      double width = getDoubleToken(3);
      double bodyDiam = 0.; 
      if(tokens.size() > 4)
         bodyDiam = getDoubleToken(4);
      double corkDist = 0.;
      if(tokens.size() > 5)
         corkDist = getDoubleToken(5);
      currentSection.blowHole = new WMLData.BlowHole(xCoord, length, width, bodyDiam, corkDist);
   }

   /** Bulk unit conversion for a file. Preserves comments. */
   public static void convertUnits(File oldFile, File newFile) throws IOException {
      // Read in the file and check units
      BufferedReader in = new BufferedReader(new InputStreamReader(
         new FileInputStream(oldFile), "UTF-8"));
      List<String> lines = new ArrayList<String>();
      boolean unitsOK = false;
      String fileLine;
      Units existingUnits = null;
      while((fileLine = in.readLine()) != null) {
         lines.add(fileLine);
         if(fileLine.startsWith(units)) {
            StringTokenizer tokenizer = new StringTokenizer(fileLine);
            if(tokenizer.getTokens().size() > 1) {
               try {
                  existingUnits = Units.valueOf(tokenizer.getTokens().get(1).toUpperCase());
               } catch(Exception e) {}              
                  unitsOK = true;
               }
         }
      }
      in.close();
      if(!unitsOK) {
         System.out.println("Conversion cannot be done; units are missing or already in requested units.");
         return;
      }
      Units newUnits = (existingUnits == Units.MM) ? Units.INCH : Units.MM;
      // convert and write out line-by-line
      double conversionFactor = WMLData.getConversionFactor(newUnits);
      PrintWriter out = new PrintWriter(new FileWriter(newFile));
      for(String line: lines) {
         if(line.startsWith(units)) {
            out.printf("%s %s\n", units, newUnits.toString());
         }
         else {
            List<String> tokens = tokenize(line);
            for(String token: tokens) {
               if(isNumeric(token)) {
                  double oldValue = Double.valueOf(token);
                  String svalue = WMLUtil.formatDouble(oldValue * conversionFactor, newUnits);
                  out.printf("%s\t", svalue);
               }
               else {
                  out.printf("%s\t", token);
               }
            }
            out.println();
         }
      }
      out.close();
   }
   
}
