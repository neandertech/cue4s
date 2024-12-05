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
//> using dep tech.neander::cue4s::latest.release

import cue4s.*

Prompts.sync.use: prompts =>
  val day = prompts
    .singleChoice("How was your day?", List("great", "okay"))
    .getOrThrow

  val work = prompts.text("Where do you work?").getOrThrow

  val letters = prompts.multiChoiceAllSelected(
      "What are your favourite letters?",
      ('A' to 'F').map(_.toString).toList
    ).getOrThrow
```

### Auto-derivation for case classes

cue4s includes an experimental auto-derivation for case classes (and only them, currently),
allowing you to create prompt chains:

```scala mdoc:compile-only

//> using tech.neander::cue4s::latest.release

import cue4s.*

val validateName: String => Option[PromptError] = s =>
    Option.when(s.trim.isEmpty)(PromptError("name cannot be empty!"))

case class Attributes(
  @cue(_.text("Your name").validate(validateName))
  name: String,
  @cue(_.text("Checklist").multi("Wake up" -> true, "Grub a brush" -> true, "Put a little makeup" -> false))
  doneToday: Set[String],
  @cue(_.text("What did you have for breakfast").options("eggs", "sadness"))
  breakfast: String,
  @cue(_.text("Do you want to build a snowman?"))
  snowman: Boolean,
  @cue(_.text("How old are you?"))
  age: Int,
  @cue(_.text("What is the value of PI?"))
  pi: Float
) derives PromptChain

val attributes: Attributes = 
  Prompts.sync.use: p =>
    p.run(PromptChain[Attributes]).getOrThrow
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
//> using dep tech.neander::cue4s-cats-effect::latest.release

import cue4s.catseffect.*

import cats.effect.*
import cats.syntax.all.*

case class Info(
    day: String,
    work: String,
    letters: List[String],
)

object ioExample extends IOApp.Simple:
  def run: IO[Unit] =
    PromptsIO.make.use: prompts =>
      for
        _ <- IO.println("let's go")

        day = prompts
          .singleChoice("How was your day?", List("great", "okay"))
          .map(_.toEither)
          .flatMap(IO.fromEither)

        work = prompts
          .text("Where do you work?")
          .map(_.toEither)
          .flatMap(IO.fromEither)

        letter = prompts
          .multiChoiceAllSelected(
            "What are your favourite letters?",
            ('A' to 'F').map(_.toString).toList,
          )
          .map(_.toEither)
          .flatMap(IO.fromEither)

        info <- (day, work, letter).mapN(Info.apply)

        _ <- IO.println(info)
      yield ()

end ioExample
```
