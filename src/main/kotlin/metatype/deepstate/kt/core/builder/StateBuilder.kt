package metatype.deepstate.kt.core.builder

import metatype.deepstate.kt.core.State
import metatype.deepstate.kt.dsl.StateMachineDSL


sealed class StateBuilder<T> : Builder<T> {
    lateinit var name: String
    lateinit var onEnter: () -> Unit
    lateinit var onExit: () -> Unit

    protected open fun validateLateInitVars(builder: StateMachineBuilder) {
        if (!this::name.isInitialized) {
            throw IllegalArgumentException("State, within statemachine ${builder.name} does not contain valid name")
        }
        if (!this::onEnter.isInitialized) {
            onEnter = {}
        }
        if (!this::onExit.isInitialized) {
            onExit = {}
        }
    }

    @StateMachineDSL
    class SimpleStateBuilder : StateBuilder<State.SimpleState>() {
        override fun build(builder: Builder<*>): State.SimpleState {
            validateLateInitVars(builder as StateMachineBuilder)
            return State.SimpleState(name, onEnter, onExit)
        }
    }

    @StateMachineDSL
    class CompoundStateBuilder : StateBuilder<State.CompoundState>() {
        lateinit var stateMachineBuilder: StateMachineBuilder

        override fun build(builder: Builder<*>): State.CompoundState {
            validateLateInitVars(builder as StateMachineBuilder)
            return State.CompoundState(name, onEnter, onExit, stateMachineBuilder.build(builder))
        }

        override fun validateLateInitVars(builder: StateMachineBuilder) {
            super.validateLateInitVars(builder)
            if (!this::stateMachineBuilder.isInitialized) {
                throw IllegalArgumentException("Statemachine, within complex state $name is not initialized")
            }
        }
    }
}