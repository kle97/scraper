package io.playground.scraper.model.key;

public enum KeyboardCode {

    a('a', "KeyA", 97),
    b('b', "KeyB", 98),
    c('c', "KeyC", 99),
    d('d', "KeyD", 100),
    e('e', "KeyE", 101),
    ;

    private final char key;
    private final String code;
    private final int windowsVirtualKeyCode;


    KeyboardCode(char key, String code, int windowsVirtualKeyCode) {
        this.key = key;
        this.code = code;
        this.windowsVirtualKeyCode = windowsVirtualKeyCode;
    }
}
