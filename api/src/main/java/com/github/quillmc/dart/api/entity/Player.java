package com.github.quillmc.dart.api.entity;

public interface Player {
    String getUsername();

    boolean isSneaking();

    boolean isOp();

    void sendMessage(String message);

    void disconnect(String reason);
}
