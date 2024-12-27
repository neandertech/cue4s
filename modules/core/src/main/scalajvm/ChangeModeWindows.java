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

package cue4s;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import java.util.List;
import java.util.Arrays;

class ChangeModeWindows implements ChangeMode {

	private static ChangeModeWindows INSTANCE;

	private ChangeModeWindows() {
	}

	public static ChangeModeWindows getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new ChangeModeWindows();
		}

		return INSTANCE;
	}

	// Define the msvcrt interface
	private static interface MsvcrtLibrary extends Library {
		MsvcrtLibrary INSTANCE = Native.load("msvcrt", MsvcrtLibrary.class);

		int _getch();
	}

	@Override
	public int getchar() {
		// _getch() is used instead of getchar(), as getchar() sometimes
		// requires an extra return to flush the buffer.
		int ch = ChangeModeWindows.MsvcrtLibrary.INSTANCE._getch();
		// For raw scan codes, this function returns either 0 or 0xE0.
		// To avoid ambiguity, force it to always be 0xE0.
		if (ch == 0) {return 0xE0;}
		else return ch;
	}

	@Override
	public void changemode(int dir) {
		// No need to change mode to use _getch()
	}
}
