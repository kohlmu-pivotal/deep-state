deep-state
===
A finite state machine library for Java

#Why

A state machine provides allows a clean separation of internal state from external
events.  As a component grows in features and complexity, typically internal flags
will be added to control state.  However, these flags can be cumbersome to maintain and
add complexity that is not intrinsic to the core capability.  When dealing with
mutable state and concurrency, many times a developer will implicitly create a state
machine without realizing it.  By explicitly encoding the events, states, and 
transitions within a state machine the code becomes much simpler, more obvious, and
easier to test. 

#What

Deep State provides a state machine definition and runtime engine following the standard
definition of a state machine from the Unified Modeling Language (UML).  The
following elements are exposed in the API:

**Events** are external stimuli delivered to the state machine.  The event consists
of a type and an optional user-defined payload that can be acted upon.  Each event
delivered to the state machine is processed fully (run-to-completion) before the
next event is evaluated.  If an event arrives while the state machine is busy, the
event is queued for later delivery.  The currently processing thread will continue
consuming events until the queue is empty.

**States** define a logical representation of the allowed conditions within a
system or component.  States may define entry and exit actions, a set of
internal event actions, and a default action.  The state machine
always has a single active state at any point in time.

**Transitions** link states together.  Transitions are triggered by the arrival
of an event.  A single event may cause a cascade of state transitions.  A
transition is selected by matching the event trigger to the transition trigger.  If
the trigger matches (or is unspecified) the guard condition is tested.  If the
guard condition passes the transition is performed.  During a transition,
the following invocation sequence happens, depending on which actions are defined:

1. Source state exit action
1. Transition action
1. Destination state entry action

Transitions to self should be guarded to avoid infinite recursion loops.

In addition, a state machine may be nested within a composite parent state.  This
allows common behaviors in the substate machine to be factored out into the parent
state.

For more information on UML state machines, see http://en.wikipedia.org/wiki/UML_state_machine.

#How

Deep State provides a fluent API for defining the state machine.

    // define the state machine
    FiniteStateMachine<String, String> hello = DeepState.<String, String>model("HelloWorld")
        .startingWith("Initial")
        .whenEntering((state) -> System.out.println("Hello World!"))
        .when("hi mom", (state, event) -> System.out.println("Look ma, no hands!"))
        .and().ready();
    
    // send an event
    hello.accept(new DeepStateEvent<>("hi mom"));
    
This would print the following:

    Hello World!
    Look ma, no hands!
