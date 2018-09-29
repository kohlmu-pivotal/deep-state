package metatype.deepstate.example;

import java.time.Duration;

/**
 * This is a toaster.  You can make toast.  It has a dial to control doneness.  It also has a
 * button for bagels and a button to cancel toasting.  Sometimes it may jam and catch on fire. 
 */
public interface Toaster {
  /**
   * Settings for the toaster dial.  High numbers mean the toast will be cooked longer.  Goes
   * to 11.
   */
  enum Dial {
    ONE(Duration.ofSeconds(0)), 
    TWO(Duration.ofSeconds(15)), 
    THREE(Duration.ofSeconds(30)), 
    FOUR(Duration.ofSeconds(45)), 
    FIVE(Duration.ofSeconds(60)), 
    SIX(Duration.ofSeconds(75)), 
    SEVEN(Duration.ofSeconds(90)), 
    EIGHT(Duration.ofSeconds(105)), 
    NINE(Duration.ofSeconds(120)), 
    TEN(Duration.ofSeconds(135)), 
    ELEVEN(Duration.ofSeconds(1000));
    
    private final Duration toastingTime;
    
    private Dial(Duration time) {
      this.toastingTime = time;
    }
    
    /**
     * The cooking duration corresponding to the dial setting.
     * @return the duration
     */
    public Duration getToastingTime() {
      return toastingTime;
    }
  }

  /**
   * Returns true when the toast is popped up.
   * @return true if done
   */
  boolean isToasting();
  
  /**
   * Returns true when the toaster is on fire.
   * @return true if burning
   */
  boolean isBurning();

  /**
   * Starts toasting any items in the toaster.
   */
  void depressLever();
  
  /**
   * Press the bagel button to let the toaster know there's a bagel being toasted.
   */
  void pressBagelButton();

  /**
   * Press the cancel button to stop toasting.
   */
  void pressCancel();

  /**
   * Indicate that the item is jammed in the toaster.
   */
  void jamLever();
  
  /**
   * Change the dial on the toaster to control the degree of doneness.
   * @param setting the setting
   */
  void changeToasterSetting(Dial setting);
  
  /**
   * Returns the current setting of the toaster dial.
   * @return the setting
   */
  Dial getToasterSetting();
}
