package metatype.deepstate.core;

import java.util.Optional;
import java.util.function.Predicate;

import metatype.deepstate.FiniteStateMachine.Guard;
import metatype.deepstate.FiniteStateMachine.State;
import metatype.deepstate.FiniteStateMachine.Transition;
import metatype.deepstate.FiniteStateMachine.TransitionAction;

public class TriggeredTransition<T> implements Transition {
  private static <T> Predicate<T> isTrue() {
    return eventTrigger -> true;
  }
  
  private static <T> Predicate<T> isEqualTo(T trigger) {
    return eventTrigger -> trigger.equals(eventTrigger);
  }
  
  private final Predicate<T> trigger;
  private final State source;
  private final State destination;
  private final Optional<Guard<T>> guard;
  private final Optional<TransitionAction<T>> action;
  
  public TriggeredTransition(Predicate<T> trigger, State source, State destination, Optional<Guard<T>> guard, Optional<TransitionAction<T>> action) {
    this.trigger = trigger;
    this.source = source;
    this.destination = destination;
    this.guard = guard;
    this.action = action;
  }

  public TriggeredTransition(T trigger, State source, State destination, Optional<Guard<T>> guard, Optional<TransitionAction<T>> action) {
    this(isEqualTo(trigger), source, destination, guard, action);
  }
  
  public TriggeredTransition(State source, State destination, Optional<Guard<T>> guard, Optional<TransitionAction<T>> action) {
    this(isTrue(), source, destination, guard, action);
  }
  
  @Override
  public State getSource() {
    return source;
  }

  @Override
  public State getDestination() {
    return destination;
  }

  public Optional<Guard<T>> getGuard() {
    return guard;
  }
  
  public Optional<TransitionAction<T>> getAction() {
    return action;
  }
  
  public Predicate<T> getTrigger() {
    return trigger;
  }

  @Override
  public String toString() {
    return String.format("%1 -> %2", source, destination);
  }
}
