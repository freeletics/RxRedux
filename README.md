# RxRedux
A Redux store implemtation entirely base on RxJava (inspired by (redux-observable)[https://redux-observable.js.org]) 
that helps to isolate side effects.


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
``` kotlin

val loadListPersonSideEffect : SideEffect<State, Action> = {  // Side effect is just a type alias for a function
  actions : Observable<Action>,  state : StateAccessor<State> -> 
  // return Observable<Action>
  actions
  .ofType(LoadPersonsAction::class.java)
  .switchMap {
     // do network request
  }
}

val actions : Observable<Action> = ...
val sideEffects : List<SideEffect<State, Action> = listof(loadListPersonSideEffect, ...)

actions
  .reduxStore( initialState, sideEffects, reducer )
  .subscribe( state -> view.render(state) )
```
