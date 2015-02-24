package com.cs408.studybuddy;

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

import java.util.List;

public class MessagesFragment extends Fragment implements MessageClientListener {

    private static final String TAG = MessagesFragment.class.getSimpleName();

    private View view;
    private MessageAdapter mMessageAdapter;
    private EditText mTxtRecipient;
    private EditText mTxtTextBody;
    private Button mBtnSend;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.messaging, container, false);

        mTxtRecipient = (EditText) view.findViewById(R.id.txtRecipient);
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
            sinch.stopClient();
        }
        super.onDestroy();
    }

    private void sendMessage() {
        String recipient = mTxtRecipient.getText().toString();
        String textBody = mTxtTextBody.getText().toString();
        if (recipient.isEmpty()) {
            //Toast.makeText(this, "No recipient added", Toast.LENGTH_SHORT).show();
            return;
        }
        if (textBody.isEmpty()) {
            //Toast.makeText(this, "No text message", Toast.LENGTH_SHORT).show();
            return;
        }

        SinchService.SinchServiceInterface sinch = StudyBuddyApplication.getSinchServiceInterface();
        sinch.sendMessage(recipient, textBody);
        mTxtTextBody.setText("");
    }

    private void setButtonEnabled(boolean enabled) {
        mBtnSend.setEnabled(enabled);
    }

    @Override
    public void onIncomingMessage(MessageClient client, Message message) {
        mMessageAdapter.addMessage(message, MessageAdapter.DIRECTION_INCOMING);
    }

    @Override
    public void onMessageSent(MessageClient client, Message message, String recipientId) {
        mMessageAdapter.addMessage(message, MessageAdapter.DIRECTION_OUTGOING);
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
}
