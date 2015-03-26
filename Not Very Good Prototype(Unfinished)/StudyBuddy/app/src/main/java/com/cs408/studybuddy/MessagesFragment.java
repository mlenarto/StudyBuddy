package com.cs408.studybuddy;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.messaging.Message;
import com.sinch.android.rtc.messaging.MessageClient;
import com.sinch.android.rtc.messaging.MessageClientListener;
import com.sinch.android.rtc.messaging.MessageDeliveryInfo;
import com.sinch.android.rtc.messaging.MessageFailureInfo;
import com.sinch.android.rtc.messaging.WritableMessage;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesFragment extends Fragment implements MessageClientListener {

    private static final String TAG = MessagesFragment.class.getSimpleName();

    private static final String PREFS_NAME = "MessagePrefs";
    private static final String LAST_GROUP_PREF = "lastGroup";

    private static final String DISPLAY_NAME_HEADER = "displayName"; // Used to cache display name in Sinch messages
    private static final String GROUP_ID_HEADER = "groupId";         // Used to set the group ID for a Sinch message

    private View view;
    private MessageAdapter mMessageAdapter;
    private EditText mTxtTextBody;
    private Button mBtnSend;
    private ProgressBar loadingIndicator;
    private ListView messagesList;
	private RelativeLayout sendLayout;
	private TextView noGroup;
    private LoadMessageHistoryTask loadTask;
	ParseObject currentGroup;

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

		sendLayout = (RelativeLayout) view.findViewById(R.id.relSendMessage);
		noGroup = (TextView) view.findViewById(R.id.no_group);

        SinchService.SinchServiceInterface sinch = StudyBuddyApplication.getSinchServiceInterface();
        sinch.addMessageClientListener(this);
        setButtonEnabled(true);
//        loadMessageHistoryInBackground();
        return view;
    }

	@Override
	public void onResume() {
		super.onResume();
		currentGroup = (ParseObject) ParseUser.getCurrentUser().get("currentRequest");
		if(currentGroup == null) {
			messagesList.setVisibility(View.GONE);
			sendLayout.setVisibility(View.GONE);
			noGroup.setVisibility(View.VISIBLE);
		} else if(CheckInternet()) {
			try {
				currentGroup.fetchIfNeeded();
				ParseQuery<ParseObject> query = ParseQuery.getQuery("HelpRequest");
				query.get(currentGroup.getObjectId());
			} catch (ParseException e) {
				if(e.getCode() == ParseException.OBJECT_NOT_FOUND) {
					ParseUser.getCurrentUser().remove("currentRequest");
					ParseUser.getCurrentUser().put("isHelper", false);
					ParseUser.getCurrentUser().remove("cacheHelpers");
					ParseUser.getCurrentUser().remove("cacheMembers");
					ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
						@Override
						public void done(ParseException e) {
							if (e == null) {
								//user saved properly.
								currentGroup = null;
								ParseUser.getCurrentUser().pinInBackground();     //cache (lack of) group info
							} else {
								//user was not saved properly.
								e.printStackTrace();
							}
						}
					});
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							messagesList.setVisibility(View.GONE);
							sendLayout.setVisibility(View.GONE);
							noGroup.setVisibility(View.VISIBLE);
							displayDeletedGroupAlert();
						}
					});
				} else {
					loadMessageHistoryInBackground();
				}
			}
		} else {
			loadMessageHistoryInBackground();
		}

	}

    @Override
    public void onDestroy() {
        if (loadTask != null) {
            // Cancel loading
            loadTask.cancel(true);
            loadTask = null;
        }
        SinchService.SinchServiceInterface sinch = StudyBuddyApplication.getSinchServiceInterface();
        if (sinch != null) {
            sinch.removeMessageClientListener(this);
        }
        if (history != null) {
            history.close();
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

        // Get all users belonging to our current request
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        final ParseObject request = (ParseObject)ParseUser.getCurrentUser().get("currentRequest");
        query.whereNotEqualTo("objectId", ParseUser.getCurrentUser().getObjectId()); // Skip the current user
        query.whereEqualTo("currentRequest", request);
        query.selectKeys(Arrays.asList("objectId", "sinch"));
        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> parseUsers, ParseException e) {
                if (e != null) {
                    // User query failed - notify the user and remove the message
                    displayNetworkError();
                    removeMessage(addedMessage);
                    return;
                }

                // TODO: Sinch only supports sending to 10 users at a time.
                // This can probably be hacked around by clearing the recipient list and resending
                // the message for each group of 10 users.

                // Add each user ID to the message if they're registered with Sinch
                for (ParseUser user : parseUsers) {
                    if (!user.has("sinch") || !user.getBoolean("sinch")) {
                        continue; // Sinch fails to send the message completely if the user hasn't registered with it...
                    }
                    Log.d("MessagesFragment", "Add recipient " + user.getObjectId());
                    message.addRecipient(user.getObjectId());
                }
                if (message.getRecipientIds().size() == 0) {
                    // Sinch doesn't let you send a message to nobody, so just finish sending it and return
                    finishSendingMessage(addedMessage);
                    return;
                }

                // Cache our display name and group ID in message headers
                message.addHeader(DISPLAY_NAME_HEADER, (String)ParseUser.getCurrentUser().get("name"));
                message.addHeader(GROUP_ID_HEADER, request.getObjectId());

                // Send the message
                pendingMessages.put(message.getMessageId(), addedMessage);
                SinchService.SinchServiceInterface sinch = StudyBuddyApplication.getSinchServiceInterface();
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
        if (pendingMessages.containsKey(message.getMessageId())) {
            // Message is still pending - save it
            ChatMessage chatMessage = pendingMessages.remove(message.getMessageId());
            finishSendingMessage(chatMessage);
        }
    }

    @Override
    public void onShouldSendPushData(MessageClient client, Message message, List<PushPair> pushPairs) {
        // Left blank intentionally
    }

    @Override
    public void onMessageFailed(MessageClient client, Message message, MessageFailureInfo failureInfo) {
        // Message failed to send - remove it
        Log.d(TAG, "onMessageFailed: " + failureInfo.getSinchError().getMessage());
        ChatMessage pending = pendingMessages.remove(message.getMessageId());
        if (pending == null) {
            return;
        }
        removeMessage(pending);
        displayNetworkError();
    }

    @Override
    public void onMessageDelivered(MessageClient client, MessageDeliveryInfo deliveryInfo) {
        // This method is kinda useless for us - basically it just lets you know once a user
        // actually receives a message, but it gets called even if the user who sent the message
        // isn't logged in
    }

    /**
     * Finishes sending a message, saving it to the server and to local history.
     * @param message
     */
    private void finishSendingMessage(ChatMessage message) {
        storeMessage(message);
        if (history != null) {
            history.saveMessage(message);
        }
        setSending(message, false);
        updateRequestTime();
    }

    /**
     * Updates the remaining time in the current request if it is less than 30 minutes.
     */
    private void updateRequestTime() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("HelpRequest");
        query.getInBackground(ParseUser.getCurrentUser().getString("currentRequest"), new GetCallback<ParseObject>() {
			@Override
			public void done(ParseObject request, ParseException e) {
				if (e == null) {
					//object retrieved
					if (request.getNumber("duration").doubleValue() < 1800000) {
						request.put("duration", 1800000);
						request.saveInBackground();
					}
				} else {
					//something went wrong
				}
			}
		});
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

        // Check the message's group ID
        Map<String, String> headers = message.getHeaders();
        if (!headers.containsKey(GROUP_ID_HEADER)) {
            Log.d(TAG, "Ignoring message " + message.getMessageId() + " because it does not have a group ID");
            return;
        }
        String groupId = headers.get(GROUP_ID_HEADER);
        ParseObject request = (ParseObject)ParseUser.getCurrentUser().get("currentRequest");
        if (!groupId.equals(request.getObjectId())) {
            Log.d(TAG, "Ignoring message " + message.getMessageId() + " because its group ID does not match");
            return;
        }

        // If the display name was cached as part of the message,
        // use it instead of doing a Parse query
        if (headers.containsKey(DISPLAY_NAME_HEADER)) {
            String username = headers.get(DISPLAY_NAME_HEADER);
            Log.d(TAG, "Got cached username for " + message.getMessageId() + ": " + username);
            addAndSaveMessage(new ChatMessage(message, username, ChatMessage.Direction.INCOMING));
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
                addAndSaveMessage(new ChatMessage(message, username, ChatMessage.Direction.INCOMING));
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
        addMessage(result);
        setSending(result, true);
        return result;
    }

    /**
     * Adds a message to the list if it is not already present.
     * @param message The message to add.
     * @return True if the message was added successfully and the message list needs to be updated.
     */
    private boolean addMessage(ChatMessage message) {
        return mMessageAdapter.addMessage(message);
    }

    /**
     * Adds a message to the list, saving it to the message history if it is new.
     * @param message The message to display.
     * @return True if the message was added successfully and the message list needs to be updated.
     */
    private boolean addAndSaveMessage(ChatMessage message) {
        if (!mMessageAdapter.addMessage(message)) {
            return false;
        }
        if (history != null) {
            history.saveMessage(message);
        }
        return true;
    }

    /**
     * Removes a message from the message list and puts its contents back into the textbox.
     * @param message The message to remove.
     * @return True if the message was redacted successfully.
     */
    private boolean removeMessage(ChatMessage message) {
        if (!mMessageAdapter.removeMessage(message)) {
            return false;
        }
        mTxtTextBody.setText(message.getBody());
        mTxtTextBody.setSelection(message.getBody().length()); // Move the cursor to the end
        Log.d(TAG, "Message " + message.getId() + " redacted");
        return true;
    }

    /**
     * Updates the "sending" state of a message.
     * @param message The message to update.
     * @param sending True if the message is currently sending.
     */
    private void setSending(ChatMessage message, boolean sending) {
        mMessageAdapter.setSending(messagesList, message, sending);
    }

    /**
     * Saves a message on the server.
     * @param message The message to save.
     */
    private void storeMessage(final ChatMessage message) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("SavedMessages");
        query.whereEqualTo("sinchId", message.getId());
        query.setLimit(1);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e != null) {
                    // TODO: Error handling
                    // The issue though is that Sinch has already succeeded to send the message,
                    // so we can't remove it and display an error to the user because that means
                    // that people might get the message twice if the user re-sends it. Retrying
                    // the operation might be the best idea here.
                    return;
                }
                if (parseObjects.size() != 0) {
                    return; // Don't store messages twice
                }
                ParseObject savedMessage = new ParseObject("SavedMessages");
                savedMessage.put("text", message.getBody());
                savedMessage.put("sender", ParseUser.getCurrentUser());
                savedMessage.put("sinchId", message.getId());
                savedMessage.put("request", ParseUser.getCurrentUser().get("currentRequest"));
                savedMessage.saveInBackground();
                Log.d(TAG, "Message " + message.getId() + " saved to database");
            }
        });
    }

    /**
     * Loads the message history in a background thread.
     */
    private void loadMessageHistoryInBackground() {
        loadTask = new LoadMessageHistoryTask();
        loadTask.execute();
    }

    /**
     * Loads the message history cached on disk.
     * @return The messages that were loaded.
     */
    private Collection<ChatMessage> loadCachedMessageHistory() {
        // Load the latest group ID from shared preferences, and if it doesn't match, then throw the local history out
        SharedPreferences prefs = view.getContext().getSharedPreferences(PREFS_NAME, 0);
        String groupId = prefs.getString(LAST_GROUP_PREF, "");
        ParseObject request = (ParseObject)ParseUser.getCurrentUser().get("currentRequest");
        if (!groupId.equals(request.getObjectId())) {
            // User changed groups, so clear the message history
            Log.d(TAG, "Clearing cached message history because the user changed groups");
            ChatMessageHistory.clear(view.getContext(), ParseUser.getCurrentUser().getObjectId());
        }

        Log.d(TAG, "Loading cached message history...");
        history = ChatMessageHistory.load(view.getContext(), ParseUser.getCurrentUser().getObjectId());
        Log.d(TAG, "Loaded " + history.getMessages().size() + " cached messages");

        // Save the current group ID associated with the message history
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(LAST_GROUP_PREF, request.getObjectId());
        editor.commit();

        return history.getMessages();
    }

    /**
     * Downloads the message history from the server and displays it.
     * @return The messages that were downloaded if successful, or null otherwise.
     */
    private Collection<ChatMessage> downloadMessageHistory() {
        Log.d(TAG, "Downloading message history...");

        // Query for all saved messages from the current request
        ParseQuery<ParseObject> query = ParseQuery.getQuery("SavedMessages");
        query.include("sender");
        ParseObject request = (ParseObject)ParseUser.getCurrentUser().get("currentRequest");
        query.whereEqualTo("request", request);
        List<ParseObject> parseObjects;
        try {
             parseObjects = query.find();
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }

        // Add each message
        ArrayList<ChatMessage> result = new ArrayList<>();
        for (final ParseObject savedMessage : parseObjects) {
            ParseUser sender = savedMessage.getParseUser("sender");
            String username = sender.getString("name");
            String text = savedMessage.getString("text");
            Date date = savedMessage.getCreatedAt();
            String sinchId = savedMessage.getString("sinchId");
            ChatMessage.Direction direction = (sender.getObjectId().equals(ParseUser.getCurrentUser().getObjectId()))
                    ? ChatMessage.Direction.OUTGOING
                    : ChatMessage.Direction.INCOMING;
            ChatMessage message = new ChatMessage(sinchId, username, text, direction, date);
            result.add(message);
            if (!mMessageAdapter.hasMessage(message)) {
                history.saveMessage(message);
            }
        }
        Log.d(TAG, "Retrieved " + parseObjects.size() + " messages");
        return result;
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

	private void displayDeletedGroupAlert() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(getString(R.string.request_deleted_info))
				.setTitle(getString(R.string.request_deleted));

		builder.setCancelable(false);
		builder.setNeutralButton(getString(R.string.neutral_option), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//Do Nothing
			}
		});
		builder.create().show();
	}

	private boolean CheckInternet()
	{
		ConnectivityManager connec = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		android.net.NetworkInfo wifi = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		android.net.NetworkInfo mobile = connec.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

		return wifi.isConnected() || mobile.isConnected();
	}

    /**
     * Task for loading the message history in the background.
     */
    private class LoadMessageHistoryTask extends AsyncTask<Void, Collection<ChatMessage>, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            publishProgress(loadCachedMessageHistory());
            Collection<ChatMessage> downloaded = downloadMessageHistory();
            if (downloaded != null) {
                publishProgress(downloaded);
                return true;
            }
            return false;
        }

        @Override
        protected void onPreExecute() {
            showLoadingIndicator(true);
        }

        @Override
        protected void onProgressUpdate(Collection<ChatMessage>... values) {
            /* Add all of the values to the message adapter */
            if (values[0] == null) {
                return;
            }
            for (ChatMessage message : values[0]) {
                addMessage(message);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            showLoadingIndicator(false);
            if (!result) {
                displayNetworkError();
            }
        }
    }
}
