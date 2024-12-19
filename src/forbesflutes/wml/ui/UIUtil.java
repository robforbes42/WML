/**
 *  Author: Rob Forbes Nov 2024
 */

package forbesflutes.wml.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Stroke;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.horstmann.corejava.GBC;

import forbesflutes.wml.WMLData;
import forbesflutes.wml.WMLData.Units;

public class UIUtil {
   public static final Color background = new Color(.95f, .95f, .95f);
   public static final Dimension preferredSize = new Dimension(50, 20);
   public static Stroke dashedStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 
         0, new float[] {3,3}, 0);
     public static final int bigInset = 8;
     public static final int smallInset = 2;
     public static final Insets smallInsets = new Insets(smallInset, smallInset, smallInset, smallInset);
     public static final Insets bigInsets = new Insets(bigInset, bigInset, bigInset, bigInset);
   
   public static JTextField createTextField(int numColumns) {
      JTextField textField = new JTextField(numColumns);
      //textField.setPreferredSize(preferredSize);
      textField.setBackground(background);
      return textField;
   }
   
   public static JComboBox<String> createComboBox(String[] choices) {
      JComboBox<String> comboBox = new JComboBox<String>(choices);
      //comboBox.setPreferredSize(preferredSize);
      comboBox.setBackground(background);
      return comboBox;
   }

   public static String formatDiameter(double d, Units units) {
      String svalue = WMLData.formatDouble(d, units);
      return String.format("\u2300%s", svalue);
   }

}
