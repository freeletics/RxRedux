# Deprecated

We've stopped maintaining this library because we are moving from RxJava to Kotlin Coroutines and `Flow`. Take a look at [FlowRedux](https://github.com/freeletics/FlowRedux) for a spiritual successor to this library.

# RxRedux

[![CircleCI](https://circleci.com/gh/freeletics/RxRedux.svg?style=svg)](https://circleci.com/gh/freeletics/RxRedux)
[![Download](https://maven-badges.herokuapp.com/maven-central/com.freeletics.rxredux/rxredux/badge.svg) ](https://maven-badges.herokuapp.com/maven-central/com.freeletics.rxredux/rxredux)

A Redux store implementation entirely based on RxJava (inspired by [redux-observable](https://redux-observable.js.org)) 
that helps to isolate side effects. RxRedux is (kind of) a replacement for RxJava's `.scan()` operator. 

![RxRedux In a Nutshell](https://raw.githubusercontent.com/freeletics/RxRedux/master/docs/rxredux.png)

## Dependency
Dependencies are hosted on Maven Central:

```groovy
implementation 'com.freeletics.rxredux:rxredux:1.0.1'
```
Keep in mind that this library is written in kotlin which means you also need to add `kotlin-stdlib` to a project using RxRedux.

#### Snapshot
Latest snapshot (directly published from master branch from Travis CI):

```groovy
allprojects {
    repositories {
        // Your repositories.
        // ...
        // Add url to snapshot repository
        maven {
            url "https://oss.sonatype.org/content/repositories/snapshots/"
        }
    }
}

```

```groovy
implementation 'com.freeletics.rxredux:rxredux:1.0.2-SNAPSHOT'
```

## How is this different from other Redux implementations like [Mobius](https://github.com/spotify/mobius)
In contrast to any other Redux inspired library out there, this library is really backed on top of RxJava (Mobius just offers some extensions to use RxJava for async works). 
This library offers a custom RxJava operator `.reduxStore( initialState, sideEffects, reducer )` and treats upstream events as `Actions`. 

## Kotlin coroutine-based implementation

If you are already using [Kotlin coroutines](https://kotlinlang.org/docs/reference/coroutines-overview.html) or planning to use it in your project - 
check [CoRedux](https://github.com/freeletics/coredux). This library implements Redux store, using same approach as RxRedux, 
but uses [Kotlin coroutines](https://kotlinlang.org/docs/reference/coroutines-overview.html) instead of RxJava.

# Redux Store
A Store is basically an observable container for state. 
This library provides a kotlin extension function `.reduxStore<State, Action>(initialState, sideEffects, reducer)` to create such a state container.
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
Every `SideEffect` can trigger multiple `Actions` (remember it returns `Observable<Action>`) which go through the `Reducer` but can also trigger other `SideEffects` registered for the corresponding `Action`.
An `Action` can also have a `payload`. For example, if you load some data from backend, you emit the loaded data as an `Action` like `data class DataLoadedAction (val data : FooData)`. 
The mantra an Action is a command to do something is still true: in that case it means data is loaded, do with it "something".

# StateAccessor
Whenever a `SideEffect` needs to know the current State it can use `StateAccessor` to grab the latest state from Redux Store. `StateAccessor` is basically just a function `() -> State` to grab the latest State anytime you need it.

# Usage
Let's create a simple Redux Store for Pagination: Goal is to display a list of `Persons` on screen.
**For a complete example check [the sample application incl. README](sample/README.md)**
but for the sake of simplicity let's stick with this simple "list of persons example":

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
fun reducer(state : State, action : Action) : State =
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

The [following video](https://youtu.be/M7lx9Y9ANYo) (click on it) illustrates the workflow:

[![RxRedux explanation](https://i.ytimg.com/vi/M7lx9Y9ANYo/hqdefault.jpg?sqp=-oaymwEXCNACELwBSFryq4qpAwkIARUAAIhCGAE=&rs=AOn4CLAqwunKP2_qGE0HYUlquWkFccM5MA)](https://youtu.be/M7lx9Y9ANYo)


0. Let's take a look at the following illustration:
The blue box is the `View` (think UI). 
The `Presenter` or `ViewModel` has not been drawn for the sake of readability but you can think of having such additional layers between View and Redux State Machine.
The yellow box represents a `Store`. 
The grey box is the `reducer`. 
The pink box is a `SideEffect`
Additionally, a green circle represents `State` and a red circle represents an `Action` (see next step).
On the right you see a UI mock of a mobile app to illustrate UI changes.

1. `NextPageAction` gets triggered from the UI (by scrolling at the end of the list). Every `Action` goes through the `reducer` and all `SideEffects` registered for this type of Action.

2. `Reducer` is not interested in `NextPageAction`. So while `NextPageAction` goes through the reducer, it doesn't change the state.

3. `loadNextPageSideEffect` (pink box), however, cares about `NextPageAction`. This is the trigger to run the side-effect.

4. So `loadNextPageSideEffect` takes `NextPageAction` and starts doing the job and makes the http request to load the next page from backend. Before doing that, this side effect starts with emitting `LoadPageAction`.

5. `Reducer` takes `LoadPageAction` emitted from the side effect and reacts on it by "reducing the state". 
This means `Reducer` knows how to react on `LoadPageAction` to compute the new state (showing progress indicator at the bottom of the list).
Please note that the state has changed (highlighted in green) which also results in changing the UI (progress indicator at the end of the list).

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


**For a complete example check [the sample application incl. README](https://github.com/freeletics/RxRedux/master/sample)**

# FAQ

## I get a `StackoverflowException`
This is a common pitfall and is most of the time caused by the fact that a `SideEffect` emits an `Action` as output that it also consumes from upstream leading to an infinite loop.

```kotlin

val sideEffect: SideEffect<State, Int> = { actions, state ->
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

## Who processes an `Action` first: `Reducer` or `SideEffect`?

Since every Action runs through both `Reducer` and registered `SideEffects` this is a valid question.
Technically speaking `Reducer` gets every `Action` from upstream before the registered `SideEffects`.
The idea behind this is that a `Reducer` may have already changed the state before a `SideEffect` start processing the Action.

For example let's assume upstream only emits exactly one Action (because then it's simpler to illustrate the sequence of workflow):

```kotlin
// 1. upstream emits events
val upstreamActions = Observable.just( SomeAction() )

val sideEffect1: SideEffect<State, Action> = { actions, state ->
    // 3. Runs because of SomeAction
    actions.filter { it is SomeAction }.map { OtherAction() }
}

val sideEffect2: SideEffect<State, Action> ={ actions, state ->
    // 5. Runs because of OtherAction
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
        // 6. This runs again because of YetAnotherAction emitted from SideEffect2)
    }.subscribe( ... )
```

So the workflow is as follows:
1. Upstream emits `SomeAction`
2. `reducer` processes `SomeAction`
3. `SideEffect1` reacts on `SomeAction` and emits `OtherAction` as output
4. `reducer` processes `OtherAction`
5. `SideEffect2` reacts on `OtherAction` and emits `YetAnotherAction`
6. `reducer` processes `YetAnotherAction`

## Can I use `val` and `fun` for `SideEffects` or `Reducer`?

Absolutely. `SideEffect` is just a type alias for a function `(actions: Observable<Action>, state: StateAccessor<State>) -> Observable<out Action>`.

In kotlin you can use a lambda for that like this:
```kotlin
val sideEffect1: SideEffect<State, Action> = { actions, state ->
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

The same is valid for Reducer. Reducer is just a type alias for a function `(State, Action) -> State`
You can define your reducer as lambda or function:

```kotlin
val reducer = { state, action -> ... }

// or

fun reducer(state : State, action : Action) : State {
  ...
}
```

## Is `distinctUntilChanged` considered as best practice?
Yes it is because `.reduxStore(...)` is not taking care of only emitting state that has been changed
compared to previous state.
Therefore, `.distinctUntilChanged()` is considered as best practice.
```kotlin
actions
    .reduxStore( ... )
    .distinctUntilChanged()
    .subscribe { state -> view.render(state) }
```

## What if I would like to have a SideEffect that returns no Action?

For example, let's say you just store something in a database but you don't need a Action as result
piped backed to your redux store. In that case you can simple use `Observable.empty()` like this:

```kotlin
fun saveToDatabaseSideEffect(actions : Observable<Action>, stateAccessor : StateAccessor<State>) {
    return actions.flatmap {
        saveToDb(...)
        Observable.empty<Action>()  // just return this to not emit an Action
    }
}
```

## How do I cancel ongoing `SideEffects` if a certain `Action` happens?

Let's assume you have a simple `SideEffect` that is triggered by `Action1`. 
Whenever `Action2` is emitted our `SideEffect` should stop. 
In RxJava this is quite easy to do by using `.takeUntil()`

```kotlin
fun mySideEffect(actions : Observable<Action>, stateAccessor : StateAccessor<State>) = 
    actions
        .ofType(Action1::class.java)
        .flatMap {
            ...
            doSomething()
        }
        .takeUntil(actions.ofType(Action2::class.java)) // Once Action2 triggers the whole SideEffect gets canceled.
```

## Do I need an Action to start observing data?
Let's say you would like to start observing a database right from the start inside your Store.
This sounds pretty much like as soon as you have subscribers to your Store and therefore you don't need a dedicated Action to start observing the database.

```kotlin
fun observeDatabaseSideEffect(_ : Observable<Action>, _ : StateAccessor<State>) : Observable<Action> =
    database // please notice that we dont use Observable<Action> at all
        .queryItems()
        .map { items -> DatabaseLoadedAction(items) }
```
