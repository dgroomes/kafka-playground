// Oddly, when I try to download the sources for some dependency, I get a Gradle error saying "no repositories defined".
// This shouldn't happen because of course repositories are defined in each of the subprojects, but I think there was a
// regression in later versions of Intellij because Intellij is using the root project to download the sources, but it
// really should be using the subproject. I mean you could make a case for either behavior. So, I need to define `mavenCentral`
// as a repository in the root project.
repositories {
    mavenCentral()
}
