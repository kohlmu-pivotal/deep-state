package metatype.deepstate.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import metatype.deepstate.FiniteStateMachine.Action;
import metatype.deepstate.FiniteStateMachine.Event;
import metatype.deepstate.FiniteStateMachine.State;
import metatype.deepstate.FiniteStateMachine.StateAction;

public class SimpleState<T, U> implements State<U>, Consumer<Event<T>> {
  private static final Logger LOG = LogManager.getLogger();

  /** the name of the state */
  private final U name;
  
  /** the action to execute before entering the state */
  private final Optional<Action<U>> entryAction;
  
  /** the action to execute after entering the state */
  private final Optional<Action<U>> exitAction;
  
  /** the set of internal actions to invoke when an event trigger matches */
  private final Map<T, StateAction<T, U>> actions;
  
  /** the action that is invoked if no other actions match */
  private final Optional<StateAction<T, U>> defaultAction;
  
  private final Consumer<Exception> uncaughtExceptionHandler;
  
  public SimpleState(U name, Optional<Action<U>> entry, Optional<Action<U>> exit, Map<T, StateAction<T, U>> actions, Optional<StateAction<T, U>> defaultAction, Optional<Consumer<Exception>> uncaughtExceptionHandler) {
    this.name = name;
    this.entryAction = entry;
    this.exitAction = exit;
    this.actions = new HashMap<>(actions);
    this.defaultAction = defaultAction;
    this.uncaughtExceptionHandler = uncaughtExceptionHandler.orElse((e) -> { 
      LOG.warn("Unexpected error", e);
    });

  }

  @Override
  public U getName() {
    return name;
  }
  
  @Override
  public String toString() {
    return getName().toString();
  }

  @Override
  public void accept(Event<T> event) {
    StateAction<T, U> action = findActionForTrigger(event.getTrigger());
    if (action == null) {
      return;
    }
      
    invokeAction(event, action);
  }

  public void enter() {
    try {
      LOG.debug("Entering state {}", this);
      entryAction.ifPresent((action) -> action.accept(this));
    } catch (Exception e) {
      uncaughtExceptionHandler.accept(e);
    }
  }

  public void exit() {
    try {
      LOG.debug("Exiting state {}", this);
      exitAction.ifPresent((action) -> action.accept(this));
    } catch (Exception e) {
      uncaughtExceptionHandler.accept(e);
    }
  }

  protected Optional<Action<U>> getEntryAction() {
    return entryAction;
  }

  protected Optional<Action<U>> getExitAction() {
    return exitAction;
  }

  private void invokeAction(Event<T> event, StateAction<T, U> action) {
    try {
      LOG.debug("Invoking action for event {} on state {}", event, this);
      action.accept(this, event);
    } catch (Exception e) {
       uncaughtExceptionHandler.accept(e);
    }
  }
  
  private StateAction<T, U> findActionForTrigger(T trigger) {
    StateAction<T, U> action = actions.get(trigger);
    if (action == null) {
      action = defaultAction.orElse(null);
    }
    return action;
  }
}
