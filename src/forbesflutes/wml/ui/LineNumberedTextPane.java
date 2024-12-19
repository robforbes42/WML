/**
 *  Author: Rob Forbes Nov 2024
 */
package forbesflutes.wml.ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

public class LineNumberedTextPane extends JPanel {
   private static final long serialVersionUID = 1L;
   protected JTextArea textArea;
   private JTextArea lineNumberArea;

   public LineNumberedTextPane() {
      setLayout(new BorderLayout());
      textArea = new JTextArea();
      lineNumberArea = new JTextArea("1 ");
      lineNumberArea.setEditable(false);
      JScrollPane scrollPane = new JScrollPane(textArea);
      add(scrollPane, BorderLayout.CENTER);

      JScrollPane lineNumberScrollPane = new JScrollPane(lineNumberArea);
      lineNumberScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
      add(lineNumberScrollPane, BorderLayout.WEST);

      // Synchronize the line number scrolling with the edit panel:
      scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
         @Override
         public void adjustmentValueChanged(AdjustmentEvent e) {
            lineNumberScrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getValue());
         }
      });
      // add line numbers when needed:
      textArea.getDocument().addDocumentListener(new DocumentListener() {
         public void insertUpdate(DocumentEvent e) {updateLineNumbers();}
         public void removeUpdate(DocumentEvent e) {updateLineNumbers();}
         public void changedUpdate(DocumentEvent e) {updateLineNumbers();}
      });
   }

   public JTextArea getTextArea() {return textArea;}

   private void updateLineNumbers() {
      try {
         int lineCount = textArea.getDocument().getDefaultRootElement().getElementCount();
         StringBuilder builder = new StringBuilder();
         for (int i = 1; i <= lineCount; i++) {
            builder.append(i).append("\n");
         }
         lineNumberArea.setText(builder.toString());
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public static void main(String[] args) {
      JFrame frame = new JFrame("Line Numbered Text Pane");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setContentPane(new LineNumberedTextPane());
      frame.pack();
      frame.setVisible(true);
   }
}