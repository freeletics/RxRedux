cd library #Changing directory to your working directory
File=library/gradle.properties
if grep -q STRING_YOU_ARE_CHECKING_FOR "$File"; ##note the space after the string you are searching for
then
echo "Hooray!! Going to release the snapshot"
./gradlew uploadArtifacts
else
echo "Skipping deploying snapshot because it is a RELEASE --> Hooray! --> Workflow to publish RELEASE will be triggered"
fi
