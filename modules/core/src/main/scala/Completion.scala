/*
 * Copyright 2023 Neandertech
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cue4s

enum Completion[+Result]:
  case Finished(value: Result)
  case Fail(error: CompletionError)

  def toOption: Option[Result] =
    this match
      case Finished(value) => Some(value)
      case _               => None

  def toEither: Either[CompletionError, Result] =
    this match
      case Finished(value) => Right(value)
      case Fail(err)       => Left(err)
end Completion

object Completion:
  private[cue4s] def interrupted = Completion.Fail(CompletionError.Interrupted)
  private[cue4s] def error(msg: String) =
    Completion.Fail(CompletionError.Error(msg))

enum CompletionError extends Throwable:
  case Interrupted
  case Error(msg: String)
