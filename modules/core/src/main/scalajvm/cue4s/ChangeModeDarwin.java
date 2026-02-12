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
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;
import cue4s.ChangeMode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

class ChangeModeDarwin implements ChangeMode, GetWindowSize {

    private static ChangeModeDarwin INSTANCE;

    private ChangeModeDarwin() {}

    public static ChangeModeDarwin getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ChangeModeDarwin();
        }

        return INSTANCE;
    }

    @Override
    public WinSize getWinSize() {
        winsize attrs = new winsize();
        CLibrary.INSTANCE.ioctl(STDIN_FILENO, TIOCGWINSZ, attrs);

        return new WinSize(
            Integer.valueOf(attrs.ws_row),
            Integer.valueOf(attrs.ws_col)
        );
    }

    // Define the libc interface
    static interface CLibrary extends Library {
        CLibrary INSTANCE = Native.load("c", CLibrary.class);

        int tcgetattr(int fd, termios termios);

        int tcsetattr(int fd, int optional_actions, termios termios);

        int read(int fd, byte[] buf, int count);

        int ioctl(int fd, long request, Object... args);
    }

    @Structure.FieldOrder({ "ws_row", "ws_col" })
    public static class winsize extends Structure {

        public short ws_row;
        public short ws_col;
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
    public static final long TIOCGWINSZ = 0x40087468L;
    public static final int STDIN_FILENO = 0;
    public static final int TCSANOW = 0;
    public static final int ICANON = 256;
    public static final int ECHO = 0x0008;
    public static final int VTIME = 17;
    public static final int VMIN = 16;

    // Function to change mode
    private termios oldt = new termios(); // store original termios

    private byte[] buf = new byte[1];

    @Override
    public synchronized int getchar() {
        int cnt = CLibrary.INSTANCE.read(STDIN_FILENO, buf, 1);
        return cnt > 0 ? buf[0] : 0;
    }

    private Optional<Long> flags = Optional.empty();
    private Optional<byte[]> c_cc = Optional.empty();

    @Override
    public void changemode(int dir) {
        termios termiosAttrs = new termios();

        if (dir == 1 && flags.isEmpty() && c_cc.isEmpty()) {
            CLibrary.INSTANCE.tcgetattr(STDIN_FILENO, termiosAttrs); // get current terminal attributes
            flags = Optional.of(termiosAttrs.c_lflag.longValue());
            c_cc = Optional.of(termiosAttrs.c_cc.clone());
            termiosAttrs.c_cc[VTIME - 1] = 5;
            termiosAttrs.c_cc[VMIN - 1] = 0;
            termiosAttrs.c_lflag.setValue(
                termiosAttrs.c_lflag.longValue() & ~(ICANON | ECHO)
            ); // disable canonical mode and echo
            CLibrary.INSTANCE.tcsetattr(STDIN_FILENO, TCSANOW, termiosAttrs); // set new terminal attributes
        } else if (dir == 0 && flags.isPresent() && c_cc.isPresent()) {
            CLibrary.INSTANCE.tcgetattr(STDIN_FILENO, termiosAttrs); // get current terminal attributes
            flags.ifPresent(old -> termiosAttrs.c_lflag.setValue(old));
            c_cc.ifPresent(old -> termiosAttrs.c_cc = old.clone());
            flags = Optional.empty();
            CLibrary.INSTANCE.tcsetattr(STDIN_FILENO, TCSANOW, termiosAttrs); // set new terminal attributes
        }
    }
}
