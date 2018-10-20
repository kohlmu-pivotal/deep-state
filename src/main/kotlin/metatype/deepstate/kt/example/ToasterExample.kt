package metatype.deepstate.kt.example

import metatype.deepstate.kt.core.State
import metatype.deepstate.kt.core.StateMachine
import metatype.deepstate.kt.core.builder.StateMachineBuilder
import metatype.deepstate.kt.dsl.*

class ToasterExample {
    fun initToaster() {
        val toaster = Toaster()
        println("safsdf")
    }

    private class Toaster {
        private val stateMachine: StateMachine = initializeStateMachine().build()
        private val toasterComponents = mutableSetOf<ToasterComponent>()

        private fun initializeStateMachine(): StateMachineBuilder {
            return stateMachine {
                name = "Toaster"
                states = setOf(
                        simpleState {
                            name = "On"
                            onEnter = { println("Warming up Toaster") }
                        },
                        compoundState {
                            stateMachineBuilder = initializeToastingStateMachine()
                            name = "Toasting"
                            onEnter = startToasting()
                            onExit = { println("Stopped toasting") }
                        },
                        simpleState {
                            name = "Off"
                            onEnter = { println("Shutting down") }
                            onExit = { println("Toaster Off") }
                        })
                initialState = initialState {
                    name = "On"
                }
                transitions = setOf(
                        transition {
                            fromState = "On"
                            toState = "Toasting"
                            event = "LeverDown"
                        }
                )
            }
        }

        private fun startToasting() = { println("Started toasting") }

        private fun initializeToastingStateMachine(): StateMachineBuilder =
                stateMachine {
                    name = "ToastingSM"
                    states = setOf(
                            simpleState {
                                name = "ToastingStart"
                                onEnter = {}
                                onExit = {}
                            }
                    )
                    initialState = initialState {
                        name = "ToastingStart"
                    }
                    transitions = setOf()
                }
    }

    private interface ToasterComponent {
        fun getStates(): Set<State>
    }

}

fun main(args: Array<String>) {
    ToasterExample().initToaster()
}

