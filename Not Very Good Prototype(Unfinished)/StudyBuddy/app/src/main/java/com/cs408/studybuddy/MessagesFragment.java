package com.cs408.studybuddy;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.messaging.Message;
import com.sinch.android.rtc.messaging.MessageClient;
import com.sinch.android.rtc.messaging.MessageClientListener;
import com.sinch.android.rtc.messaging.MessageDeliveryInfo;
import com.sinch.android.rtc.messaging.MessageFailureInfo;

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

import java.util.Arrays;
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

        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereNotEqualTo("objectId", ParseUser.getCurrentUser().getObjectId());
        query.selectKeys(Arrays.asList("objectId"));
        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> parseUsers, ParseException e) {
                if (e != null) {
                    Toast.makeText(view.getContext(), getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                    return;
                }
                SinchService.SinchServiceInterface sinch = StudyBuddyApplication.getSinchServiceInterface();
                for (ParseUser user : parseUsers) {
                    Log.d("MessagesFragment", "Sending message to " + user.getObjectId());
                    sinch.sendMessage(user.getObjectId(), textBody);
                }
                mTxtTextBody.setText("");
            }
        });
    }

    private void setButtonEnabled(boolean enabled) {
        mBtnSend.setEnabled(enabled);
    }

    @Override
    public void onIncomingMessage(MessageClient client, Message message) {
        addMessageInBackground(message, MessageAdapter.DIRECTION_INCOMING);
    }

    @Override
    public void onMessageSent(MessageClient client, Message message, String recipientId) {
        addMessageInBackground(message, MessageAdapter.DIRECTION_OUTGOING);
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

    private void addMessageInBackground(final Message message, final int direction) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("objectId", message.getSenderId());
        query.selectKeys(Arrays.asList("name"));
        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> parseUsers, ParseException e) {
                if (e != null || parseUsers.size() == 0) {
                    return; // Ignore errors...
                }
                String username = parseUsers.get(0).getString("name");
                mMessageAdapter.addMessage(message, direction, username);
            }
        });
    }
}
