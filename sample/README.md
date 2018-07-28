# RxRedux Pagination example


# Testing

## Screenshot testing
Since our app is state driven and a state change also triggers a UI change, we can easily screenshot
test our app since we only have to wait unti a state transition happen and then make a screenshot.
The procedure looks as follows

1. Record the screenshots with `./gradlew executeScreenshotTests -Precord`.
You have to run this whenever you change your UI on purpose.
2. Run verification with `./gradlew executeScreenshotTests`.
This runs the test and compares the screenshots with the previously recored screenshots (see step 1.)
3. See test report in `RxRedux/sample/build/reports/shot/verification/index.html`
