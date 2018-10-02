package metatype.deepstate.core;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import metatype.deepstate.FiniteStateMachine;

public class DeepStateFsm<T, U> implements FiniteStateMachine<T, U> {
  private static final Logger LOG = LogManager.getLogger();
  
  private static final <T> Guard<T> isTrue() {
    return trigger -> true;
  }
  
  private final SimpleState<T, U> initialState;
  private final Set<TriggeredTransition<T, U>> transitions;
  private final Consumer<Exception> uncaughtExceptionHandler;
  private final Optional<Consumer<Event<T>>> auditor;
  
  private final Object lock = new Object();
  private boolean active;
  private SimpleState<T, U> current;

  private ConcurrentLinkedQueue<Event<T>> events;
  
  public DeepStateFsm(SimpleState<T, U> initial, Set<TriggeredTransition<T, U>> transitions, Optional<Consumer<Exception>> uncaughtExceptionHandler, Optional<Consumer<Event<T>>> auditor) {
    this.initialState = initial;
    this.transitions = Collections.unmodifiableSet(transitions);
    this.events = new ConcurrentLinkedQueue<>();
    this.uncaughtExceptionHandler = uncaughtExceptionHandler.orElse(ex -> { 
      LOG.warn("Unexpected error", ex);
    });
    this.auditor = auditor;
  }

  @Override
  public State<U> getCurrentState() {
    synchronized (lock) {
      return current;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public Deque<State<U>> getCurrentStates() {
    synchronized (lock) {
      Deque<State<U>> states = new ArrayDeque<>();
      states.add(current);
      
      if (states.getLast() instanceof FiniteStateMachine<?, ?>) {
        states.addAll(((FiniteStateMachine<?, U>) states.getLast()).getCurrentStates());
      }
      return states;
    }
  }
  
  public State<U> getInitialState() {
    return initialState;
  }

  public <R> R read(Supplier<R> value) {
    synchronized (lock) {
      return value.get();
    }
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

  public DeepStateFsm<T, U> begin() {
    LOG.debug("Setting initial state {}", initialState.getName());
    synchronized (lock) {
      current = initialState;
      current.enter();
    }
    return this;
  }

  public DeepStateFsm<T, U> end() {
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
      Event<T> capturedEvent = event;
      auditor.ifPresent((consumer) -> consumer.accept(capturedEvent));
      processEvent(capturedEvent);
    }
  }

  private void processEvent(Event<T> event) {
    LOG.debug("Sending event {} to state {}", event, current.getName());
    current.accept(event);

    findMatchingTransition(event).ifPresent(transition -> performTransition(transition, event));
  }

  private Optional<TriggeredTransition<T, U>> findMatchingTransition(Event<T> event) {
    return transitions.stream()
      .filter(t -> t.getSource().equals(current))
      .filter(t -> t.getTrigger().test(event.getTrigger()))
      .filter(t -> t.getGuard().orElse(isTrue()).test(event))
      .findFirst();
  }

  @SuppressWarnings("unchecked")
  private void performTransition(TriggeredTransition<T, U> transition, Event<T> event) {
    LOG.debug("Transitioning from state {} to state {}", transition.getSource(), transition.getDestination());
    current.exit();
    fireTransitionAction(transition, event);
    
    current = (SimpleState<T, U>) transition.getDestination();
    current.enter();
    
    processEvent(event);
  }

  private void fireTransitionAction(TriggeredTransition<T, U> transition, Event<T> event) {
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
