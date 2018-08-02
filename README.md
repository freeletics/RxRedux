# RxRedux
A Redux store implemtation entirely base on RxJava (inspired by [redux-observable](https://redux-observable.js.org)) 
that helps to isolate side effects.

![RxRedux In a Nutshell](https://raw.githubusercontent.com/freeletics/RxRedux/master/docs/rxredux.png)

[![Build Status](https://travis-ci.org/freeletics/RxRedux.svg?branch=master)](https://travis-ci.org/freeletics/RxRedux)

## Dependency
Dependencies are hosted on Maven Central:

```
implementation 'com.freeletics.rxredux:rxredux:1.0.0'
```
Keep in mind that this is library is written in kotlin which means you also add `kotlin-stdlib` to your project by using RxRedux.

#### Snapshot
Latest snapshot (directly published from master branch from Travis CI):

```
allprojects {
    repositories {
        ...
        // Add url to snapshot repository
        maven {
            url "https://oss.sonatype.org/content/repositories/snapshots/"
        }
    }
}

```

```
implementation 'com.freeletics.rxredux:rxredux:1.0.1-SNAPSHOT'
```

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
Also an `Action` can have a `payload`. For example if you load some data from backend you emit the loaded data as an `Action` like `data class DataLoadedAction (val data : FooData)`. 
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
    object LoadNextPageAction : Action() // Action to load the first page. Triggered by the user.

    data class PageLoadedAction(val personsLoaded : List<Person>, val page : Int) : Action() // Persons has been loaded
    object LoadPageAction : Action() // Started loading the list of persons
    data class ErrorLoadingNextPageAction(val error : Throwable) : Action() // An error occurred while loading
}
```

```kotlin
// SideEffect is just a type alias for such a function:
fun loadNextPageSideEffect (actions : Observable<Action>, state: StateAccessor<State>) : Observable<Action> =
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
```

```kotlin
// Reducer is just a typealias for a function
fun reducer(state : Statine, action : Action) : State =
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
```


```kotlin
val actions : Observable<Action> = ...
val sideEffects : List<SideEffect<State, Action> = listOf(::loadNextPageSideEffect, ... )

actions
  .reduxStore( initialState, sideEffects, ::reducer )
  .subscribe( state -> view.render(state) )
```

The [following video](https://youtu.be/M7lx9Y9ANYo) (click on it) illustrate the workflow:

[![RxRedux explanation](https://i.ytimg.com/vi/M7lx9Y9ANYo/hqdefault.jpg?sqp=-oaymwEXCNACELwBSFryq4qpAwkIARUAAIhCGAE=&rs=AOn4CLAqwunKP2_qGE0HYUlquWkFccM5MA)](https://youtu.be/M7lx9Y9ANYo)


0. Let's take a look at the following illustration:
The blue box is the `View` (think UI). 
`Presenter` or `ViewModel` has not been drawn for the sake of readability but you can think of having such additional layers between View and Redux State Machine.
The yellow box represents a `Store`. 
The grey box is the `reducer`. 
The pink box is a `SideEffect`
Additionally, a green circle represents `State` and a red circle represents an `Action` (see next step).
On the right you see a UI mock of a mobile app to illustrate UI changes.

1. `NextPageAction` get triggered from the UI (by scrolling at the end of the list). Every `Action` goes through the `reducer` and all `SideEffects` registered for this type of Action.

2. `Reducer` is not interesting on `NextPageAction`. So while `NextPageAction` goes through the reducer, it doesn't change the state.

3. `loadNextPageSideEffect` (pink box), however, cares about `NextPageAction`. This is the trigger to run the side-effect

4. So `loadNextPageSideEffect` takes `NextPageAction` and starts doing the job and makes the http request to load the next page from backend. Before doing that, this side effect starts with emitting `LoadPageAction`.

5. `Reducer` takes `LoadPageAction` emitted from the side effect and reacts on it by "reducing the state". 
This means `Reducer` knows how to react on `LoadPageAction` to compute the new state (showing progress indicator at the bottom of the list).
Please note that the state has changed (highlighted in green) which results in also changing the UI (progress indicator at the end of the list).

6. Once `loadNextPageSideEffect` gets the result back from backend, the side effect emits a new `PageLoadedAction`.
This Action contains a "payload" - the loaded data.

```kotlin
data class PageLoadedAction(val personsLoaded : List<Person>, val page : Int)
```

7. As any other Action `PageLoadedAction` goes through the `Reducer`. The Reducer processes this Action and computes a new state out of it by appending the loaded data to the already existing data (progress bar also is hidden).

Final remark:
This system allows you to create a plugin in system of `SideEffects` that are highly reusable and specific to do a single use case.

![Step12](https://raw.githubusercontent.com/freeletics/RxRedux/master/docs/step13.png)

Also `SideEffects` can be invoked by `Actions` from other `SideEffects`.

# FAQ

### I get a `StackoverflowException`
This is a common pitfall and is most of the time caused by the fact that an `SideEffect` emits an `Action` as output that it also consumes from upstream leading to a infinite loop.

```kotlin

val sideEffect: SideEffect<Int, State> = { actions, state ->
    actions.map { it * 2 }
}

val inputActions = Observable.just(1)

inputActions
    .reduxStore(
        initialState = "InitialState",
        sideEffects = listOf(sideEffect)
    ) { state, action ->
        ...
    }
```

The problem is that from upstream we get `Int 1`.
But since `SideEffect` reacts on that action `Int 1` too, it computes `1 * 2` and emits `2`, which then again gets handled by the same SideEffect ` 2 * 2 = 4` and emits `4`, which then again gets handled by the same SideEffect `4 * 2 = 8` and emits `8`, which then getst handled by the same SideEffect and so on (endless loop) ...

### Who processes an `Action` from upstream first: `Reducer` or `SideEffect`?

Since every Action runs through both `Reducer` and registered `SideEffects` this is a valid question.
Technically speaking `Reducer` gets every `Action` from upstream before the registered `SideEffects`.
The idea behind this is that a `Reducer` may change already the state before a `SideEffect` starts processing the action.

For example let's assume upstream only emits exactly one action (because then it's simpler to illustrate the sequence of workflow):

```kotlin
// 1. upstream emits events
val upstreamActions = Observable.just( SomeAction() )

val sideEffect1: SideEffect<Action, Action> = { actions, state ->
    // 3. Runs because of SomeAction
    actions.filter { it is SomeAction }.map { OtherAction() }
}

val sideEffect2: SideEffect<Action, Action> ={ actions, state ->
    // 5. This runs second because of OtherAction
    actions.filter { it is OtherAction }.map { YetAnotherAction() }
}

upstreamActions
    .reduxStore(
        initialState = ... ,
        sideEffects = listOf(sideEffect)
    ) { state, action ->
        // 2. This runs first because of SomeAction
        ...
        // 4. This runs again because of OtherAction (emitted by SideEffect1)
        ...
        // 5. This runs again because of YetAnotherAction emitted from SideEffect2)
    }.subscribe( ... )
```

So the workflow is as follows:
1. Upstream emits `SomeAction`
2. `reducer` processes `SomeAction`
3. `SideEffect1` reacts on `SomeAction` and emits `OtherAction` as output
4. `reducer` processes `OtherAction`
5. `SideEffect2` reacts on `OtherAction` and emits `YetAnotherAction`
6. `reducer` processes `YetAnotherAction`

### Can I use `val` and `fun` for `SideEffects` or `Reducer`?

Absolutely. `SideEffect` is just a type alias for a function `(actions: Observable<Action>, state: StateAccessor<State>) -> Observable<out Action>`.

In kotlin you can use a lambda for that like this:
```kotlin
val sideEffect1: SideEffect<Action, Action> = { actions, state ->
    actions.filter { it is SomeAction }.map { OtherAction() }
}
```

of write a function (instead of a lambda):

```kotlin
fun sideEffect2(actions : Observable<Action>, state : StateAccessor<State>) : Observable<Action> {
    return actions
        .filter { it is SomeAction }.map { OtherAction() }
}
```

Both are totally equal and can be used like that:

```kotlin
upstreamActions
    .reduxStore(
        initialState = ... ,
        sideEffects = listOf(sideEffect1, ::sideEffect2)
    ) { state, action ->
        ...
    }
    .subscribe( ... )
```

The same thing is valid for Reducer. Reducer is just a type alias for a function `(State, Action) -> State`
You can define your reducer as lambda or function:

```kotlin
val reducer = { state, action -> ... }

// or

fun reducer(state : State, action : Action) : State {
  ...
}
```

### Is `distinctUntilChanged` considered as best practice?
Yes it is because `.reduxStore(...)` is not taking care of only emitting state that has been changed
compared to previous state.
Therefore, `.distinctUntilChanged()` is considered as best practice.
```kotlin
actions
    .reduxStore( ... )
    .distinctUntilChanged()
    .subscribe { state -> view.render(state) }
```
