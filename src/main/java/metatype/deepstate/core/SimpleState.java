package metatype.deepstate.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import metatype.deepstate.FiniteStateMachine.Action;
import metatype.deepstate.FiniteStateMachine.Event;
import metatype.deepstate.FiniteStateMachine.State;
import metatype.deepstate.FiniteStateMachine.StateAction;

public class SimpleState<T, U> implements State<U>, Consumer<Event<T>> {
  private static final Logger LOG = LoggerFactory.getLogger(SimpleState.class);

  /** the name of the state */
  private final U name;
  
  /** the action to execute before entering the state */
  private final Action<U> entryAction;
  
  /** the action to execute after entering the state */
  private final Action<U> exitAction;
  
  /** the set of internal actions to invoke when an event trigger matches */
  private final Map<T, StateAction<T, U>> actions;
  
  /** the action that is invoked if no other actions match */
  private final StateAction<T, U> defaultAction;
  
  private final Consumer<Exception> uncaughtExceptionHandler;
  
  public SimpleState(U name, Action<U> entry, Action<U> exit, Map<T, StateAction<T, U>> actions, StateAction<T, U> defaultAction, Consumer<Exception> uncaughtExceptionHandler) {
    this.name = name;
    this.entryAction = entry;
    this.exitAction = exit;
    this.actions = new HashMap<>(actions);
    this.defaultAction = defaultAction;
    this.uncaughtExceptionHandler = defaultExceptionHandler(uncaughtExceptionHandler);
  }

  @Override
  public U getIdentity() {
    return name;
  }
  
  @Override
  public String toString() {
    return getIdentity().toString();
  }

  @Override
  public void accept(Event<T> event) {
    findActionForTrigger(event.getTrigger()).ifPresent((action) -> invokeAction(event, action));
  }

  public void enter() {
    try {
      LOG.debug("Entering state {}", this);
      getEntryAction().ifPresent((action) -> action.accept(this));
    } catch (Exception e) {
      uncaughtExceptionHandler.accept(e);
    }
  }

  public void exit() {
    try {
      LOG.debug("Exiting state {}", this);
      getExitAction().ifPresent((action) -> action.accept(this));
    } catch (Exception e) {
      uncaughtExceptionHandler.accept(e);
    }
  }

  protected Optional<Action<U>> getEntryAction() {
    return Optional.ofNullable(entryAction);
  }

  protected Optional<Action<U>> getExitAction() {
    return Optional.ofNullable(exitAction);
  }

  private Consumer<Exception> defaultExceptionHandler(Consumer<Exception> uncaughtExceptionHandler) {
    if (uncaughtExceptionHandler == null) {
      uncaughtExceptionHandler = (e) -> { 
        LOG.warn("Unexpected error", e);
      };
    }
    return uncaughtExceptionHandler;
  }

  private void invokeAction(Event<T> event, StateAction<T, U> action) {
    try {
      LOG.debug("Invoking action for event {} on state {}", event, this);
      action.accept(this, event);
    } catch (Exception e) {
       uncaughtExceptionHandler.accept(e);
    }
  }
  
  private Optional<StateAction<T, U>> findActionForTrigger(T trigger) {
    StateAction<T, U> action = actions.get(trigger);
    if (action == null) {
      action = defaultAction;
    }
    return Optional.ofNullable(action);
  }
}
