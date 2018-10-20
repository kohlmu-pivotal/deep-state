package metatype.deepstate.kt.core

class Transition(val fromState: State, val toState: State, val trigger: Event, val handler: () -> Unit)
