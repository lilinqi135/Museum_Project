package com.xiaozhi.event;

import com.xiaozhi.communication.common.ChatSession;
import org.springframework.context.ApplicationEvent;
import org.springframework.util.StringUtils;

import java.time.Clock;

/**
 * 设备端（客户端）发起打断的事件
 */
public class ChatAbortEvent extends ApplicationEvent {
    private String reason;//打断原因

    public ChatAbortEvent(Object source, String reason) {
        super(source);
        this.reason = StringUtils.hasText(reason) ? reason : "设备端打断";
    }

    public ChatAbortEvent(Object source, Clock clock) {
        super(source, clock);
        this.reason = "设备端打断";
    }

    public ChatSession getSession() {
        return (ChatSession) getSource();
    }

    public String getReason() {
        return reason;
    }
}
