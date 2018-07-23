# RxRedux
A Redux store implemtation entirely base on RxJava (inspired by [redux-observable](https://redux-observable.js.org)) 
that helps to isolate side effects.

![RxRedux In a Nutshell](https://raw.githubusercontent.com/freeletics/RxRedux/master/docs/RxRedux-in-a-nutshell.png)

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
An Action is a command to "do something" in the store. 
An `Action` can be triggered by the user of your app (i.e. UI interaction like clicking a button) but also a `SideEffect` can trigger actions.
Every Action goes through the reducer. 
If an `Action` is not changing the state at all by the `Reducer` (because it's handled as a side effect), just return the previous state.
Furthermore, `SideEffects` can be registered for a certain type of `Action`.

# Reducer
A `Reducer` is basically a function `(State, Action) -> State` that takes the current State and an Action to compute a new State.
Every `Action` goes through the state reducer.
If an `Action` is not changing the state at all by the `Reducer` (because it's handled as a side effect), just return the previous state.

# Side Effect
A Side Effect is a function of type `(Observable<Action>, StateAccessor<State>) -> Observable<Action>`.
**So basically it's Actions in and Actions out.** 
You can think of a `SideEffect` as a use case in clean architecture: It should do just one job.
Every SideEffect can trigger multiple `Actions` (remember it returns `Observable<Action>`) which go through the `Reducer` but can also trigger other `SideEffects` registered for the corresponding `Action`.
Also a `Action` can have a `payload`. For example if you load some data from backend you emit the loaded data as a `Action` like `data class DataLoadedAction (val data : FooData)`. 
The mantra an Action is a command to do something is still true: in that case it means data is loaded, do with it "something".

# StateAccessor
Whenever a `SideEffect` needs to know the current State it can use `StateAccessor` to grab the latest state from Redux Store. StateAccessor is basically just a function `() -> State` to grab the latest State anytime you need it.

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
    object LoadPageAction : SideEffectAction() // Started loading the list of persons
    data class ErrorLoadingNextPageAction(val error : Throwable) : SideEffectAction() // An error occurred while loading
  }
}
```

```kotlin
val loadNextPageSideEffect : SideEffect<State, Action> = {  // Side effect is just a type alias for a function
  actions : Observable<Action>,  state : StateAccessor<State> -> 
  actions
    .ofType(LoadNextPageAction::class.java) // This side effect only runs for actions of type LoadNextPageAction
    .switchMap {
        // do network request
        val currentState : State  = state()
        val nextPage = state.currentPage + 1
        backend.getPersons(nextPage)
          .map { persons : List<Person> -> 
                  PageLoadedAction(
                      personsLoaded = persons, 
                      page = nextPage
                  ) 
           }
          .onErrorReturn { error -> ErrorLoadingNextPageAction(error) }
          .startWith(LoadPageAction)
     }
  }
}
```

```kotlin
val reducer : Reducer<State, Action> = { // Reducer is just a typealias for a function (State, Action) -> State
  state : State, action: Action ->
  when(action) {
    is LoadPageAction -> state.copy (loadingNextPage = true)
    is ErrorLoadingNextPageAction -> state.copy( loadingNextPage = false, errorLoadingNextPage = action.error)
    is PageLoadedAction -> state.copy(
      loadingNextPage = false, 
      errorLoadingNextPage = null
      persons = state.persons + action.persons,
      page = action.page
    )
    else -> state // Reducer is actually not handling this action (a SideEffect does it)
  }
}
```


```kotlin
val actions : Observable<Action> = ...
val sideEffects : List<SideEffect<State, Action> = listof(loadNextPageSideEffect, ... )

actions
  .reduxStore( initialState, sideEffects, reducer )
  .subscribe( state -> view.render(state) )
```

The following images illustrate the workflow:

0. Let's take a look at the following illustration:
![RxRedux In a Nutshell](https://raw.githubusercontent.com/freeletics/RxRedux/master/docs/step0
.png)
The blue box is the `View` (think UI). 
`Presenter` or `ViewModel` has not been drawn for the sake of readability but you can think of having such additional layers between View and Redux State Machine.
The yellow box represents a `Store`. 
The grey box is the `reducer`. 
The pink box is a `SideEffect`
Additionally, a green circle represent `State` and a red circle represent an `Action` (see next step).
On the right you see a UI mock of a mobile app to illustrate UI changes.

1. `NextPageAction` get triggered from the UI (by scrolling at the end of the list). Every `Action` goes through the `reducer` and all `SideEffects` registered for this type of Action.
![RxRedux In a Nutshell](https://raw.githubusercontent.com/freeletics/RxRedux/master/docs/step1.png)

2. `Reducer` is not interresting on `NextPageAction`. So while `NextPageAction` goes through the reducer, it doesn't change the state. 
![Step2](https://raw.githubusercontent.com/freeletics/RxRedux/master/docs/step2.png)

3. `loadNextPageSideEffect` (pink box), however, cares about `NextPageAction`. This is the trigger to run the side-effect

![Stpe3](https://raw.githubusercontent.com/freeletics/RxRedux/master/docs/step2.png)

4. So `loadNextPageSideEffect` takes `NextPageAction` and starts doing the job and makes the http request to load the next page from backend. Before doing that, this side effect starts with emitting `LoadPageAction`.

![Step4](https://raw.githubusercontent.com/freeletics/RxRedux/master/docs/step4.png)

5. `Reducer` takes `LoadPageAction` emitted from the side effect and reacts on it by "reducing the state". 
This means `Reducer` knows how to react on `LoadPageAction` to compute the new state (showing progress indicator at the bottom of the list). 

![Step5](https://raw.githubusercontent.com/freeletics/RxRedux/master/docs/step5.png)
![Step6](https://raw.githubusercontent.com/freeletics/RxRedux/master/docs/step6.png)
![Step7](https://raw.githubusercontent.com/freeletics/RxRedux/master/docs/step7.png)

Please note that the state has changed (highlighted in green) which results in also changing the UI (progress indicator at the end of the list).

6. Once `loadNextPageSideEffect` got the result back from backend the side effect emits a new `PageLoadedAction`. 
This Action contains a "payload" - the loaded data.

```kotlin
data class PageLoadedAction(val personsLoaded : List<Person>, val page : Int)
```
![Step8](https://raw.githubusercontent.com/freeletics/RxRedux/master/docs/step8.png)
![Step9](https://raw.githubusercontent.com/freeletics/RxRedux/master/docs/step8.png)

7. As any other Action `PageLoadedAction` goes through the `Reducer`. The Reducer processes this Action and computes a new state out of it by appending the loaded data to the already existing data.

![Step10](https://raw.githubusercontent.com/freeletics/RxRedux/master/docs/step9.png)
![Step11](https://raw.githubusercontent.com/freeletics/RxRedux/master/docs/step10.png)
![Step12](https://raw.githubusercontent.com/freeletics/RxRedux/master/docs/step11.png)


Final remark:
This system allows you to create a plugin in system of `SideEffects` that are highly reusable and specific to do a single use case.

![Step12](https://raw.githubusercontent.com/freeletics/RxRedux/master/docs/step13.png)

Also `SideEffects` can be invoked by `Actions` from other `SideEffects`. 