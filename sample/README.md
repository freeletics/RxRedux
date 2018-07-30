# RxRedux Pagination example

This example shows an app that loads a list of popular repositories (number of stars) on Github.
It users github api endpoint to query popular repositories.
Github doesn't give us the whole list of repositories in one single response but offers pagination
to load the next page once you hit the end of the list and need more repositories to display.

![First page]()
![Second page]()

To implement that of course we use `RxRedux`. The User can trigger `LoadFirstPageAction` and
`LoadNextPageAction`.
This Actions are handled by:`
- `fun loadFirstPageSideEffect(action : Observable<Action>) : Observable<Action>`
- `fun loadNextPageSideEffect(action : Observable<Action>) : Observable<Action>`

Furthermore, if a error occurs while loading the next page an internal Action
(not triggered by the user)  `ErrorLoadingPageAction` is emitted
which is handled by another internal SideEffect:
`fun showAndHideLoadingErrorSideEffect(action : Observable<Action>) : Observable<Action>` takes care
of showing and hiding a `SnackBar` that is used to display an error on screen.


# Testing
Testing is fairly easy in a state machine based architecure because all you have to do trigger
input actions and then check for state changes caused by an action.
So at the end it's basically `assertEquals(expectedState, actualStates)`.

## Functional testing
Of course we could test our side effects and reducers individually.
However, since they are pure functions, we believe that writing functional tests for the whole system
adds more value then single unit tests.
Actually we have two kind of functional tests:

1. Functional tests that run on JVM: Here we basically have no real UI but just a mocked one that
records states that should be rendered over time. Eventually, this allows us to do `assertEquals(expectedState, recordedStates)`
2. Functional tests that run on real Android Device: Same idea as for functional tests on JVM, in this case, however, we run our tests on a real android device interacting with real android UI widgets. We use `ViewBinding` to interact with UI Widgets. While running the function tests we use a `RecordingViewBinding` that again records the state changes over time which then allows us to check `assertEquals(expectedState, recordedStates)`.

## Screenshot testing
Since our app is state driven and a state change also triggers a UI change, we can easily screenshot
test our app since we only have to wait until a state transition happen and then make a screenshot.
The procedure looks as follows

1. Record the screenshots with `./gradlew executeScreenshotTests -Precord`.
You have to run this whenever you change your UI on purpose.
2. Run verification with `./gradlew executeScreenshotTests`.
This runs the test and compares the screenshots with the previously recored screenshots (see step 1.)
3. See test report in `RxRedux/sample/build/reports/shot/verification/index.html`

Please keep in mind that you always have to use the same device to run your screenshot test.
The screenshots added to this repository have been taken from a Nexus 5X emulator (default settings) running Android API 26.
