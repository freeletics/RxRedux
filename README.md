# RxRedux
A Redux store implemtation entirely base on RxJava (inspired by [redux-observable](https://redux-observable.js.org)) 
that helps to isolate side effects.

## State of this repository
Why is there no code in this repository?
We haven't published the code yet. We wanted to share first how the API looks like to get Feedback early and not reinvent the wheel (and maybe combine forces if there are similar efforts going on).

## How is this different from other Redux implementations like [Mobius](https://github.com/spotify/mobius)
In contrast to any other Redux inspired library out there, this library is really backed on top of RxJava (Mobius just offers some extensions to use RxJava for async works). 
This library offers a custom RxJava operator `.reduxStore( initialState, sideEffects, reducer )` and treats upstream events as `Actions`. 

# Redux Store
A Store is basically an observable container for state. 
This library provides a kotlin extension function to create `.  .reduxStore<State, Action>(initialState, sideEffects, reducer)` to create such a state container.
It takes an `initialState` and a list of `SideEffect<State, Action>` and a `Reducer<State, Action>`

# Action
An Action is a command to "do something" in the store. Every Action goes through the reducer. If an `Action` is not changing the state at all by the `Reducer` (because it's handled as a side effect), just return the previous state.
Furthermore, `SideEffects` can be registered for a certain type of `Action`.

# Reducer
A `Reducer` is basically a function `(State, Action) -> State` that takes the current State and an Action to compute a new State.
Every `Action` goes through the state reducer.
If an `Action` is not changing the state at all by the `Reducer` (because it's handled as a side effect), just return the previous state.

# Side Effect
A Side Effect is a function of type `(Observable<Action>, StateAccessor<State>) -> Observable<Action>`.
So basically it's Actions in and Actions out. 
You can think of a `SideEffect` as a use case in clean architecture: It should do just one job.
Every SideEffect can trigger multiple `Actions` (remember it returns `Observable<Action>`) which go through the `Reducer` but can also trigger other `SideEffects` registered for the corresponding Action.

# StateAccessor
Whenever a `SideEffect` needs to know the current State in its workflow, it can use `StateAccessor` to grab the latest state from Redux Store.

# Usage
Let's create a simple Redux Store for Pagination: Goal is to display a list of `Persons` on screen. Plus one can 

``` kotlin
data class State {
  val currentPage : Int,
  val persons : List<Person>, // The list of persons 
  val loadingNextPage : Boolean,
  val errorLoadingNextPage : Throwable?
}

val initialState = State(
  currentPage = 0, 
  persons = emptyList(), 
  loadingNextPage = false, 
  errorLoadingNextPage = null
)
```

```kotlin
sealed class Action {
  sealed class UserAction : Action(){   // Actions triggered by the user
    object LoadNextPageAction : UserAction()    // Action to load the first page
  }
  
  sealed class SideEffectAction : Action() { // Actions triggered by a side effect
    data class PageLoadedAction(val personsLoaded : List<Person>, val page : Int) : SideEffectAction() // Persons has been loaded
    object StartedLoadingNextPageAction : SideEffectAction() // Started loading the list of persons
    data class ErrorLoadingNextPageAction(val error : Throwable) : SideEffectAction() // An error occurred while loading
  }
}
```

```kotlin
val loadListPersonSideEffect : SideEffect<State, Action> = {  // Side effect is just a type alias for a function
  actions : Observable<Action>,  state : StateAccessor<State> -> 
  actions
    .ofType(LoadPersonsAction::class.java) // This side effect only runs for actions of type LoadPersonsAction
    .switchMap {
        // do network request
        val currentState : State  = state()
        val nextPage = state.currentPage + 1
        backend.getPersons(nextPage)
          .map { persons : List<Person> -> PageLoadedAction(
                      personsLoaded = persons, 
                      page = nextPage
                   ) 
           }
          .onErrorReturn { ErrorLoadingNextPageAction(it) }
          .startWith(StartedLoadingNextPageAction)
     }
  }
}
```

```kotlin
val reducer : Reducer<State, Action> = { // Reducer is just a typealias for a function (State, Action) -> State
  state : State, action: Action ->
  when(action) {
    is StartedLoadingNextPageAction -> state.copy (loadingNextPage = true)
    is ErrorLoadingNextPageAction -> state.copy( loadingNextPage = false, errorLoadingNextPage = action.error)
    is PageLoadedAction -> state.copy(
      loadingNextPage = false, 
      errorLoadingNextPage = null
      persons = state.persons + action.persons,
      page = action.page
    )
    else -> state // Reducer is actually not handling this action (a sideeffect does most likely)
  }
}
```


```kotlin
val actions : Observable<Action> = ...
val sideEffects : List<SideEffect<State, Action> = listof(loadListPersonSideEffect, ...)

actions
  .reduxStore( initialState, sideEffects, reducer )
  .subscribe( state -> view.render(state) )
```
