package forbesflutes.wml;

import java.util.ArrayList;
import java.util.List;

/** A tokenizer that (unlike java.util.StringTokenizer) will handle double-quoted strings. 
 *  delimiters are space and tab. */
public class StringTokenizer {
   private List<String> tokens = new ArrayList<String>();
   public static final String delimiters = " \t";
   private String line;
   
   public StringTokenizer(String line) {
      this.line = line;
      StringBuilder stringBuilder = new StringBuilder();
      boolean inQuote = false;

      for (char c : line.toCharArray()) {
          if (c == '"') {
              inQuote = !inQuote;
          } else if (delimiters.indexOf(c) >= 0 && !inQuote) {
              if(stringBuilder.length() > 0) {
                 tokens.add(stringBuilder.toString());
                 stringBuilder = new StringBuilder();
              }
          } else {
              stringBuilder.append(c);
          }
      }
      if(stringBuilder.length() > 0) 
         tokens.add(stringBuilder.toString());
   }
   
   public String getLine() {
      return line;
   }
   
   public List<String> getTokens() {
      return tokens;
   }
   
}