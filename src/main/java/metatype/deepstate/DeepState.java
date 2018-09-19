package metatype.deepstate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import metatype.deepstate.FiniteStateMachine.Action;
import metatype.deepstate.FiniteStateMachine.Guard;
import metatype.deepstate.FiniteStateMachine.StateAction;
import metatype.deepstate.FiniteStateMachine.TransitionAction;
import metatype.deepstate.core.CompositeState;
import metatype.deepstate.core.DeepStateFsm;
import metatype.deepstate.core.SimpleState;
import metatype.deepstate.core.TriggeredTransition;

public class DeepState {
  private DeepState() { }
  
  public static <T> FsmFactory<T> model(String name) {
    return new FsmFactory<>(name);
  }
  
  public static class FsmFactory<T> {
    private final String name;
    
    private String initialState;
    private Map<String, StateFactory<T>> states;
    private Set<TransitionFactory<T>> transitions;
    
    private Optional<Consumer<Exception>> uncaughtExceptionHandler;

    private FsmFactory(String name) {
      this.name = name;
      states = new HashMap<>();
      transitions = new HashSet<>();
      uncaughtExceptionHandler = Optional.empty();
    }
        
    public StateFactory<T> startingWith(String name) {
      initialState = name;
      return define(name);
    }

    public StateFactory<T> define(String name) {
      if (states.containsKey(name)) {
        throw new IllegalStateException("Unable to redefine existing state " + name);
      }
      
      StateFactory<T> factory = new StateFactory<>(this);
      states.put(name, factory);
      return factory;
    }
    
    public FsmFactory<T> catchExceptionsUsing(Consumer<Exception> handler) {
      uncaughtExceptionHandler = Optional.of(handler);
      return this;
    }
    
    public TransitionFactory<T> transition(T trigger) {
      TransitionFactory<T> factory = new TransitionFactory<>(this, trigger);
      transitions.add(factory);
      return factory;
    }
    
    public DeepStateFsm<T> ready() {
      return create().begin();
    }
    
    private DeepStateFsm<T> create() {
      if (initialState == null) {
        throw new IllegalStateException("Initial state is not defined");
      }
      
      Map<String, SimpleState<T>> realStates = new HashMap<>();
      states.forEach((name, factory) -> {
        realStates.put(name, factory.create(name, uncaughtExceptionHandler));
      });
      
      Set<TriggeredTransition<T>> realTransitions = new HashSet<>();
      transitions.forEach((factory) -> {
        SimpleState<T> from = realStates.get(factory.from);
        if (from == null) {
          throw new IllegalStateException("Undefined from state " + factory.from + " for transition " + factory.trigger);
        }
        
        SimpleState<T> to = realStates.get(factory.to);
        if (to == null) {
          throw new IllegalStateException("Undefined to state " + factory.to + " for transition " + factory.trigger);
        }
        
        realTransitions.add(new TriggeredTransition<>(factory.trigger, from, to, factory.guard, factory.action));
      });
      return new DeepStateFsm<>(name, realStates.get(initialState), realTransitions, uncaughtExceptionHandler);
    }
  }
  
  public static class StateFactory<T> {
    private final FsmFactory<T> fsm;
    private Optional<Action> entryAction;
    private Optional<Action> exitAction;
    private Map<T, StateAction<T>> actions;
    private Optional<StateAction<T>> defaultAction;
    private Optional<FsmFactory<T>> nestedStateMachine;
    
    public StateFactory(FsmFactory<T> fsm) {
      this.fsm = fsm;
      this.entryAction = Optional.empty();
      this.exitAction = Optional.empty();
      this.defaultAction = Optional.empty();
      actions = new HashMap<>();
      nestedStateMachine = Optional.empty();
    }
    
    public StateFactory<T> whenEntering(Action entry) {
      this.entryAction = Optional.of(entry);
      return this;
    }
    
    public StateFactory<T> whenExiting(Action exit) {
      this.exitAction = Optional.of(exit);
      return this;
    }
    
    public StateFactory<T> when(T trigger, StateAction<T> action) {
      actions.put(trigger, action);
      return this;
    }
    
    public StateFactory<T> whenNothingElseMatches(StateAction<T> defaultAction) {
      this.defaultAction = Optional.of(defaultAction);
      return this;
    }
    
    public FsmFactory<T> nest(String name) {
      nestedStateMachine = Optional.of(new FsmFactory<>(name));
      return nestedStateMachine.get();
    }

    public FsmFactory<T> and() {
      return fsm;
    }
    
    private SimpleState<T> create(String name, Optional<Consumer<Exception>> uncaughtExceptionHandler) {
      if (nestedStateMachine.isPresent()) {
        return new CompositeState<>(name, entryAction, exitAction, actions, defaultAction, uncaughtExceptionHandler, nestedStateMachine.get().create());
      }
      return new SimpleState<>(name, entryAction, exitAction, actions, defaultAction, uncaughtExceptionHandler);
    }
  }
  
  public static class TransitionFactory<T> {
    private final FsmFactory<T> fsm;
    private final T trigger;
    
    private String from;
    private String to;
    private Optional<Guard<T>> guard;
    private Optional<TransitionAction<T>> action;

    private TransitionFactory(FsmFactory<T> fsm, T trigger) {
      this.fsm = fsm;
      this.trigger = trigger;
      guard = Optional.empty();
      action = Optional.empty();
    }
    
    public TransitionFactory<T> from(String name) {
      this.from = name;
      return this;
    }
    
    public TransitionFactory<T> to(String name) {
      this.to = name;
      return this;
    }

    public TransitionFactory<T> guard(Guard<T> guard) {
      this.guard = Optional.of(guard);
      return this;
    }
    
    public TransitionFactory<T> invoke(TransitionAction<T> action) {
      this.action = Optional.of(action);
      return this;
    }
    
    public FsmFactory<T> and() {
      if (from == null || to == null) {
        throw new IllegalStateException("Transition endpoints must be set");
      }
      return fsm;
    }
  }
}
