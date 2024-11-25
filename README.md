<!--toc:start-->
- [Cue4s](#cue4s)
  - [Installation](#installation)
  - [Platform support](#platform-support)
  - [Usage](#usage)
  - [Auto-derivation for case classes](#auto-derivation-for-case-classes)
  - [Cats Effect integration](#cats-effect-integration)
<!--toc:end-->

## Cue4s

[![cue4s Scala version support](https://index.scala-lang.org/neandertech/cue4s/cue4s/latest.svg)](https://index.scala-lang.org/indoorvivants/cue4s/cue4s)

Scala 3 library for CLI prompts that works on JVM, JS, and Native.

The inspiration is taken from a JS library [prompts](https://github.com/terkelg/prompts#options), and the eventual goal is to have cue4s support all the same functionality. 

https://github.com/user-attachments/assets/c762e557-26a2-4d24-bee2-23dd3443a21b

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
  .toOption
info = info.copy(day = day)

val work = prompts.sync(Prompt.Input("Where do you work?")).toOption
info = info.copy(work = work)

val letters = prompts
  .sync(
    Prompt.MultipleChoice.withAllSelected(
      "What are your favourite letters?",
      ('A' to 'F').map(_.toString).toList
    )
  )
  .toOption
info = info.copy(letters = letters.fold(Set.empty)(_.toSet))

prompts.close() // important to put the terminal back into line mode
```

### Auto-derivation for case classes

cue4s includes an experimental auto-derivation for case classes (and only them, currently),
allowing you to create prompt chains:

```scala mdoc:compile-only
import cue4s.*
val validateName: String => Option[PromptError] = s =>
    Option.when(s.trim.isEmpty)(PromptError("name cannot be empty!"))

case class Attributes(
  @cue(_.text("Your name").validate(validateName))
  name: String,
  @cue(_.text("Checklist").multi("Wake up" -> true, "Grub a brush" -> true, "Put a little makeup" -> false))
  doneToday: Set[String],
  @cue(_.text("What did you have for breakfast").options("eggs", "sadness"))
  breakfast: String
) derives PromptChain

val attributes: Attributes = 
    Prompts.use(): p =>
      p.sync(PromptChain[Attributes]).getOrThrow
```

There is no generic mechanism to define how parameters of different types will be handled, just a set of 
rules that felt right at the time of writing this library:

1. If the type is `String`, and `.options(...)` is present in annotation, the prompt will become `SingleChoice`
2. If the type is `F[String]` where `F` is one of `List, Vector, Set`, and either `.options(...)` or `.multi(...)` are present,
   then the prompt will become `MultipleChoice`
3. If the type is `Option[String]`, then _empty_ value will be turned into `None` (check for emptiness will be run before any validation)

In the future more combinations can be added.

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
          .map(_.toOption)
          .flatTap(day => ref.update(_.copy(day = day)))

        work <- prompts
          .io(
            Prompt.Input("Where do you work?")
          )
          .map(_.toOption)
          .flatTap(work => ref.update(_.copy(work = work)))

        letter <- prompts
          .io(
            Prompt.MultipleChoice.withNoneSelected(
              "What are your favourite letters?",
              ('A' to 'F').map(_.toString).toList
            )
          )
          .map(_.toOption)
          .flatTap(letter =>
            ref.update(_.copy(letters = letter.fold(Set.empty)(_.toSet)))
          )

        _ <- ref.get.flatMap(IO.println)
      yield ()

end ioExample
```
