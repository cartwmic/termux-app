/* Disposable emulator input driver: real non-alphabetic Linux volume buttons. */
#include <fcntl.h>
#include <linux/uinput.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <unistd.h>

#define CHECK(expr) do { if ((expr) < 0) { perror(#expr); exit(1); } } while (0)

static void event(int fd, int type, int code, int value) {
    struct input_event e = {0};
    e.type = type;
    e.code = code;
    e.value = value;
    if (write(fd, &e, sizeof(e)) != sizeof(e)) { perror("write event"); exit(1); }
}

int main(int argc, char **argv) {
    if (argc < 2 || (strcmp(argv[1], "VOLUME_UP") && strcmp(argv[1], "VOLUME_DOWN"))) return 2;
    int key = !strcmp(argv[1], "VOLUME_UP") ? KEY_VOLUMEUP : KEY_VOLUMEDOWN;
    int fd = open("/dev/uinput", O_WRONLY);
    CHECK(fd);
    CHECK(ioctl(fd, UI_SET_EVBIT, EV_KEY));
    CHECK(ioctl(fd, UI_SET_KEYBIT, KEY_VOLUMEUP));
    CHECK(ioctl(fd, UI_SET_KEYBIT, KEY_VOLUMEDOWN));
    struct uinput_setup setup = {0};
    setup.id.bustype = BUS_VIRTUAL;
    strcpy(setup.name, "termux-test-volume-buttons");
    CHECK(ioctl(fd, UI_DEV_SETUP, &setup));
    CHECK(ioctl(fd, UI_DEV_CREATE));
    /* Allow Android InputReader to discover the newly attached device. */
    usleep(1000000);
    event(fd, EV_KEY, key, 1);
    event(fd, EV_SYN, SYN_REPORT, 0);
    usleep(argc > 2 ? 1200000 : 100000);
    event(fd, EV_KEY, key, 0);
    event(fd, EV_SYN, SYN_REPORT, 0);
    usleep(200000);
    CHECK(ioctl(fd, UI_DEV_DESTROY));
    close(fd);
    return 0;
}
