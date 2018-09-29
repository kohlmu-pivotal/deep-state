package metatype.deepstate.example;

import java.time.Duration;
import java.util.Random;
import java.util.TimerTask;

/**
 * A toaster implemented using flags and synchronization of mutable state.
 */
public class ConcurrentToaster extends AbstractToaster {
  /** if true, the toaster is on */
  private boolean toasting;
  
  /** if true, the toaster is on fire */
  private boolean burning;
  
  /** if true, the toaster is cooking in bagel mode */
  private boolean bagel;

  public ConcurrentToaster() {
    this(Duration.ofMinutes(5));
  }
  
  public ConcurrentToaster(Duration autoIgniteDuration) {
    super(autoIgniteDuration);
  }
  
  @Override
  public synchronized boolean isToasting() {
    return toasting;
  }
  
  @Override
  public synchronized boolean isBurning() {
    return burning;
  }
  
  @Override
  public synchronized Dial getToasterSetting() {
    return setting;
  }

  @Override
  public synchronized void depressLever() {
    if (toasting || burning) {
      return;
    }
    
    toasting = true;
    
    maybeJam();
    schedulePopup();
    scheduleIgnite();
  }

  @Override
  public synchronized void pressBagelButton() {
    if (toasting) {
      bagel = !bagel;
      if (bagel) {
        turnOnOneHeatingElement();
      } else {
        turnOnBothHeatingElements();
      }
    }
  }

  @Override
  public synchronized void pressCancel() {
    if (toasting && !jammed) {
      popup.cancel();
      powerOff();
    }
  }
  
  private void schedulePopup() {
    popup = new TimerTask() {
      @Override
      public void run() {
        synchronized (ConcurrentToaster.this) {
          if (toasting && !jammed) {
            powerOff();
          }
        }
      }
    };
    timer.schedule(popup, setting.getToastingTime().toMillis());
  }

  @Override
  public synchronized void jamLever() {
    jammed = true;
  }
  
  @Override
  public synchronized void changeToasterSetting(Dial setting) {
    this.setting = setting;
    if (toasting) {
      popup.cancel();
      schedulePopup();
    }
  }

  private void scheduleIgnite() {
    ignite = new TimerTask() {
      @Override
      public void run() {
        synchronized (ConcurrentToaster.this) {
          catchFire();
        }
      }
    };
    timer.schedule(ignite, autoIgnitionDuration.toMillis());
  }

  private void maybeJam() {
    if (dutyCycle > 1000 && new Random().nextBoolean()) {
      jammed = true;
    }
  }

  private void turnOnOneHeatingElement() {
    System.out.println("Toasting one side");
  }

  private void turnOnBothHeatingElements() {
    System.out.println("Toasting both sides");
  }

  private void powerOff() {
    ignite.cancel();
    toasting = false;
    System.out.println("Toast is done!");
    dutyCycle++;
  }
  
  private void catchFire() {
    toasting = false;
    burning = true;
    System.out.println("Your toaster is on fire and the smoke alarm is going off!");
  }
}
