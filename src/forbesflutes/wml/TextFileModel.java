/**
 *  Author: Rob Forbes Nov 2024
 */
package forbesflutes.wml;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** A model (as in MVC) for a text file (e.g. for an editor). It can be used
 *  either for in-memory text or can be associated with a file. */
public class TextFileModel {
   public static interface TextModelListener {
      public void modelChanged();
   }
   
   protected String text = "";
   private String filePath; // nullable
   private boolean isDirty = false; // indicates if latest text has not been saved to a file
   private List<TextModelListener> listeners = new ArrayList<TextModelListener>();
   
   public TextFileModel() {
   }
   
   /** Add a change listener. */
   public void addListener(TextModelListener listener) {
      listeners.add(listener);
   }
   
   protected void notifyListeners() {
      for(TextModelListener listener: listeners)
         listener.modelChanged();
   }
   
   /** Returns if there is no meaningful text content. */
   public boolean isEmpty() {
      return text.equals("");
   }
   
   /** Returns if the text is newer than the file contents. */
   public boolean isDirty() {return isDirty;}
      
   /** Returns the name of the current file. */
   public String getFileName() {
      return Path.of(filePath).getFileName().toString();
   }
   
   /** Returns the path of the current file. */
   public String getFilePath() {return filePath;}
   
   /** Returns the current in-memory text. */
   public String getText() {return text;}
   
   /** File New action: Re-initialize with a blank string; remove all file associations. */
   public void reset() {
      text = "";
      filePath = null;
      isDirty = false;
      notifyListeners();
   }
   
   /** Set the in-memory text. */
   public void setText(String text) {
      this.text = text;
      isDirty = true;   // we assume this text is different than any file contents
      notifyListeners() ;
   }

   /** Open a new file and make it the current file. */
   public void open(String filePath) throws IOException {
      this.filePath = filePath;
      text = Files.readString(Path.of(filePath));
      isDirty = false;  
      notifyListeners();
   }
   
   /** Re-open the current file. Any current changes are lost. Throws if there is no current file.
    * @throws IOException */
   public void refresh() throws IOException {
      if(filePath == null) throw new IOException("No filepath is defined.");
      open(filePath);      
   }
   
   /** Save to the current file; throws if no current file is defined. */
   public void save() throws IOException {
      if(filePath == null) throw new IOException("No filepath is defined; use Save As");
      BufferedWriter out = new BufferedWriter(new FileWriter(filePath, false));
      out.write(text);
      out.close();
      isDirty = false;
   }
   
   /** Save to the given file, and make that the current file. */
   public void saveAs(String filePath) throws IOException {
      this.filePath = filePath;
      save();
   }
      
}
