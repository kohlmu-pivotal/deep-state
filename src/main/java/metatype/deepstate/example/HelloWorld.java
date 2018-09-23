package metatype.deepstate.example;

import metatype.deepstate.DeepState;
import metatype.deepstate.DeepStateEvent;
import metatype.deepstate.FiniteStateMachine;

public class HelloWorld {
  public static void main(String[] args) {
    FiniteStateMachine<String, String> hello = DeepState.<String, String>model()
        .startingWith("HelloWorld")
        .whenEntering((state) -> System.out.println("Hello World!"))
        .when("hi mom", (state, event) -> System.out.println("Look ma, no hands!"))
        .and().ready();
    
    hello.accept(new DeepStateEvent<>("hi mom"));
  }
}
