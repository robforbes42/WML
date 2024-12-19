/**
 *  Author: Rob Forbes Nov 2024
 */
package forbesflutes.wml;

import java.io.IOException;

import forbesflutes.wml.WMLData.Units;

/** An editor model for WML files. It adds WML file parsing/validation to 
 *  the basic TextFileModel. */
public class WMLFileModel extends TextFileModel {
   protected WMLParser parser;
   
   public WMLFileModel() {
      parser = new WMLParser();
   }
   
   public WMLParser getParser() {return parser;}
   
   /** Returns if the text has been successfully parsed. */
   public boolean isValid() {return parser.dataIsValid();}

   /** Returns the parsed WML Data; may be null if the parse failed. */
   public WMLData getData() {return parser.getData();}
   
   /** Get the unit of the file. Returns null if the file is not valid. */
   public Units getUnits() {
      return isValid() ? getData().units : null;
   }
   
   @Override
   /** Clear the model state. */
   public void reset() {
      parser.reset();
      super.reset();
   }

   /** Parse the in-memory text. */
   public boolean parse() {
      try {
         parser.parse(text);
         return parser.dataIsValid();
      } catch (IOException e) {
         return false;
      }
   }
   
   @Override
   protected void notifyListeners() {
      // override the superclass method so any content change is parsed before listeners are notified.
      if(!isEmpty())
         parse();
      super.notifyListeners();
   }
}
