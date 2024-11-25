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

class TextFormatting(enabled: Boolean):
  private def colored(msg: String)(f: String => fansi.Str) =
    if enabled then f(msg).toString else msg
  extension (t: String)
    def bold =
      colored(t)(fansi.Bold.On(_))
    def underline =
      colored(t)(fansi.Underlined.On(_))
    def green =
      colored(t)(fansi.Color.Green(_))
    def cyan =
      colored(t)(fansi.Color.Cyan(_))
    def red =
      colored(t)(fansi.Color.Red(_))
  end extension
end TextFormatting
