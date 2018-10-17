package metatype.deepstate.example;

import metatype.deepstate.DeepState;
import metatype.deepstate.DeepStateEvent;
import metatype.deepstate.FiniteStateMachine;

public class HelloWorld {
  private enum States { HELLO, WORLD };
  private enum Triggers { SAY_HELLO }
  
  public static void main(String[] args) {
    @SuppressWarnings("unchecked")
    // begin defining the state machine
    FiniteStateMachine<Triggers, States> hello = DeepState.<Triggers, States>model()
        // define the initial state
        .startingWith(States.HELLO)
        
        // invoke this action on exit
        .whenExiting(System.out::println)
        
        // define another state
        .and().define(States.WORLD)
        
        // invoke this action on entry
        .whenEntering(System.out::println)
        
        // define a transtion from HELLO -> WORLD triggered by SAY_HELLO
        .and().transition(Triggers.SAY_HELLO).from(States.HELLO).to(States.WORLD)
        
        // invoke this action during the transition
        .invoke((state, event) -> System.out.println(((DeepStateEvent<Triggers, String>) event).getPayload()))
        
        // let's go!
        .and().ready();
    
    // send an event that causes the transition
    hello.accept(new DeepStateEvent<>(Triggers.SAY_HELLO, "Look ma, no hands!"));
  }
}
