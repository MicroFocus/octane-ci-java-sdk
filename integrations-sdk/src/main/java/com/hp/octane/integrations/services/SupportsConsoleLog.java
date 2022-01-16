package com.hp.octane.integrations.services;

public interface SupportsConsoleLog {

    void println(String msg);

    void print(String msg);

    void append(String msg);

    void newLine();
}
