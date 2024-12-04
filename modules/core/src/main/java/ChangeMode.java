package cue4s;

interface ChangeMode {
  abstract void changemode(int dir);
  abstract int getchar();

  static ChangeMode forDarwin() {
    return ChangeModeDarwin.getInstance();
  }

  static ChangeMode forLinux() {
    return ChangeModeLinux.getInstance();
  }
} 
