/*
 * Copyright 2023 Anton Sviridov
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

package proompts;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;
import java.util.List;
import java.util.Arrays;

public class ChangeMode {

	// Define the libc interface
	public static interface CLibrary extends Library {
		CLibrary INSTANCE = Native.load("c", CLibrary.class);

		int tcgetattr(int fd, termios termios);

		int tcsetattr(int fd, int optional_actions, termios termios);

		int getchar();
	}

	// Define the termios structure
	@Structure.FieldOrder({ "c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_line", "c_cc", "c_ispeed", "c_ospeed" })
	public static class termios extends Structure {
		public NativeLong c_iflag;
		public NativeLong c_oflag;
		public NativeLong c_cflag;
		public NativeLong c_lflag;
		public byte c_line;
		public byte[] c_cc = new byte[32];
		public NativeLong c_ispeed;
		public NativeLong c_ospeed;
	}

	// Constants
	public static final int STDIN_FILENO = 0;
	public static final int TCSANOW = 0;
	public static final int ICANON = 256;
	public static final int ECHO = 0x0008;

	// Function to change mode
	public static termios oldt = new termios(); // store original termios

	public static void changemode(int dir) {
		termios newt = new termios();

		if (dir == 1) {
			CLibrary.INSTANCE.tcgetattr(STDIN_FILENO, oldt); // get current terminal attributes
			newt.c_iflag = oldt.c_iflag;
			newt.c_oflag = oldt.c_oflag;
			newt.c_cflag = oldt.c_cflag;
			newt.c_lflag = oldt.c_lflag;
			newt.c_line = oldt.c_line;
			newt.c_cc = oldt.c_cc;
			newt.c_ispeed = oldt.c_ispeed;
			newt.c_ospeed = oldt.c_ospeed;

			newt.c_lflag.setValue(newt.c_lflag.longValue() & ~(ICANON | ECHO)); // disable canonical mode and echo
			CLibrary.INSTANCE.tcsetattr(STDIN_FILENO, TCSANOW, newt); // set new terminal attributes
		} else {
			CLibrary.INSTANCE.tcsetattr(STDIN_FILENO, TCSANOW, oldt); // restore original terminal attributes
		}
	}
}
