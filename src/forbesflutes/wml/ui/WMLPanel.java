/**
 *  Author: Rob Forbes Nov 2024
 */
package forbesflutes.wml.ui;

/** A panel in the main tabbed pane. Defines actions panels must handle, and interfaces they must implement. */
public interface WMLPanel  {
   
   void tabOpened();

   void tabClosed();
   
   void doSaveImage();
   
   void doPrint();
   
   void onExit();

}
