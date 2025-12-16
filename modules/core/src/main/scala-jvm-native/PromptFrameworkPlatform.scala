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

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

private trait PromptFrameworkPlatform[Result]:
  self: PromptFramework[Result] =>

  /** Evaluate this prompt synchronously until it produces a result or is
    * cancelled
    */
  def run(ip: InputProvider): Completion[Result] =
    ip.evaluate(self.handler)

  /** Evaluate this prompt asynchronously until it produces a result or is
    * cancelled
    */
  def runFuture(ip: InputProvider)(using
      ExecutionContext,
  ): Future[Completion[Result]] =
    ip.evaluateFuture(self.handler)
end PromptFrameworkPlatform
