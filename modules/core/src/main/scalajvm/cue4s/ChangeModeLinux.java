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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

class ChangeModeLinux implements ChangeMode {

    private static ChangeModeLinux INSTANCE;

    private ChangeModeLinux() {}

    public static ChangeModeLinux getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ChangeModeLinux();
        }

        return INSTANCE;
    }

    // Define the libc interface
    private static interface CLibrary extends Library {
        CLibrary INSTANCE = Native.load("c", CLibrary.class);

        int tcgetattr(int fd, termios termios);

        int tcsetattr(int fd, int optional_actions, termios termios);

        int getchar();
    }

    // Define the termios structure
    @Structure.FieldOrder(
        {
            "c_iflag",
            "c_oflag",
            "c_cflag",
            "c_lflag",
            "c_line",
            "c_cc",
            "c_ispeed",
            "c_ospeed",
        }
    )
    public static class termios extends Structure {

        public int c_iflag;
        public int c_oflag;
        public int c_cflag;
        public int c_lflag;
        public byte c_line;
        public byte[] c_cc = new byte[32];
        public int c_ispeed;
        public int c_ospeed;
    }

    // Constants
    private static final int STDIN_FILENO = 0;
    private static final int TCSANOW = 0;
    private static final int ICANON = 0x0000002;
    private static final int ECHO = 0x0008;
    private static final int VTIME = 5;
    private static final int VMIN = 6;

    @Override
    public int getchar() {
        return CLibrary.INSTANCE.getchar();
    }

    private Optional<Integer> flags = Optional.empty();
    private Optional<byte[]> c_cc = Optional.empty();

    @Override
    public void changemode(int dir) {
        termios termiosAttrs = new termios();

        if (dir == 1 && flags.isEmpty() && c_cc.isEmpty()) {
            CLibrary.INSTANCE.tcgetattr(STDIN_FILENO, termiosAttrs); // get current terminal attributes
            flags = Optional.of(termiosAttrs.c_lflag);
            c_cc = Optional.of(termiosAttrs.c_cc.clone());
            termiosAttrs.c_cc[VTIME - 1] = 5;
            termiosAttrs.c_cc[VMIN - 1] = 0;
            termiosAttrs.c_lflag = termiosAttrs.c_lflag & ~(ICANON | ECHO); // disable canonical mode and echo
            CLibrary.INSTANCE.tcsetattr(STDIN_FILENO, TCSANOW, termiosAttrs); // set new terminal attributes
        } else if (dir == 0 && flags.isPresent() && c_cc.isPresent()) {
            CLibrary.INSTANCE.tcgetattr(STDIN_FILENO, termiosAttrs); // get current terminal attributes
            flags.ifPresent(old -> {
                termiosAttrs.c_lflag = old;
            });
            c_cc.ifPresent(old -> termiosAttrs.c_cc = old.clone());
            flags = Optional.empty();
            c_cc = Optional.empty();
            CLibrary.INSTANCE.tcsetattr(STDIN_FILENO, TCSANOW, termiosAttrs); // set new terminal attributes
        }
    }
}
