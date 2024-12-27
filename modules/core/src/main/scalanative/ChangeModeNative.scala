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

import scala.scalanative.meta.LinktimeInfo

trait ChangeModeNative:
  def changeMode(rawMode: Boolean): Boolean
  def getchar(): Int

object ChangeModeNative:
  def instance: ChangeModeNative =
    if LinktimeInfo.isMac then ChangeModeMac
    else if LinktimeInfo.isLinux then ChangeModeLinux
    else if LinktimeInfo.isWindows then ChangeModeWindows
    else
      sys.error(
        "Cue4s failed to detect the operating system, it is likely unsupported. Please raise an issue (or even a PR!) at https://github.com/neandertech/cue4s",
      )
    end if
end ChangeModeNative
