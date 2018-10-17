package metatype.deepstate.example;

import java.time.Duration;
import java.util.Random;
import java.util.TimerTask;

import metatype.deepstate.DeepState;
import metatype.deepstate.DeepState.StateFactory;
import metatype.deepstate.DeepStateEvent;
import metatype.deepstate.FiniteStateMachine;
import metatype.deepstate.FiniteStateMachine.Event;
import metatype.deepstate.FiniteStateMachine.State;
import metatype.deepstate.FiniteStateMachine.Transition;

/** 
 * A toaster implemented using a finite state machine.
 */
public class FsmToaster extends AbstractToaster {
  /**
   * Defines the available toaster states.
   */
  private enum States {
    TOASTER, OFF, TOASTING, NORMAL_TOASTING, HALF_TOASTING, ON_FIRE
  }

  /**
   * Defines the available toaster events.
   */
  private enum Triggers {
    DIAL_CHANGED, BAGEL_BUTTON_PRESSED, CANCEL_BUTTON_PRESSED, LEVER_DEPRESSED, TIMER_EXPIRED, CATCH_FIRE, LEVER_JAMMED
  }
  
  /**
   * A generic toasting event.
   */
  private static class ToasterEvent extends DeepStateEvent<Triggers, Object> {
    public ToasterEvent(Triggers trigger) {
      super(trigger);
    }
  }
  
  /**
   * An event that changes the toaster dial setting.
   */
  private static class SetDialEvent extends DeepStateEvent<Triggers, Dial> {
    public SetDialEvent(Triggers trigger, Dial setting) {
      super(trigger, setting);
    }
  }
  
  /** finite state machine for the toaster */
  private final FiniteStateMachine<Triggers, States> toaster;

  public FsmToaster() {
    this(Duration.ofMinutes(5));
  }
  
  public FsmToaster(Duration autoIgnitionDuration) {
    super(autoIgnitionDuration);
    this.toaster = DeepState.<Triggers, States>model()
        .startingWith(States.TOASTER)
        .when(Triggers.DIAL_CHANGED, this::changeSetting)
        .when(Triggers.LEVER_JAMMED, this::toggledJammed)
        .configure(this::defineToastingStates)
        .and().ready();
  }
  
  private StateFactory<Triggers, States> defineToastingStates(StateFactory<Triggers, States> state) {
    return state.nest().startingWith(States.OFF)
        .and().define(States.TOASTING)
        .whenEntering(this::startTimers)
        .whenExiting(this::popupAlert)
        .when(Triggers.DIAL_CHANGED, this::restartPopupTimer)
        .configure(this::defineHeatingStates)
        .and().define(States.ON_FIRE)
        .whenEntering(this::triggerSmokeAlarm)
        .and().transition(Triggers.LEVER_DEPRESSED).from(States.OFF).to(States.TOASTING)
        .invoke(this::maybeJam)
        .and().transition(Triggers.CANCEL_BUTTON_PRESSED).from(States.TOASTING).to(States.OFF)
        .guardedBy((transition) -> !jammed)
        .and().transition(Triggers.TIMER_EXPIRED).from(States.TOASTING).to(States.OFF)
        .guardedBy((transition) -> !jammed)
        .and().transition(Triggers.CATCH_FIRE).from(States.TOASTING).to(States.ON_FIRE)
        .and().parent();
  }

  private StateFactory<Triggers, States> defineHeatingStates(StateFactory<Triggers, States> state) {
    return state.nest()
        .startingWith(States.NORMAL_TOASTING)
        .whenEntering(this::turnOnBothHeatingElements)
        .and().define(States.HALF_TOASTING).whenEntering(this::turnOnOneHeatingElement)
        .and().transition(Triggers.BAGEL_BUTTON_PRESSED).from(States.NORMAL_TOASTING).to(States.HALF_TOASTING)
        .and().transition(Triggers.BAGEL_BUTTON_PRESSED).from(States.HALF_TOASTING).to(States.NORMAL_TOASTING)
        .and().parent();
  }

  @Override
  public boolean isToasting() {
    return toaster.getCurrentStates().stream()
        .anyMatch((state) -> state.getIdentity().equals(States.TOASTING));
  }
  
  @Override
  public boolean isBurning() {
    return toaster.getCurrentStates().getLast().getIdentity().equals(States.ON_FIRE);
  }

  @Override
  public void depressLever() {
    toaster.accept(new ToasterEvent(Triggers.LEVER_DEPRESSED));
  }
  
  @Override
  public void pressBagelButton() {
    toaster.accept(new ToasterEvent(Triggers.BAGEL_BUTTON_PRESSED));
  }

  @Override
  public void pressCancel() {
    toaster.accept(new ToasterEvent(Triggers.CANCEL_BUTTON_PRESSED));
  }

  @Override
  public void jamLever() {
    toaster.accept(new ToasterEvent(Triggers.LEVER_JAMMED));
  }
  
  @Override
  public void changeToasterSetting(Dial setting) {
    toaster.accept(new SetDialEvent(Triggers.DIAL_CHANGED, setting));
  }
  
  @Override
  public Dial getToasterSetting() {
    return toaster.read(() -> setting);
  }

  private void changeSetting(State<States> current, Event<Triggers> event) {
    setting = ((SetDialEvent) event).getPayload().orElseThrow(() -> new IllegalStateException("Unspecified dial setting"));
  }
  
  private void toggledJammed(State<States> current, Event<Triggers> event) {
    jammed = !jammed;
  }
  
  private void startTimers(State<States> current) {
    dutyCycle++;
    scheduleIgnitionTimer();
    schedulePopupTimer();
  }

  private void scheduleIgnitionTimer() {
    ignite = new TimerTask() {
      public void run() {
        toaster.accept(new ToasterEvent(Triggers.CATCH_FIRE));
      }
    };
    timer.schedule(ignite, autoIgnitionDuration.toMillis());
  }

  private void schedulePopupTimer() {
    popup = new TimerTask() {
      public void run() {
        toaster.accept(new ToasterEvent(Triggers.TIMER_EXPIRED));
      }
    };
    timer.schedule(popup, setting.getToastingTime().toMillis());
  }

  private void restartPopupTimer(State<States> current, Event<Triggers> event) {
    popup.cancel();
    startTimers(current);
  }

  private void turnOnBothHeatingElements(State<States> current) {
    System.out.println("Toasting both sides");
  }

  private void turnOnOneHeatingElement(State<States> current) {
    System.out.println("Toasting one side");
  }

  private void popupAlert(State<States> current) {
    powerOff();
    System.out.println("Toast is done!");
  }

  private void powerOff() {
    ignite.cancel();
    popup.cancel();
  }

  private void maybeJam(Transition<States> transition, Event<Triggers> event) {
    if (dutyCycle > 1000 && new Random().nextBoolean()) {
      jammed = true;
    }
  }

  private void triggerSmokeAlarm(State<States> current) {
    System.out.println("Your toaster is on fire and the smoke alarm is going off!");
  }
}
