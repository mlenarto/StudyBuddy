package com.cs408.studybuddy;

import com.parse.FindCallback;
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
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesFragment extends Fragment implements MessageClientListener {

    private static final String TAG = MessagesFragment.class.getSimpleName();
    private static final String DISPLAY_NAME_HEADER = "displayName"; // Used to cache display name in Sinch messages

    private View view;
    private MessageAdapter mMessageAdapter;
    private EditText mTxtTextBody;
    private Button mBtnSend;
    private ProgressBar loadingIndicator;
    private ListView messagesList;

    private HashMap<String, ChatMessage> pendingMessages = new HashMap<>(); // Messages that haven't been stored to the database yet, keyed by ID
    private ChatMessageHistory history;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.messaging, container, false);

        mTxtTextBody = (EditText) view.findViewById(R.id.txtTextBody);
        loadingIndicator = (ProgressBar) view.findViewById(R.id.loadingIndicator);

        mMessageAdapter = new MessageAdapter(inflater);
        messagesList = (ListView) view.findViewById(R.id.lstMessages);
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

        // Create a message object and update the UI immediately to make the user think the message sent successfully
        final WritableMessage message = new WritableMessage();
        message.setTextBody(textBody);
        final ChatMessage addedMessage = addOutgoingMessage(message);
        mTxtTextBody.setText("");

        // Get all active users for now
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereNotEqualTo("objectId", ParseUser.getCurrentUser().getObjectId()); // Skip the current user
        query.selectKeys(Arrays.asList("objectId", "sinch"));
        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> parseUsers, ParseException e) {
                if (e != null) {
                    // User query failed - notify the user and redact the message
                    displayNetworkError();
                    redactMessage(addedMessage);
                    return;
                }

                // Add each user ID to the message if they're registered with Sinch
                // TODO: What happens if there are no recipients?
                for (ParseUser user : parseUsers) {
                    if (!user.has("sinch") || !user.getBoolean("sinch")) {
                        continue; // Sinch fails to send the message completely if the user hasn't registered with it...
                    }
                    Log.d("MessagesFragment", "Add recipient " + user.getObjectId());
                    message.addRecipient(user.getObjectId());
                }

                // Cache our display name in a message header
                message.addHeader(DISPLAY_NAME_HEADER, (String)ParseUser.getCurrentUser().get("name"));

                // Send the message to every user at once
                SinchService.SinchServiceInterface sinch = StudyBuddyApplication.getSinchServiceInterface();
                pendingMessages.put(message.getMessageId(), addedMessage);
                sinch.sendMessage(message);
            }
        });
    }

    private void setButtonEnabled(boolean enabled) {
        mBtnSend.setEnabled(enabled);
    }

    @Override
    public void onIncomingMessage(MessageClient client, Message message) {
        addIncomingMessage(message);
    }

    @Override
    public void onMessageSent(MessageClient client, Message message, String recipientId) {
        // Only store the message if it is still pending
        if (pendingMessages.containsKey(message.getMessageId())) {
            storeMessage(message);

            // Remove the message from the pending list and save it to the history
            ChatMessage chatMessage = pendingMessages.remove(message.getMessageId());
            if (history != null) {
                history.saveMessage(chatMessage);
            }

            // Indicate that the message is done sending
            mMessageAdapter.setSending(messagesList, chatMessage, false);
        }
    }

    @Override
    public void onShouldSendPushData(MessageClient client, Message message, List<PushPair> pushPairs) {
        // Left blank intentionally
    }

    @Override
    public void onMessageFailed(MessageClient client, Message message, MessageFailureInfo failureInfo) {
        // Message failed to send - redact it
        Log.d(TAG, "onMessageFailed: " + failureInfo.getSinchError().getMessage());
        ChatMessage pending = pendingMessages.remove(message.getMessageId());
        if (pending == null) {
            return;
        }
        redactMessage(pending);
        displayNetworkError();
    }

    @Override
    public void onMessageDelivered(MessageClient client, MessageDeliveryInfo deliveryInfo) {
        // This method is kinda useless for us - basically it just lets you know once a user
        // actually receives a message, but it gets called even if the user who sent the message
        // isn't logged in
    }

    /**
     * Adds an incoming message to the message list.
     * @param message The message to add.
     */
    private void addIncomingMessage(final Message message) {
        if (mMessageAdapter.hasMessage(message.getMessageId())) {
            Log.d(TAG, "Ignoring message " + message.getMessageId() + " because it's already in the list");
            return; // Message is already in the list - don't bother with it
        }

        // If the display name was cached as part of the message,
        // use it instead of doing a Parse query
        Map<String, String> headers = message.getHeaders();
        if (headers.containsKey(DISPLAY_NAME_HEADER)) {
            String username = headers.get(DISPLAY_NAME_HEADER);
            Log.d(TAG, "Got cached username for " + message.getMessageId() + ": " + username);
            displayAndSaveMessage(new ChatMessage(message, username, ChatMessage.Direction.INCOMING));
            return;
        }

        // Use Parse to look up the user's display name
        Log.d(TAG, "Cached username not available for " + message.getMessageId() + "; falling back on Parse");
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("objectId", message.getSenderId());
        query.selectKeys(Arrays.asList("name"));
        query.setLimit(1);
        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> parseUsers, ParseException e) {
                if (e != null || parseUsers.size() == 0) {
                    // TODO: Retry the query if an error occurred?
                    Log.e(TAG, "Failed to get username for " + message.getMessageId() + ": " + ((e != null) ? e.getMessage() : " User not found"));
                    return;
                }
                String username = parseUsers.get(0).getString("name");
                Log.d(TAG, "Parse query returned username for " + message.getMessageId() + ": " + username);
                displayAndSaveMessage(new ChatMessage(message, username, ChatMessage.Direction.INCOMING));
            }
        });
    }

    /**
     * Adds an outgoing message to the message list.
     * @param message The message to add.
     * @return The ChatMessage object that was added to the list.
     */
    private ChatMessage addOutgoingMessage(WritableMessage message) {
        // Just use the current user's display name
        String username = ParseUser.getCurrentUser().getString("name");
        ChatMessage result = new ChatMessage(message.getMessageId(), username, message.getTextBody(), ChatMessage.Direction.OUTGOING, new Date());
        displayMessage(result);
        mMessageAdapter.setSending(messagesList, result, true);
        return result;
    }

    /**
     * Displays a message.
     * @param message The message to display.
     */
    private void displayMessage(ChatMessage message) {
        mMessageAdapter.addMessage(message);
    }

    /**
     * Displays a message, saving it to the message history if it is new.
     * @param message The message to display.
     */
    private void displayAndSaveMessage(ChatMessage message) {
        if (mMessageAdapter.addMessage(message) && history != null) {
            history.saveMessage(message);
        }
    }

    /**
     * Removes a message from the message list and puts its contents back into the textbox.
     * @param message The message to remove.
     * @return True if the message was redacted successfully.
     */
    private boolean redactMessage(ChatMessage message) {
        if (!mMessageAdapter.removeMessage(message)) {
            return false;
        }
        mTxtTextBody.setText(message.getBody());
        mTxtTextBody.setSelection(message.getBody().length()); // Move the cursor to the end
        Log.d(TAG, "Message " + message.getId() + " redacted");
        return true;
    }

    /**
     * Saves a message on the server.
     * @param message The message to save.
     */
    private void storeMessage(final Message message) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("SavedMessages");
        query.whereEqualTo("sinchId", message.getMessageId());
        query.setLimit(1);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e != null) {
                    // TODO: Error handling
                    // The issue though is that Sinch has already succeeded to send the message,
                    // so we can't redact it and display an error to the user because that means
                    // that people might get the message twice if the user re-sends it. Retrying
                    // the operation might be the best idea here.
                    return;
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
                Log.d(TAG, "Message " + message.getMessageId() + " saved to database");
            }
        });
    }

    /**
     * Loads and displays the message history.
     */
    private void loadMessageHistory() {
        showLoadingIndicator(true);
        loadCachedMessageHistory();
        downloadMessageHistory();
    }

    /**
     * Loads the message history cached on disk.
     */
    private void loadCachedMessageHistory() {
        Log.d(TAG, "Loading cached message history...");
        history = ChatMessageHistory.load(view.getContext(), ParseUser.getCurrentUser().getObjectId());
        for (ChatMessage message : history.getMessages()) {
            displayMessage(message);
        }
        Log.d(TAG, "Loaded " + history.getMessages().size() + " cached messages");
    }

    /**
     * Downloads the message history from the server and displays it.
     */
    private void downloadMessageHistory() {
        Log.d(TAG, "Downloading message history...");
        ParseQuery<ParseObject> query = ParseQuery.getQuery("SavedMessages");
        query.include("sender");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e != null) {
                    displayNetworkError();
                    showLoadingIndicator(false);
                    return;
                }
                for (final ParseObject savedMessage : parseObjects) {
                    // Add the message
                    ParseUser sender = savedMessage.getParseUser("sender");
                    String username = sender.getString("name");
                    String text = savedMessage.getString("text");
                    Date date = savedMessage.getDate("date");
                    String sinchId = savedMessage.getString("sinchId");
                    ChatMessage.Direction direction = (sender.getObjectId().equals(ParseUser.getCurrentUser().getObjectId()))
                            ? ChatMessage.Direction.OUTGOING
                            : ChatMessage.Direction.INCOMING;
                    displayAndSaveMessage(new ChatMessage(sinchId, username, text, direction, date));
                }
                showLoadingIndicator(false);
                Log.d(TAG, "Retrieved " + parseObjects.size() + " messages");
            }
        });
    }

    /**
     * Shows or hides the loading indicator.
     * @param show True to show the loading indicator, false to hide it.
     */
    private void showLoadingIndicator(boolean show) {
        loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Shows a toast to the user indicating that a network error occurred.
     */
    private void displayNetworkError() {
        Toast.makeText(view.getContext(), getString(R.string.network_error), Toast.LENGTH_SHORT).show();
    }
}
