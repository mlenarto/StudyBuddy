package com.cs408.studybuddy;

import android.app.Activity;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.sinch.android.rtc.messaging.Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MessageAdapter extends BaseAdapter {

    public static final int DIRECTION_INCOMING = 0;

    public static final int DIRECTION_OUTGOING = 1;

    private List<MessageInfo> mMessages;

    private SimpleDateFormat mFormatter;

    private LayoutInflater mInflater;

    public MessageAdapter(LayoutInflater inflater) {
        mInflater = inflater;
        mMessages = new ArrayList<MessageInfo>();
        mFormatter = new SimpleDateFormat("HH:mm");
    }

    public void addMessage(Message message, int direction, String username) {
        mMessages.add(new MessageInfo(message, direction, username));
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mMessages.size();
    }

    @Override
    public Object getItem(int i) {
        return mMessages.get(i);
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
        return mMessages.get(i).getDirection();
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        int direction = getItemViewType(i);

        if (convertView == null) {
            int res = 0;
            if (direction == DIRECTION_INCOMING) {
                res = R.layout.message_right;
            } else if (direction == DIRECTION_OUTGOING) {
                res = R.layout.message_left;
            }
            convertView = mInflater.inflate(res, viewGroup, false);
        }

        Message message = mMessages.get(i).getMessage();
        String name = mMessages.get(i).getUsername();

        TextView txtSender = (TextView) convertView.findViewById(R.id.txtSender);
        TextView txtMessage = (TextView) convertView.findViewById(R.id.txtMessage);
        TextView txtDate = (TextView) convertView.findViewById(R.id.txtDate);

        txtSender.setText(name);
        txtMessage.setText(message.getTextBody());
        txtDate.setText(mFormatter.format(message.getTimestamp()));

        return convertView;
    }

    private class MessageInfo {
        private Message message;
        private int direction;
        private String username;

        public MessageInfo(Message message, int direction, String username) {
            this.message = message;
            this.direction = direction;
            this.username = username;
        }

        public Message getMessage() {
            return message;
        }

        public int getDirection() {
            return direction;
        }

        public String getUsername() {
            return username;
        }
    }
}
