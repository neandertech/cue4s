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

class CueBuilder private[cue4s] (hints: List[CueHint] = Nil):
  inline def getHints: List[CueHint] = hints

  private def add(hint: CueHint): CueBuilder = new CueBuilder(
    hint +: hints
  )
  inline def text(inline name: String)     = add(CueHint.Text(name))
  inline def options(inline name: String*) = add(CueHint.Options(name.toList))
  inline def multi(inline name: (String, Boolean)*) = add(
    CueHint.MultiSelect(name.toList)
  )
  inline def validate(f: String => Option[PromptError]) = add(
    CueHint.Validate(f)
  )
end CueBuilder
