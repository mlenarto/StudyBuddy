package com.cs408.studybuddy;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.messaging.Message;
import com.sinch.android.rtc.messaging.MessageClient;
import com.sinch.android.rtc.messaging.MessageClientListener;
import com.sinch.android.rtc.messaging.MessageDeliveryInfo;
import com.sinch.android.rtc.messaging.MessageFailureInfo;
import com.sinch.android.rtc.messaging.WritableMessage;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MessagesFragment extends Fragment implements MessageClientListener {

    private static final String TAG = MessagesFragment.class.getSimpleName();

    private View view;
    private MessageAdapter mMessageAdapter;
    private EditText mTxtTextBody;
    private Button mBtnSend;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.messaging, container, false);

        mTxtTextBody = (EditText) view.findViewById(R.id.txtTextBody);

        mMessageAdapter = new MessageAdapter(inflater);
        ListView messagesList = (ListView) view.findViewById(R.id.lstMessages);
        messagesList.setAdapter(mMessageAdapter);

        mBtnSend = (Button) view.findViewById(R.id.btnSend);
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        SinchService.SinchServiceInterface sinch = StudyBuddyApplication.getSinchServiceInterface();
        sinch.addMessageClientListener(this);
        setButtonEnabled(true);
        loadMessageHistory();
        return view;
    }

    @Override
    public void onDestroy() {
        SinchService.SinchServiceInterface sinch = StudyBuddyApplication.getSinchServiceInterface();
        if (sinch != null) {
            sinch.removeMessageClientListener(this);
        }
        super.onDestroy();
    }

    private void sendMessage() {
        final String textBody = mTxtTextBody.getText().toString();
        if (textBody.isEmpty()) {
            Toast.makeText(view.getContext(), "No text message", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get all active users for now
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereNotEqualTo("objectId", ParseUser.getCurrentUser().getObjectId());
        query.selectKeys(Arrays.asList("objectId", "sinch"));
        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> parseUsers, ParseException e) {
                if (e != null) {
                    Toast.makeText(view.getContext(), getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                    return;
                }

                // Build a list of the user IDs
                ArrayList<String> recipients = new ArrayList<String>();
                for (ParseUser user : parseUsers) {
                    if (!user.has("sinch") || !user.getBoolean("sinch")) {
                        continue;
                    }
                    Log.d("MessagesFragment", "Add recipient " + user.getObjectId());
                    recipients.add(user.getObjectId());
                }

                // Send the message to every user at once
                SinchService.SinchServiceInterface sinch = StudyBuddyApplication.getSinchServiceInterface();
                WritableMessage message = new WritableMessage(recipients, textBody);
                sinch.sendMessage(message);

                mTxtTextBody.setText("");
            }
        });
    }

    private void setButtonEnabled(boolean enabled) {
        mBtnSend.setEnabled(enabled);
    }

    @Override
    public void onIncomingMessage(MessageClient client, Message message) {
        addIncomingMessageInBackground(message);
    }

    @Override
    public void onMessageSent(MessageClient client, Message message, String recipientId) {
        if (addOutgoingMessage(message)) {
            storeMessage(message);
        }
    }

    @Override
    public void onShouldSendPushData(MessageClient client, Message message, List<PushPair> pushPairs) {
        // Left blank intentionally
    }

    @Override
    public void onMessageFailed(MessageClient client, Message message,
            MessageFailureInfo failureInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sending failed: ")
                .append(failureInfo.getSinchError().getMessage());

        //Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show();
        Log.d(TAG, sb.toString());
    }

    @Override
    public void onMessageDelivered(MessageClient client, MessageDeliveryInfo deliveryInfo) {
        Log.d(TAG, "onDelivered");
    }

    private void addIncomingMessageInBackground(final Message message) {
        // Look up the user's display name
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("objectId", message.getSenderId());
        query.selectKeys(Arrays.asList("name"));
        query.setLimit(1);
        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> parseUsers, ParseException e) {
                if (e != null || parseUsers.size() == 0) {
                    return; // Ignore errors...
                }
                String username = parseUsers.get(0).getString("name");
                mMessageAdapter.addMessage(new ChatMessage(message, username, ChatMessage.Direction.INCOMING));
            }
        });
    }

    private boolean addOutgoingMessage(Message message) {
        // Just use the current user's display name
        String username = ParseUser.getCurrentUser().getString("name");
        return mMessageAdapter.addMessage(new ChatMessage(message, username, ChatMessage.Direction.OUTGOING));
    }

    private void storeMessage(final Message message) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("SavedMessages");
        query.whereEqualTo("sinchId", message.getMessageId());
        query.setLimit(1);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e != null) {
                    return; // TODO: Error handling?
                }
                if (parseObjects.size() != 0) {
                    return; // Don't store messages twice
                }
                ParseObject savedMessage = new ParseObject("SavedMessages");
                savedMessage.put("text", message.getTextBody());
                savedMessage.put("sender", ParseUser.getCurrentUser());
                savedMessage.put("date", message.getTimestamp());
                savedMessage.put("sinchId", message.getMessageId());
                savedMessage.saveInBackground();
            }
        });
    }

    private void loadMessageHistory() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("SavedMessages");
        query.include("sender");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e != null) {
                    return; // TODO: Error handling?
                }
                for (final ParseObject savedMessage : parseObjects) {
                    // Add the message
                    ParseUser sender = savedMessage.getParseUser("sender");
                    String username = sender.getString("name");
                    String text = savedMessage.getString("text");
                    Date date = savedMessage.getDate("date");
                    String sinchId = savedMessage.getString("sinchId");
                    WritableMessage message = new WritableMessage(username, text);
                    ChatMessage.Direction direction = (sender.getObjectId().equals(ParseUser.getCurrentUser().getObjectId()))
                            ? ChatMessage.Direction.OUTGOING
                            : ChatMessage.Direction.INCOMING;
                    mMessageAdapter.addMessage(new ChatMessage(sinchId, username, text, direction, date));
                }
            }
        });
    }
}
