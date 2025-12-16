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

/** [TerminalHandler] acts as a bridge between prompts and platform-specific
  * input providers.
  */
private[cue4s] abstract class TerminalHandler[Result]:
  /** Invoked by the [InputProvider] to setup a channel of communication from
    * the prompt to input provider. Implemented in [PromptFramework].
    */
  def setupBackchannel(notif: Next[Result] => Unit): Unit = ()

  /** Invoked by the [InputProvider] when a terminal event needs to be handled.
    * Implemented in [PromptFramework].
    */
  def apply(ev: TerminalEvent): Next[Result]
end TerminalHandler

/** Input provider encapsulates the logic that translates terminal events into
  * [TerminalEvent] values. Implementation is highly platform-specific
  *
  * @param terminal
  */
abstract class InputProvider(protected val terminal: Terminal)
    extends AutoCloseable,
      InputProviderPlatform

object InputProvider extends InputProviderCompanionPlatform
