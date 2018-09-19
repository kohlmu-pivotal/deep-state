package metatype.deepstate.core;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import metatype.deepstate.FiniteStateMachine;

public class DeepStateFsm<T> implements FiniteStateMachine<T> {
  private static final Logger LOG = LogManager.getLogger();
  
  private static final <T> Guard<T> isTrue() {
    return trigger -> true;
  }
  
  private final String name;
  private final SimpleState<T> initialState;
  private final Set<TriggeredTransition<T>> transitions;
  private final Consumer<Exception> uncaughtExceptionHandler;
  
  private final Object lock = new Object();
  private boolean active;
  private SimpleState<T> current;

  private ConcurrentLinkedQueue<Event<T>> events;
  
  public DeepStateFsm(String name, SimpleState<T> initial, Set<TriggeredTransition<T>> transitions, Optional<Consumer<Exception>> uncaughtExceptionHandler) {
    this.name = name;
    this.initialState = initial;
    this.transitions = Collections.unmodifiableSet(transitions);
    this.events = new ConcurrentLinkedQueue<>();
    this.uncaughtExceptionHandler = uncaughtExceptionHandler.orElse(ex -> { 
      LOG.warn("Unexpected error", ex);
    });
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public State getCurrentState() {
    synchronized (lock) {
      return current;
    }
  }

  @Override
  public Deque<State> getCurrentStates() {
    synchronized (lock) {
      Deque<State> states = new ArrayDeque<>();
      states.add(current);
      
      if (states.getLast() instanceof FiniteStateMachine<?>) {
        states.addAll(((FiniteStateMachine<?>) states.getLast()).getCurrentStates());
      }
      return states;
    }
  }
  
  @Override
  public String toString() {
    return name;
  }

  public State getInitialState() {
    return initialState;
  }

  @Override
  public void accept(Event<T> event) {
    events.add(event);
    
    synchronized (lock) {
      if (active) {
        return;
      }
      
      active = true;
      try {
        runToCompletion();
        
      } finally {
        active = false;
      }
    }
  }

  public DeepStateFsm<T> begin() {
    LOG.debug("Setting initial state {}", initialState.getName());
    synchronized (lock) {
      current = initialState;
      current.enter();
    }
    return this;
  }

  public DeepStateFsm<T> end() {
    synchronized (lock) {
      LOG.debug("Leaving final state {}", current.getName());
      current.exit();
      current = null;
    }
    return this;
  }

  private void runToCompletion() {
    Event<T> event;
    while ((event = events.poll()) != null) {
      LOG.debug("Sending event {} to state {}", event, current.getName());
      current.accept(event);

      Event<T> capturedEvent = event;
      findMatchingTransition(event).ifPresent(transition -> performTransition(transition, capturedEvent));
    }
  }

  private void performTransition(TriggeredTransition<T> transition, Event<T> event) {
    LOG.debug("Transitioning from state {} to state {}", transition.getSource(), transition.getDestination());
    current.exit();
    fireTransitionAction(transition, event);
    
    current = (SimpleState<T>) transition.getDestination();
    current.enter();
  }

  private Optional<TriggeredTransition<T>> findMatchingTransition(Event<T> event) {
    return transitions.stream()
      .filter(t -> t.getSource().equals(current))
      .filter(t -> t.getTrigger().test(event.getTrigger()))
      .filter(t -> t.getGuard().orElse(isTrue()).test(event))
      .findFirst();
  }

  private void fireTransitionAction(TriggeredTransition<T> transition, Event<T> event) {
    try {
      transition.getAction().ifPresent(action -> {
        LOG.debug("Invoking action for event {} during transtion {}", event, transition);
        action.accept(transition, event); 
      });
    } catch (Exception e) {
      uncaughtExceptionHandler.accept(e);
    }
  }
}
