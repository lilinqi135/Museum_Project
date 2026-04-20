package com.xiaozhi.dialogue.service;

import com.xiaozhi.communication.common.ChatSession;

@FunctionalInterface
public interface WakeUp<T> {
    T wake(ChatSession  session, String wakeWordDetected);
}
