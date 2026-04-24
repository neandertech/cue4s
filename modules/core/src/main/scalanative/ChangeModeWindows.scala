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

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

object ChangeModeWindows extends ChangeModeNative:

  @extern
  private def SetConsoleOutputCP(wCodePageID: UInt): Boolean = extern
  @extern
  private def GetConsoleOutputCP(): UInt = extern

  // https://learn.microsoft.com/en-us/windows/win32/intl/code-page-identifiers
  private val UtfCodePage                   = 65001.toUInt
  private var consoleOutputCP: Option[UInt] = None

  def getchar(): Int =
    val ch = Msvcrt._getch()
    /* For raw scan codes, _getch() returns either 0 or 0xE0 as prefix.
     * Normalize 0x00 to 0xE0 to match JVM behavior and avoid
     * the 0-byte being skipped by KeyboardReadingThread.
     */
    if ch == 0 then 0xe0 else ch

  def changeMode(rawMode: Boolean): Boolean =
    // The code page logic here is required only on native: https://github.com/scala-native/scala-native/issues/4144
    if rawMode then
      val currentCP = GetConsoleOutputCP()
      if currentCP != UtfCodePage then
        consoleOutputCP = Some(currentCP)
        SetConsoleOutputCP(UtfCodePage)
    else
      // Restore code page to what it was before entering prompts
      consoleOutputCP.foreach: cp =>
        SetConsoleOutputCP(cp)

    rawMode
  end changeMode

  override def read(): Int = getchar()

  object Msvcrt:
    @extern() @link("msvcrt")
    def _getch(): Int = extern
  end Msvcrt
end ChangeModeWindows
