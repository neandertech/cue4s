<!--toc:start-->
- [Cue4s](#cue4s)
  - [Installation](#installation)
  - [Platform support](#platform-support)
  - [Usage](#usage)
  - [Cats Effect integration](#cats-effect-integration)
<!--toc:end-->

## Cue4s

Experimental Scala library for CLI prompts that works on JVM, JS, and Native.

The inspiration is taken from a JS library [prompts](https://github.com/terkelg/prompts#options), and the eventual goal is to have cue4s support all the same functionality. We're nowhere near that yet of course.

Library is experimental and is only available in Sonatype Snapshots, mainly 
because there is plenty of known bugs (they're present even in the GIF below!), so we are still not ready for a 
proper release. If you are interested in this effort, please create issues 
and contribute fixes!

![CleanShot 2024-08-01 at 09 58 23](https://github.com/user-attachments/assets/a369e014-40d8-4c35-9738-434ba10d02d9)

### Installation

 - Scala CLI: `//> using dep tech.neander::cue4s::<version>`
 - SBT: `libraryDependencies += "tech.neander" %%% "cue4s" % "<version>"`

### Platform support

On JS, we can only execute the prompts asynchronously, so the minimal 
usable implementation of a prompt will always return `Future[Completion[Result]]`.

On JVM and Native, we can execute prompts synchronously, so the simplest 
implementation returns `Completion[Result]` - but methods wrapping the result in `Future` are provided for convenience.

This is encoded in the methods (`sync` or `future`, or both) available on the `cue4s.Prompts` class, which is the main entry point for the library.

This library is published for Scala.js 1.16.0+, Scala Native 0.5, and JVM.

### Usage

This example is runnable on both JVM and Native (note how we're using `sync`).
For this to work on JS, you need to use the `future`-based methods, example for that is provided in [examples folder](./modules/example/src/main/).

```scala mdoc:compile-only
import cue4s.*

case class Info(
    day: Option[String] = None,
    work: Option[String] = None,
    letters: Set[String] = Set.empty
)

var info = Info()

val prompts = Prompts()

val day = prompts
  .sync(
    Prompt.SingleChoice("How was your day?", List("great", "okay"))
  )
  .toResult
info = info.copy(day = day)

val work = prompts.sync(Prompt.Input("Where do you work?")).toResult
info = info.copy(work = work)

val letters = prompts
  .sync(
    Prompt.MultipleChoice(
      "What are your favourite letters?",
      ('A' to 'F').map(_.toString).toList
    )
  )
  .toResult
info = info.copy(letters = letters.fold(Set.empty)(_.toSet))

prompts.close() // important to put the terminal back into line mode
```

### Cats Effect integration

A simple Cats Effect integration is provided, which wraps the future-based implementation of terminal interactions. 

The integration is available only for JVM and JS targets.


**Installation**

 - Scala CLI: `//> using dep tech.neander::cue4s-cats-effect::<version>`
 - SBT: `libraryDependencies += "tech.neander" %%% "cue4s-cats-effect" % "<version>"`

**Usage**

```scala mdoc:compile-only
import cats.effect.*
import cue4s.*, catseffect.*

case class Info(
    day: Option[String] = None,
    work: Option[String] = None,
    letters: Set[String] = Set.empty
)

object ioExample extends IOApp.Simple:
  def run: IO[Unit] =
    PromptsIO().use: prompts =>
      for
        ref <- IO.ref(Info())

        day <- prompts
          .io(
            Prompt.SingleChoice("How was your day?", List("great", "okay"))
          )
          .map(_.toResult)
          .flatTap(day => ref.update(_.copy(day = day)))

        work <- prompts
          .io(
            Prompt.Input("Where do you work?")
          )
          .map(_.toResult)
          .flatTap(work => ref.update(_.copy(work = work)))

        letter <- prompts
          .io(
            Prompt.MultipleChoice(
              "What are your favourite letters?",
              ('A' to 'F').map(_.toString).toList
            )
          )
          .map(_.toResult)
          .flatTap(letter =>
            ref.update(_.copy(letters = letter.fold(Set.empty)(_.toSet)))
          )

        _ <- ref.get.flatMap(IO.println)
      yield ()

end ioExample
```
