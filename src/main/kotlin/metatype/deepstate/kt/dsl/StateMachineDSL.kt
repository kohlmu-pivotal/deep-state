package metatype.deepstate.kt.dsl

import metatype.deepstate.kt.core.builder.StateBuilder
import metatype.deepstate.kt.core.builder.StateMachineBuilder
import metatype.deepstate.kt.core.builder.TransitionBuilder

@DslMarker
annotation class StateMachineDSL

fun stateMachine(init: StateMachineBuilder.() -> Unit): StateMachineBuilder {
    val stateMachineBuilder = StateMachineBuilder()
    stateMachineBuilder.init()
    return stateMachineBuilder
}

fun initialState(init: StateBuilder.SimpleStateBuilder.() -> Unit): StateBuilder.SimpleStateBuilder {
    val initialStateBuilder = StateBuilder.SimpleStateBuilder()
    initialStateBuilder.init()
    return initialStateBuilder

}

fun simpleState(init: StateBuilder.SimpleStateBuilder.() -> Unit): StateBuilder.SimpleStateBuilder {
    val simpleStateBuilder = StateBuilder.SimpleStateBuilder()
    simpleStateBuilder.init()
    return simpleStateBuilder
}

fun compoundState(init: StateBuilder.CompoundStateBuilder.() -> Unit): StateBuilder.CompoundStateBuilder {
    val complexStateBuilder = StateBuilder.CompoundStateBuilder()
    complexStateBuilder.init()
    return complexStateBuilder
}

fun transition(init: TransitionBuilder.() -> Unit): TransitionBuilder {
    val transitionBuilder = TransitionBuilder()
    transitionBuilder.init()
    return transitionBuilder
}