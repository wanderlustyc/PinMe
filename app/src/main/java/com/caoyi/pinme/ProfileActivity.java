package com.caoyi.pinme;

import android.app.ProgressDialog;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private DatabaseReference mUserReference;
    private DatabaseReference mFriendReqReference;
    private DatabaseReference mFriendsReference;

    private FirebaseUser mCurrentUser;

    private TextView mName, mStatus, mTotalFriends;
    private ImageView mImage;
    private Button mSendRequestButton, mDeclineRequestButton;

    ProgressDialog mProgress;

    private String mReqStatus = "not_friends";
    private final int NOT_FRIENDS = 0;
    private final int REQUEST_SENT = 1;
    private final int REQUEST_RECEIVED = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        final String uid = getIntent().getStringExtra("uid");

        mUserReference = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);
        mUserReference.keepSynced(true);
        mFriendReqReference = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mFriendReqReference.keepSynced(true);
        mFriendsReference = FirebaseDatabase.getInstance().getReference().child("Friends");
        mFriendReqReference.keepSynced(true);

        mName = findViewById(R.id.profile_name);
        mStatus = findViewById(R.id.profile_status);
        mImage = findViewById(R.id.profile_image);
        mTotalFriends = findViewById(R.id.profile_total_friends);
        mSendRequestButton = findViewById(R.id.profile_request_button);
        mDeclineRequestButton = findViewById(R.id.profile_decline_button);
        mDeclineRequestButton.setVisibility(View.INVISIBLE);

        mProgress = new ProgressDialog(this);
        mProgress.setTitle("Loading user profile...");
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

        mFriendReqReference.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.hasChild(uid)) {

                    String request_status = dataSnapshot.child(uid).child("request_status").getValue().toString();

                    if (request_status.equals("request_sent")) {

                        mReqStatus = "request_sent";
                        mSendRequestButton.setText("Cancel Friend Request");
                        mSendRequestButton.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                        mSendRequestButton.setBackgroundColor(getResources().getColor(android.R.color.white));

                    } else if (request_status.equals("request_received")) {

                        mReqStatus = "request_received";
                        mSendRequestButton.setText("Accept Friend Request");
                        mSendRequestButton.setTextColor(getResources().getColor(android.R.color.white));
                        mSendRequestButton.setBackgroundColor(getResources().getColor(R.color.colorAccentCold));
                        mDeclineRequestButton.setVisibility(View.VISIBLE);

                    }

                } else {

                    mFriendsReference.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            if (dataSnapshot.hasChild(uid)) {

                                mReqStatus = "friends";
                                mSendRequestButton.setText("Unfriend");
                                mSendRequestButton.setTextColor(getResources().getColor(android.R.color.white));
                                mSendRequestButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));

                            }

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });



        mUserReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();

                mName.setText(name);
                mStatus.setText(status);
                Picasso.get().load(image).placeholder(R.drawable.default_avatar).into(mImage);

                mProgress.dismiss();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mSendRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mSendRequestButton.setEnabled(false);

                // Not Friends State
                if (mReqStatus.equals("not_friends")) {
                    mFriendReqReference.child(mCurrentUser.getUid()).child(uid).child("request_status")
                            .setValue("request_sent")
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if (task.isSuccessful()) {

                                mFriendReqReference.child(uid).child(mCurrentUser.getUid()).child("request_status")
                                        .setValue("request_received")
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                        mReqStatus = "request_sent";
                                        mSendRequestButton.setText("Cancel Friend Request");
                                        mSendRequestButton.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                                        mSendRequestButton.setBackgroundColor(getResources().getColor(android.R.color.white));
                                        Toast.makeText(ProfileActivity.this, "Successfully sending request", Toast.LENGTH_LONG).show();

                                    }
                                });

                            } else {
                                Toast.makeText(ProfileActivity.this, "Failed sending request", Toast.LENGTH_LONG).show();
                            }

                            mSendRequestButton.setEnabled(true);

                        }
                    });
                }

                // Cancel Friend Request
                if (mReqStatus.equals("request_sent")) {

                    mFriendReqReference.child(mCurrentUser.getUid()).child(uid).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            mFriendReqReference.child(uid).child(mCurrentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    mReqStatus = "not_friends";
                                    mSendRequestButton.setEnabled(true);
                                    mSendRequestButton.setText("Send Friend Request");
                                    mSendRequestButton.setTextColor(getResources().getColor(android.R.color.white));
                                    mSendRequestButton.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                    Toast.makeText(ProfileActivity.this, "Friend Request Canceled", Toast.LENGTH_SHORT).show();


                                }
                            });

                        }
                    });

                    mSendRequestButton.setEnabled(true);

                }

                // Request Received State
                if (mReqStatus.equals("request_received")) {

                    final Map<String, String> timestamp = ServerValue.TIMESTAMP;

                    // Add B to A's Friends
                    mFriendsReference.child(mCurrentUser.getUid()).child(uid).child("timestamp").setValue(timestamp).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            // Add A to B's Friends
                            mFriendsReference.child(uid).child(mCurrentUser.getUid()).child("timestamp").setValue(timestamp).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    // Remove Friend Request
                                    mFriendReqReference.child(mCurrentUser.getUid()).child(uid).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {

                                            mFriendReqReference.child(uid).child(mCurrentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {

                                                    mReqStatus = "friends";
                                                    mSendRequestButton.setText("Unfriend");
                                                    mSendRequestButton.setTextColor(getResources().getColor(android.R.color.white));
                                                    mSendRequestButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
                                                    mDeclineRequestButton.setVisibility(View.INVISIBLE);

                                                }
                                            });

                                        }
                                    });

                                }
                            });

                        }

                    });

                    mSendRequestButton.setEnabled(true);

                }


                // Unfriend
                if (mReqStatus.equals("friends")) {

                    mFriendsReference.child(mCurrentUser.getUid()).child(uid).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            mFriendsReference.child(uid).child(mCurrentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    mReqStatus = "not_friends";
                                    mSendRequestButton.setText("Send Friend Request");
                                    mSendRequestButton.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                    mSendRequestButton.setTextColor(getResources().getColor(android.R.color.white));

                                }
                            });

                        }
                    });

                    DatabaseReference chatsReference = FirebaseDatabase.getInstance().getReference().child("Chats").child(mCurrentUser.getUid());
                    chatsReference.keepSynced(true);
                    chatsReference.child(uid).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                        }
                    });


                    mSendRequestButton.setEnabled(true);

                }

            }
        });

        mDeclineRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mReqStatus.equals("request_received")) {

                    mDeclineRequestButton.setEnabled(false);

                    // Remove request
                    mFriendReqReference.child(mCurrentUser.getUid()).child(uid).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            mFriendReqReference.child(uid).child(mCurrentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    mReqStatus = "not_friends";
                                    mSendRequestButton.setText("Send Friend Request");
                                    mSendRequestButton.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                    mSendRequestButton.setTextColor(getResources().getColor(android.R.color.white));
                                    mDeclineRequestButton.setVisibility(View.INVISIBLE);
                                    Toast.makeText(ProfileActivity.this, "Request Declined", Toast.LENGTH_LONG).show();


                                }
                            });

                        }
                    });

                    mDeclineRequestButton.setEnabled(true);
                }
            }
        });

    }
}
