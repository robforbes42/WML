/**
 *  Author: Rob Forbes Nov 2024
 */
package forbesflutes.wml.ui;

import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import forbesflutes.wml.TextFileModel;

/** A generic text editor. */
public class TextEditorPanel extends LineNumberedTextPane {
   private static final long serialVersionUID = 1L;
   protected final TextFileModel model;
   protected boolean textDirty = false;

   public TextEditorPanel(TextFileModel model) {
      this.model = model;
      textArea.getDocument().addDocumentListener(new DocumentListener() {
         public void insertUpdate(DocumentEvent e) {textDirty = true;}
         public void removeUpdate(DocumentEvent e) {textDirty = true;}
         public void changedUpdate(DocumentEvent e) {textDirty = true;}
      });
   }

   public void syncWithModel() {
      if(textDirty) {
         model.setText(textArea.getText());
         textDirty = false;
      }
   }

   private void handleError(String msg, Exception e) {
      JOptionPane.showMessageDialog(this, msg + ": " + e.getMessage(), "", JOptionPane.ERROR_MESSAGE);
   }
   
   /** Give the user a chance to save changes before deleting them. 
    * @throws IOException */
   protected void checkUnsavedChanges() {
      syncWithModel();
      if(model.isDirty()) {
         int response = JOptionPane.showConfirmDialog(this,
            "You have unsaved changes. Do you want to save them?");
         if(response == JOptionPane.YES_OPTION)
            try {model.save();} catch (IOException e) {handleError("Error saving file " + model.getFileName(), e);}
      }      
   }

   public void filePrint() {
      try {
         textArea.print();
      } catch (PrinterException e) {
         handleError("Error printing file " + model.getFilePath(), e);
      }
   }

   public void fileSave() {
      syncWithModel();
      try {model.save();} catch (IOException e) {handleError("Couldn't save file " + model.getFilePath(), e);}

   }

   public void fileSaveAs() {
      syncWithModel();
      JFileChooser chooser = new JFileChooser();
      if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
         File filepath = new File(chooser.getSelectedFile().getAbsolutePath());
         try {model.saveAs(filepath.toString());} catch (IOException e) {handleError("Couldn't save file as " + filepath, e);}
      }
   }

   public void fileNew() {
      checkUnsavedChanges();
      model.reset();
      textArea.setText(model.getText());
      textDirty = false;
   }

   public void fileOpen() {
      checkUnsavedChanges();
      JFileChooser chooser = new JFileChooser();
      if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
         String filePath = chooser.getSelectedFile().getAbsolutePath();
         openFile(filePath);
      }
   }

   void openFile(String filePath) {
      try {model.open(filePath);} catch (IOException e) {handleError("Couldn't open file " + filePath, e);}
      textArea.setText(model.getText());
      textDirty = false;
   }

   public void fileRefresh() {
      checkUnsavedChanges();
      try {model.refresh();} catch (IOException e) {handleError("Couldn't refresh file " + model.getFilePath(), e);}
      textArea.setText(model.getText());
      textDirty = false;
   }

   public void fileExit() {
      checkUnsavedChanges();
   }

   public void print() {
      try {textArea.print();} catch (PrinterException e) {handleError("Print error", e);}
   }

   public void cut() {
      textArea.cut();
   }
   
   public void copy() {
      textArea.copy();
   }
   
   public void paste() {
      textArea.paste();
   }

   void tabOpened() {}
   
   void tabClosed() {syncWithModel();}

   void onExit() {
      checkUnsavedChanges();
   }

}
