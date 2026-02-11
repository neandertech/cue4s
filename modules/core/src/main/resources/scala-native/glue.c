#if defined(__linux__) || defined(__APPLE__)

#include "sys/signal.h"
#include <stdio.h>
#include <sys/ioctl.h>

int scalanative_sigwinch() {
    return SIGWINCH;
}

int scalanative_get_window_size(struct winsize* ws) {
    return ioctl(0, TIOCGWINSZ, ws);
}

#endif
