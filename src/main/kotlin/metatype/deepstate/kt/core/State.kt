package metatype.deepstate.kt.core

sealed class State(val name: String, val onEnter: () -> Unit, val onExit: () -> Unit) {
    override fun hashCode() = name.hashCode()

    override fun equals(other: Any?) = other is State && other.name == name

    class SimpleState(name: String, onEnter: () -> Unit = {}, onExit: () -> Unit = {})
        : State(name, onEnter, onExit)

    class CompoundState(name: String, onEnter: () -> Unit = {}, onExit: () -> Unit = {},
                        val stateMachine: StateMachine) : State(name, onEnter, onExit)
}

