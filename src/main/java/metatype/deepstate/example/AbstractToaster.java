package metatype.deepstate.example;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The beginnings of a toaster.  Extract some common bits so it's easier to see the differences
 * between the types of implementations.
 */
public abstract class AbstractToaster implements Toaster {
  /** schedules asynchronous toasting events */
  protected final Timer timer;
  
  /** time before the toaster catches on fire */
  protected final Duration autoIgnitionDuration;
  
  /** triggers when the toast is done */
  protected TimerTask popup;
  
  /** triggers when the toaster catches fire */
  protected TimerTask ignite;
  
  /** controls how long the toaster will cook */
  protected Dial setting;
  
  /** if true, prevents the toaster lever from raising when done */
  protected boolean jammed;
  
  /** incremented when an item is toasted */
  protected int dutyCycle;

  public AbstractToaster(Duration autoIgnitionDuration) {
    this.timer = new Timer();
    this.autoIgnitionDuration = autoIgnitionDuration;
    this.setting = Dial.FIVE;
  }
}
