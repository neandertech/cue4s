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

private object Platform extends PlatformShim:
  enum OS:
    case Windows, Linux, MacOS, Unknown

  lazy val os =
    detected.getOrElse(
      sys.props
        .getOrElse("os.name", "unknown")
        .toLowerCase
        .replaceAll("[^a-z0-9]+", "") match
        case p if p.startsWith("linux")                         => OS.Linux
        case p if p.startsWith("windows")                       => OS.Windows
        case p if p.startsWith("osx") || p.startsWith("macosx") => OS.MacOS
        case _                                                  => OS.Unknown,
    )
end Platform
