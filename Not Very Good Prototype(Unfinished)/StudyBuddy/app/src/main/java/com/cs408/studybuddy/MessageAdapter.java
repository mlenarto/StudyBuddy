package com.cs408.studybuddy;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class MessageAdapter extends BaseAdapter {

    private List<ChatMessage> messages;
    private HashSet<String> messageIds;
    private HashSet<String> sendingMessages;
    private SimpleDateFormat dateFormatter;
    private LayoutInflater inflater;

    private static ChatMessageComparator messageComparator = new ChatMessageComparator();

    public MessageAdapter(LayoutInflater inflater) {
        this.inflater = inflater;
        messages = new ArrayList<ChatMessage>();
        messageIds = new HashSet<String>();
        sendingMessages = new HashSet<String>();
        dateFormatter = new SimpleDateFormat("HH:mm");
    }

    /**
     * Determines whether or not a message with the given ID is in the list.
     * @param id The ID to check for.
     * @return True if a message with the ID is in the list.
     */
    public synchronized boolean hasMessage(String id) {
        return messageIds.contains(id);
    }

    /**
     * Determines whether or not a message is in the list based off of its ID.
     * @param message The message to check for.
     * @return True if the message is in the list.
     */
    public synchronized boolean hasMessage(ChatMessage message) {
        return hasMessage(message.getId());
    }

    /**
     * Adds a message to the list if it is not already present.
     * @param message The message to add.
     * @return True if the message was not already in the list and was added.
     */
    public synchronized boolean addMessage(ChatMessage message) {
        if (hasMessage(message)) {
            return false; // Avoid duplicate messages
        }

        // Binary search the message list to get the position to insert the message
        /*int index = Collections.binarySearch(messages, message, messageComparator);
        if (index < 0) {
            index = ~index;
        }
        messages.add(index, message);*/

        // FLAW: Messages are out-of-order
        messages.add(message);
        messageIds.add(message.getId());
        notifyDataSetChanged();
        return true;
    }

    /**
     * Removes a message from the list.
     * @param message The message to remove.
     * @return True if the message was removed.
     */
    public synchronized boolean removeMessage(ChatMessage message) {
        if (!hasMessage(message)) {
            return false;
        }

        // Binary search the message list to find it
        /*int index = Collections.binarySearch(messages, message, messageComparator);
        if (index < 0) {
            return false;
        }
        messages.remove(index);*/

        // FLAW: Messages are out-of-order
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getId().equals(message.getId())) {
                messages.remove(i);
                break;
            }
        }
        messageIds.remove(message.getId());
        sendingMessages.remove(message.getId());
        notifyDataSetChanged();
        return true;
    }

    /**
     * Alters the "sending" status of a message.
     * @param list The ListView the message belongs to.
     * @param message The message to alter.
     * @param sending True to mark that the message is sending, false otherwise.
     */
    public synchronized void setSending(ListView list, ChatMessage message, boolean sending) {
        if (sending) {
            sendingMessages.add(message.getId());
        } else {
            sendingMessages.remove(message.getId());
        }
        updateItem(list, message);
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Object getItem(int i) {
        return messages.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int i) {
        return messages.get(i).getDirection().ordinal();
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        int direction = getItemViewType(i);

        if (convertView == null) {
            int res = 0;
            if (direction == ChatMessage.Direction.INCOMING.ordinal()) {
                res = R.layout.message_left;
            } else if (direction == ChatMessage.Direction.OUTGOING.ordinal()) {
                res = R.layout.message_right;
            }
            convertView = inflater.inflate(res, viewGroup, false);
        }

        ChatMessage message = messages.get(i);
        String name = message.getSender();

        TextView txtSender = (TextView) convertView.findViewById(R.id.txtSender);
        TextView txtMessage = (TextView) convertView.findViewById(R.id.txtMessage);
        TextView txtDate = (TextView) convertView.findViewById(R.id.txtDate);
        TextView txtSending = (TextView) convertView.findViewById(R.id.txtSending);

        txtSender.setText(name);
        txtMessage.setText(message.getBody());
        txtDate.setText(dateFormatter.format(message.getDate()));
        txtSending.setVisibility(sendingMessages.contains(message.getId()) ? View.VISIBLE : View.GONE);

        return convertView;
    }

    /**
     * Updates the view for an item in a list.
     * @param list The ListView the message belongs to.
     * @param item The message to update the view for.
     */
    private void updateItem(ListView list, ChatMessage item) {
        // Find the view in the list by searching between the first and last visible items
        int start = list.getFirstVisiblePosition();
        int end = list.getLastVisiblePosition();
        for (int i = start; i <= end; i++) {
            if (list.getItemAtPosition(i) != item) {
                continue;
            }

            // Force the view to update
            View view = list.getChildAt(i - start);
            getView(i, view, list);
            break;
        }
    }

    /**
     * Compares chat messages by date and then by Sinch ID.
     */
    private static class ChatMessageComparator implements Comparator<ChatMessage> {
        @Override
        public int compare(ChatMessage lhs, ChatMessage rhs) {
            if (lhs == rhs) {
                return 0;
            }
            int dateComparison = lhs.getDate().compareTo(rhs.getDate());
            if (dateComparison != 0) {
                return dateComparison;
            }
            return lhs.getId().compareTo(rhs.getId());
        }
    }
}
