package com.cs408.studybuddy;

import com.sinch.android.rtc.messaging.Message;

import java.util.Date;

/**
 * Holds information for a message that was sent in chat.
 * Created by Aaron on 3/7/2015.
 */
public class ChatMessage {
    private String body;
    private Direction direction;
    private String sender;
    private Date date;
    private String id;

    /**
     * Message direction types.
     */
    public enum Direction
    {
        /**
         * Indicates that a message was sent by another user.
         */
        INCOMING,

        /**
         * Indicates that the message was sent by the current user.
         */
        OUTGOING
    }

    /**
     * Constructs a new ChatMessage object.
     * @param id The internal ID string for the message.
     * @param sender The display name of the person who sent the message.
     * @param body The message body.
     * @param direction The direction of the message.
     * @param date The time the message was sent.
     */
    public ChatMessage(String id, String sender, String body, Direction direction, Date date) {
        this.body = body;
        this.sender = sender;
        this.direction = direction;
        this.date = date;
        this.id = id;
    }

    /**
     * Constructs a new ChatMessage object from a Sinch message.
     * @param message The Sinch message.
     * @param sender The display name of the person who sent the message.
     * @param direction The direction of the message.
     */
    public ChatMessage(Message message, String sender, Direction direction) {
        this(message.getMessageId(), sender, message.getTextBody(), direction, new Date());
    }

    /**
     * Gets the internal ID string for the chat message.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the display name of the person who sent the message.
     */
    public String getSender() {
        return sender;
    }

    /**
     * Gets the body of the message.
     */
    public String getBody() {
        return body;
    }

    /**
     * Gets the direction in which the message was sent.
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Gets the time the message was sent.
     */
    public Date getDate() {
        return date;
    }
}