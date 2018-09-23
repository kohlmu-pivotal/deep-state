package metatype.deepstate.example;

import java.time.Duration;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import metatype.deepstate.DeepState;
import metatype.deepstate.DeepState.StateFactory;
import metatype.deepstate.DeepStateEvent;
import metatype.deepstate.FiniteStateMachine;
import metatype.deepstate.FiniteStateMachine.Event;
import metatype.deepstate.FiniteStateMachine.State;
import metatype.deepstate.FiniteStateMachine.Transition;

/**
 * This is a toaster.  You can make toast.  It has a dial to control doneness.  It also has a
 * button for bagels and a button to cancel toasting.  Sometimes it may jam and catch on fire. 
 */
public class Toaster {
  /**
   * Settings for the toaster dial.  High numbers mean the toast will be cooked longer.  Goes
   * to 11.
   */
  public enum ToasterDial {
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
    
    private ToasterDial(Duration time) {
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
   * Defines the available toaster states.
   */
  private enum ToasterStates {
    TOASTER, OFF, TOASTING, NORMAL_TOASTING, HALF_TOASTING, ON_FIRE
  }

  /**
   * Defines the available toaster events.
   */
  private enum ToasterTriggers {
    DIAL_CHANGED, BAGEL_BUTTON_PRESSED, CANCEL_BUTTON_PRESSED, LEVER_DEPRESSED, TIMER_EXPIRED, DIAL_VIEWED, CATCH_FIRE, LEVER_JAMMED
  }
  
  /**
   * A generic toasting event.
   */
  private static class ToasterEvent extends DeepStateEvent<ToasterTriggers, Object> {
    public ToasterEvent(ToasterTriggers trigger) {
      super(trigger);
    }
  }
  
  /**
   * An event that changes the toaster dial setting.
   */
  private static class SetDialEvent extends DeepStateEvent<ToasterTriggers, ToasterDial> {
    public SetDialEvent(ToasterTriggers trigger, ToasterDial setting) {
      super(trigger, setting);
    }
  }
  
  /**
   * An event that provides provides the toaster dial setting to a consumer. 
   */
  private static class GetDialEvent extends DeepStateEvent<ToasterTriggers, Consumer<ToasterDial>> {
    public GetDialEvent(ToasterTriggers trigger, Consumer<ToasterDial> setting) {
      super(trigger, setting);
    }
  }

  /** finite state machine for the toaster */
  private final FiniteStateMachine<ToasterTriggers, ToasterStates> toaster;

  /** schedules asynchronous toasting events */
  private final Timer timer;
  
  /** time before the toaster catches on fire */
  private final Duration autoIgnitionDuration;
  
  /** triggers when the toast is done */
  private TimerTask popup;
  
  /** triggers when the toaster catches fire */
  private TimerTask ignite;
  
  /** controls how long the toaster will cook */
  private ToasterDial setting;
  
  /** if true, prevents the toaster lever from raising when done */
  private boolean jammed;
  
  /** incremented when an item is toasted */
  private int dutyCycle;
  
  public Toaster() {
    this(Duration.ofMinutes(5));
  }
  
  public Toaster(Duration autoIgnitionDuration) {
    this.timer = new Timer();
    this.autoIgnitionDuration = autoIgnitionDuration;
    this.setting = ToasterDial.FIVE;
    
    this.toaster = DeepState.<ToasterTriggers, ToasterStates>model()
        .startingWith(ToasterStates.TOASTER)
        .when(ToasterTriggers.DIAL_CHANGED, this::changeSetting)
        .when(ToasterTriggers.DIAL_VIEWED, this::provideSetting)
        .when(ToasterTriggers.LEVER_JAMMED, this::toggledJammed)
        .configure(this::defineToastingStates)
        .and().ready();
  }
  
  private StateFactory<ToasterTriggers, ToasterStates> defineToastingStates(StateFactory<ToasterTriggers, ToasterStates> state) {
    return state.nest().startingWith(ToasterStates.OFF)
        .whenEntering(this::powerOff)
        .and().define(ToasterStates.TOASTING)
        .whenEntering(this::startTimers)
        .whenExiting(this::popupAlert)
        .when(ToasterTriggers.DIAL_CHANGED, this::restartPopupTimer)
        .configure(this::defineHeatingStates)
        .and().define(ToasterStates.ON_FIRE)
        .whenEntering(this::triggerSmokeAlarm)
        .and().transition(ToasterTriggers.LEVER_DEPRESSED).from(ToasterStates.OFF).to(ToasterStates.TOASTING)
        .invoke(this::maybeJam)
        .and().transition(ToasterTriggers.CANCEL_BUTTON_PRESSED).from(ToasterStates.TOASTING).to(ToasterStates.OFF)
        .guardedBy((transition) -> !jammed)
        .and().transition(ToasterTriggers.TIMER_EXPIRED).from(ToasterStates.TOASTING).to(ToasterStates.OFF)
        .guardedBy((transition) -> !jammed)
        .and().transition(ToasterTriggers.CATCH_FIRE).from(ToasterStates.TOASTING).to(ToasterStates.ON_FIRE)
        .and().parent();
  }

  private StateFactory<ToasterTriggers, ToasterStates> defineHeatingStates(StateFactory<ToasterTriggers, ToasterStates> state) {
    return state.nest().startingWith(ToasterStates.NORMAL_TOASTING)
    .whenEntering(this::turnOnBothHeatingElements)
    .and().define(ToasterStates.HALF_TOASTING)
    .whenEntering(this::turnOnOneHeatingElement)
    .and().transition(ToasterTriggers.BAGEL_BUTTON_PRESSED).from(ToasterStates.NORMAL_TOASTING).to(ToasterStates.HALF_TOASTING)
    .and().transition(ToasterTriggers.BAGEL_BUTTON_PRESSED).from(ToasterStates.HALF_TOASTING).to(ToasterStates.NORMAL_TOASTING)
    .and().parent();
  }

  /**
   * Returns true when the toast is popped up .
   * @return true if done
   */
  public boolean isToasting() {
    return toaster.getCurrentStates().stream()
        .anyMatch((state) -> state.getName().equals(ToasterStates.TOASTING));
  }
  
  /**
   * Returns true when the toast is popped up .
   * @return true if done
   */
  public boolean isBurning() {
    return toaster.getCurrentStates().getLast().getName().equals(ToasterStates.ON_FIRE);
  }

  /**
   * Starts toasting any items in the toaster.
   */
  public void makeToast() {
    toaster.accept(new ToasterEvent(ToasterTriggers.LEVER_DEPRESSED));
  }
  
  /**
   * Press the bagel button to let the toaster know there's a bagel being toasted.
   */
  public void pressBagelButton() {
    toaster.accept(new ToasterEvent(ToasterTriggers.BAGEL_BUTTON_PRESSED));
  }

  /**
   * Press the cancel button to stop toasting.
   */
  public void pressCancel() {
    toaster.accept(new ToasterEvent(ToasterTriggers.CANCEL_BUTTON_PRESSED));
  }

  /**
   * Indicate that the item is jammed in the toaster.
   */
  public void jamLever() {
    toaster.accept(new ToasterEvent(ToasterTriggers.LEVER_JAMMED));
  }
  
  /**
   * Change the dial on the toaster to control the degree of doneness.
   * @param setting the setting
   */
  public void changeToasterSetting(ToasterDial setting) {
    toaster.accept(new SetDialEvent(ToasterTriggers.DIAL_CHANGED, setting));
  }
  
  /**
   * Request the current setting of the toaster dial.
   * @param settingConsumer the consumer to tell about the setting
   */
  public void getToasterSetting(Consumer<ToasterDial> settingConsumer) {
    toaster.accept(new GetDialEvent(ToasterTriggers.DIAL_VIEWED, settingConsumer));
  }

  private void provideSetting(State<ToasterStates> current, Event<ToasterTriggers> event) {
     ((GetDialEvent) event).get().accept(setting);
  }

  private void changeSetting(State<ToasterStates> current, Event<ToasterTriggers> event) {
    setting = ((SetDialEvent) event).get();
  }
  
  private void toggledJammed(State<ToasterStates> current, Event<ToasterTriggers> event) {
    jammed = !jammed;
  }
  
  private void startTimers(State<ToasterStates> current) {
    dutyCycle++;
    scheduleIgnitionTimer();
    schedulePopupTimer();
  }

  private void scheduleIgnitionTimer() {
    ignite = new TimerTask() {
      public void run() {
        toaster.accept(new ToasterEvent(ToasterTriggers.CATCH_FIRE));
      }
    };
    timer.schedule(ignite, autoIgnitionDuration.toMillis());
  }

  private void schedulePopupTimer() {
    popup = new TimerTask() {
      public void run() {
        toaster.accept(new ToasterEvent(ToasterTriggers.TIMER_EXPIRED));
      }
    };
    timer.schedule(popup, setting.getToastingTime().toMillis());
  }

  private void restartPopupTimer(State<ToasterStates> current, Event<ToasterTriggers> event) {
    popup.cancel();
    startTimers(current);
  }

  private void powerOff(State<ToasterStates> current) {
    if (ignite != null) {
      ignite.cancel();
      ignite = null;
    }
    
    if (popup != null) {
      popup.cancel();
      popup = null;
    }
  }

  private void turnOnBothHeatingElements(State<ToasterStates> current) {
    System.out.println("Toasting both sides");
  }

  private void turnOnOneHeatingElement(State<ToasterStates> current) {
    System.out.println("Toasting one side");
  }

  private void popupAlert(State<ToasterStates> current) {
    System.out.println("Toast is done!");
  }

  private void maybeJam(Transition<ToasterStates> transition, Event<ToasterTriggers> event) {
    if (dutyCycle > 1000 && new Random().nextBoolean()) {
      jammed = true;
    }
  }

  private void triggerSmokeAlarm(State<ToasterStates> current) {
    System.out.println("Your toaster is on fire and the smoke alarm is going off!");
  }
}
