package forbesflutes.wml;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import forbesflutes.wml.WMLData.Section;
import forbesflutes.wml.WMLData.Units;

public class WMLUtil {
   
   /** Convert list of Doubles to an array of primitives. */
   public static double[] toDoubleArray(List<Double> list) {
      double[] array = new double[list.size()];
      for(int i = 0; i < array.length; i++)
         array[i] = list.get(i);
      return array;
   }
   
   /** Convert list of Integers to an array of primitives. */
   public static int[] toIntArray(List<Integer> list) {
      int[] array = new int[list.size()];
      for(int i = 0; i < array.length; i++)
         array[i] = list.get(i);
      return array;
   }
   
   /** Format a float value to an approprate level of precision for the given units:
    *  .01 mm or .001 inch. Remove leading/trailing zeros.
    */
   public static String formatDouble(double d, Units units) {
      if(d == 0.) return "0.";
      // Remove leading and trailing zeros
      String formatString = (units == Units.MM) ? "%.2f" : "%.3f";
      String str = String.format(formatString, d);
      int start = 0;
      int end = str.length();
      for(int i = 0; i < str.length(); i++)
         if(str.charAt(i) == '0') ++start;
         else break;
      for(int i = str.length() -  1; i > 0; i--)
         if(str.charAt(i) == '0') --end;
         else break;
      return str.substring(start, end);
   }
   
   /** Check two lists for equality, including null checks. */
   public static boolean listsAreEqual(List l1, List l2) {
      if(l1 == null && l2 == null) return true;  // both are null
      if(l1 == null || l2 == null) return false; // only one is null
      // Java implements a list equals that checks for same # of elements and value equality
      return l1.equals(l2);
   }
   
   /** Check that two objects are equal. Doesn't check everything, but is sufficient
    *  for the WMLData classes. Not very efficient, as it uses reflection, but it
    *  is very convenient. */
   public static boolean equals(Object o1, Object o2) {
      if(o1 == null && o2 == null) return true;  // both are null
      if(o1 == null || o2 == null) return false;  // only one is null
      if(o1.getClass() != o2.getClass()) return false;
      for(Field field: o1.getClass().getFields()) {
          //Class type = field.getType();
          try {
            Object val1 = field.get(o1);
            Object val2 = field.get(o2);
            if(!Objects.equals(val1, val2)) {
               System.out.printf("%s.%s %s != %s\n", o1.getClass().getSimpleName(), field.getName(), String.valueOf(val1), String.valueOf(val2));
               return false;
            }
          }
          catch(Exception e) {e.printStackTrace();}
      }
      return true;
   }
}
