/* 
 * Copyright 2014 OpenMarket Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.matrix.matrixandroidsdk.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.data.MyUser;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.callback.SimpleApiCallback;
import org.matrix.androidsdk.rest.model.MatrixError;
import org.matrix.androidsdk.rest.model.PowerLevels;
import org.matrix.androidsdk.rest.model.RoomMember;
import org.matrix.matrixandroidsdk.Matrix;
import org.matrix.matrixandroidsdk.R;
import org.matrix.matrixandroidsdk.adapters.AdapterUtils;

import java.util.ArrayList;
import java.util.Collection;

public class ContactDetailsActivity extends MXCActionBarActivity {

    private static final String LOG_TAG = "ContactDetailsActivity";

    public static final String EXTRA_ROOM_ID = "org.matrix.matrixandroidsdk.ContactDetailsActivity.EXTRA_ROOM_ID";
    public static final String EXTRA_USER_ID = "org.matrix.matrixandroidsdk.ContactDetailsActivity.EXTRA_USER_ID";

    // info
    private Room mRoom;
    private String mUserId;
    private RoomMember mMember;
    private MXSession mSession;

    // Views
    private ImageView mThumbnailImageView;
    private TextView mMatrixIdTextView;
    private ArrayList<Button>mButtonsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_details);

        Intent intent = getIntent();
        if (!intent.hasExtra(EXTRA_ROOM_ID)) {
            Log.e(LOG_TAG, "No room ID extra.");
            finish();
            return;
        }
        String roomId = intent.getStringExtra(EXTRA_ROOM_ID);

        if (!intent.hasExtra(EXTRA_USER_ID)) {
            Log.e(LOG_TAG, "No user ID extra.");
            finish();
            return;
        }

        mUserId = intent.getStringExtra(EXTRA_USER_ID);

        mSession = Matrix.getInstance(getApplicationContext()).getDefaultSession();
        mRoom = mSession.getDataHandler().getRoom(roomId);

        if (null == mRoom) {
            Log.e(LOG_TAG, "The room is not found");
            finish();
            return;
        }

        // find out the room member
        Collection<RoomMember> members = mRoom.getMembers();
        for(RoomMember member : members) {
            if (member.getUserId().equals(mUserId)) {
                mMember = member;
                break;
            }
        }

        // sanity checks
        if (null == mMember) {
            Log.e(LOG_TAG, "The user " + mUserId + " is not in the room " + roomId);
            finish();
            return;
        }

        mButtonsList = new ArrayList<Button>();
        mButtonsList.add((Button)findViewById(R.id.contact_button_1));
        mButtonsList.add((Button)findViewById(R.id.contact_button_2));
        mButtonsList.add((Button)findViewById(R.id.contact_button_3));
        mButtonsList.add((Button)findViewById(R.id.contact_button_4));

        // set the click listener for each button
        for(Button button : mButtonsList) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String text = (String)((Button)v).getText();

                    final View refreshingView = findViewById(R.id.profile_mask);
                    final ApiCallback callback = new SimpleApiCallback<Void>() {
                        @Override
                        public void onMatrixError(MatrixError e) {
                            if (MatrixError.FORBIDDEN.equals(e.errcode)) {
                                Toast.makeText(ContactDetailsActivity.this, e.error, Toast.LENGTH_LONG).show();
                            }

                            ContactDetailsActivity.this.refresh();
                        }

                        @Override
                        public void onSuccess(Void info) {
                            ContactDetailsActivity.this.refresh();
                        }
                    };

                    // disable the buttons
                    for(Button button : mButtonsList){
                        button.setEnabled(false);
                    }

                    if (text.equals(getResources().getString(R.string.kick))) {
                        refreshingView.setVisibility(View.VISIBLE);
                        mRoom.kick(mMember.getUserId(), callback);
                    } else  if (text.equals(getResources().getString(R.string.ban))) {
                        refreshingView.setVisibility(View.VISIBLE);
                        mRoom.ban(mMember.getUserId(), callback);
                    } else  if (text.equals(getResources().getString(R.string.unban))) {
                        refreshingView.setVisibility(View.VISIBLE);
                        mRoom.unban(mMember.getUserId(), callback);
                    } else  if (text.equals(getResources().getString(R.string.invite))) {
                        refreshingView.setVisibility(View.VISIBLE);
                        mRoom.invite(mMember.getUserId(), callback);
                    } else  if (text.equals(getResources().getString(R.string.chat))) {
                        refreshingView.setVisibility(View.VISIBLE);
                        ContactDetailsActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                CommonActivityUtils.goToOneToOneRoom(mUserId, ContactDetailsActivity.this, new SimpleApiCallback<Void>() {
                                    @Override
                                    public void onMatrixError(MatrixError e) {
                                        if (MatrixError.FORBIDDEN.equals(e.errcode)) {
                                            Toast.makeText(ContactDetailsActivity.this, e.error, Toast.LENGTH_LONG).show();
                                        }
                                        ContactDetailsActivity.this.refresh();
                                    }

                                    @Override
                                    public void onNetworkError(Exception e) {
                                        Toast.makeText(ContactDetailsActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                        ContactDetailsActivity.this.refresh();
                                    }

                                    @Override
                                    public void onUnexpectedError(Exception e) {
                                        Toast.makeText(ContactDetailsActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                        ContactDetailsActivity.this.refresh();
                                    }
                                });
                            }
                        });
                    } else  if (text.equals(getResources().getString(R.string.set_power_level))) {
                        String title = getResources().getString(R.string.set_power_level);
                        String hint =  mRoom.getLiveState().getPowerLevels().getUserPowerLevel(mUserId) + "";

                        final AlertDialog alert = CommonActivityUtils.createEditTextAlert(ContactDetailsActivity.this,title,hint, new CommonActivityUtils.OnSubmitListener() {
                            @Override
                            public void onSubmit(String text) {
                                if (text.length() == 0) {
                                    return;
                                }
                                refreshingView.setVisibility(View.VISIBLE);

                                //  Todo allow to set the power user

                                ContactDetailsActivity.this.refresh();
                            }

                            @Override
                            public void onCancelled() {
                                ContactDetailsActivity.this.refresh();
                            }
                        });

                        ContactDetailsActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                alert.show();
                            }
                        });
                    }
                }
            });
        }

        // load the thumbnail
        mThumbnailImageView = (ImageView) findViewById(R.id.imageView_avatar);

        // set the title
        mMatrixIdTextView = (TextView) findViewById(R.id.textView_matrixid);

        // refresh the activity views
        refresh();
    }

    /**
     * refresh each activity views
     */
    private void refresh() {

        final View refreshingView = findViewById(R.id.profile_mask);
        refreshingView.setVisibility(View.GONE);

        mMatrixIdTextView.setText(mUserId);
        this.setTitle(mMember.displayname);
        this.refreshProfileThumbnail();

        MyUser myUser = Matrix.getInstance(this).getDefaultSession().getMyUser();
        ArrayList<String> buttonTitles = new ArrayList<String>();

        // Check user's power level before allowing an action (kick, ban, ...)
        PowerLevels powerLevels = mRoom.getLiveState().getPowerLevels();

        int userPowerLevel = powerLevels.getUserPowerLevel(mUserId);
        int myPowerLevel = powerLevels.getUserPowerLevel(myUser.userId);

        // Consider the case of the user himself
        if (mUserId.equals(myUser.userId)) {
            buttonTitles.add(getResources().getString(R.string.leave));

            if (userPowerLevel >= powerLevels.stateDefault) {
               // buttonTitles.add(getResources().getString(R.string.set_power_level));
            }
        } else {

            if ((RoomMember.MEMBERSHIP_JOIN.equals(mMember.membership)) || (RoomMember.MEMBERSHIP_INVITE.equals(mMember.membership))) {
                // Check conditions to be able to kick someone
                if ((myPowerLevel >= powerLevels.kick) && (myPowerLevel >= userPowerLevel)) {
                    buttonTitles.add(getResources().getString(R.string.kick));
                }

                // Check conditions to be able to ban someone
                if ((myPowerLevel >= powerLevels.ban) && (myPowerLevel >= userPowerLevel)) {
                    buttonTitles.add(getResources().getString(R.string.ban));
                }
            } else if (RoomMember.MEMBERSHIP_LEAVE.equals(mMember.membership)) {
                // Check conditions to be able to invite someone
                if (myPowerLevel >= powerLevels.invite) {
                    buttonTitles.add(getResources().getString(R.string.invite));
                }
                // Check conditions to be able to ban someone
                if (myPowerLevel >= powerLevels.ban) {
                    buttonTitles.add(getResources().getString(R.string.ban));
                }
            } else if (RoomMember.MEMBERSHIP_BAN.equals(mMember.membership)) {
                // Check conditions to be able to invite someone
                if (myPowerLevel >= powerLevels.ban) {
                    buttonTitles.add(getResources().getString(R.string.unban));
                }
            }

            // update power level
            if (myPowerLevel >= powerLevels.stateDefault) {
                //buttonTitles.add(getResources().getString(R.string.set_power_level));
            }

            // allow to invite an user if the room has > 2 users
            // else it will reopen this chat
            if (mRoom.getMembers().size() > 2) {
                buttonTitles.add(getResources().getString(R.string.chat));
            }
        }

        // display the available buttons
        int buttonIndex = 0;
        for(; buttonIndex < buttonTitles.size(); buttonIndex++) {
            Button button = mButtonsList.get(buttonIndex);
            button.setVisibility(View.VISIBLE);
            button.setEnabled(true);
            button.setText(buttonTitles.get(buttonIndex));
        }

        for(;buttonIndex < mButtonsList.size(); buttonIndex++) {
            Button button = mButtonsList.get(buttonIndex);
            button.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * refresh the profile thumbnail
     */
    private void refreshProfileThumbnail() {
        if (mMember.avatarUrl == null) {
            mThumbnailImageView.setImageResource(R.drawable.ic_contact_picture_holo_light);
        } else {
            int size = getResources().getDimensionPixelSize(R.dimen.profile_avatar_size);
            AdapterUtils.loadThumbnailBitmap(mThumbnailImageView, mMember.avatarUrl, size, size);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
