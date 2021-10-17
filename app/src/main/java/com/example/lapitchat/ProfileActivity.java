package com.example.lapitchat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
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
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView mProfileName;
    private TextView mProfileStatus;
    private TextView mProfileFriendsCount;
    private ImageView mProfileImage;
    private Button mProfileSendReqBtn;
    private Button mProfileDeclineReqBtn;

    private DatabaseReference mFriendReqDatabase;
    private DatabaseReference mUserDatabase;
    private DatabaseReference mFriendDatabase;
    private DatabaseReference mNotificationDatabase;
    private FirebaseUser mCurrentUser;

    ProgressDialog mProgress;
    private String mCurrentState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        String userId = getIntent().getStringExtra("user_id");

        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(userId);
        mFriendReqDatabase = FirebaseDatabase.getInstance().getReference().child("FriendRequests");
        mFriendDatabase = FirebaseDatabase.getInstance().getReference().child("Friends");
        mNotificationDatabase = FirebaseDatabase.getInstance().getReference().child("Notifications");
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

        mProfileName = findViewById(R.id.profile_name);
        mProfileStatus = findViewById(R.id.profile_status);
        mProfileFriendsCount = findViewById(R.id.profile_friends_count);
        mProfileImage = findViewById(R.id.profile_image);
        mProfileSendReqBtn = findViewById(R.id.profile_send_request_btn);
        mProfileDeclineReqBtn = findViewById(R.id.profile_decline_request_btn);

        mProfileDeclineReqBtn.setVisibility(View.INVISIBLE);
        mProfileDeclineReqBtn.setEnabled(false);

        mCurrentState = "not_friends";

        // progress dialog setup
        mProgress = new ProgressDialog(this);
        mProgress.setTitle("Loading User Data");
        mProgress.setMessage("Please wait this make take some time");
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();

        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String display_name = snapshot.child("name").getValue().toString();
                String status = snapshot.child("status").getValue().toString();
                String image = snapshot.child("image").getValue().toString();
                mProfileName.setText(display_name);
                mProfileStatus.setText(status);
                Picasso.get().load(image).placeholder(R.drawable.ic_baseline_person).into(mProfileImage);

                //----------------FRIENDS LIST / REQUEST FEATURE --------------------

                mFriendReqDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot.hasChild(userId)) {
                            String reqType = snapshot.child(userId).child("request_type").getValue().toString();

                            if (reqType.equals("received")) {
                                mCurrentState = "request_received";
                                mProfileSendReqBtn.setText("Accept Friend Request");

                                mProfileDeclineReqBtn.setVisibility(View.VISIBLE);
                                mProfileDeclineReqBtn.setEnabled(true);
                            } else if (reqType.equals("sent")) {
                                mCurrentState = "request_sent";
                                mProfileSendReqBtn.setText("Cancel Friend Request");

                                mProfileDeclineReqBtn.setVisibility(View.INVISIBLE);
                                mProfileDeclineReqBtn.setEnabled(false);
                            }
                            mProgress.dismiss();
                        } else {
                            mFriendDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    // user is already friend
                                    if (snapshot.hasChild(userId)) {
                                        mCurrentState = "friends";
                                        mProfileSendReqBtn.setText("Remove Friend");

                                        mProfileDeclineReqBtn.setVisibility(View.INVISIBLE);
                                        mProfileDeclineReqBtn.setEnabled(false);

                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                            mProgress.dismiss();
                        }


                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        mProfileSendReqBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                mProfileSendReqBtn.setEnabled(false);

                // ---------------- NOT FRIEND STATE-----------------
                if (mCurrentState.equals("not_friends")) {
                    mFriendReqDatabase.child(mCurrentUser.getUid()).child(userId).child("request_type")
                            .setValue("sent").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                mFriendReqDatabase.child(userId).child(mCurrentUser.getUid()).child("request_type")
                                        .setValue("received").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {

                                        HashMap<String, String> notifyData = new HashMap<>();
                                        notifyData.put("from", mCurrentUser.getUid());
                                        notifyData.put("type", "request");


                                        mNotificationDatabase.child(userId).push().setValue(notifyData)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void unused) {

                                                        mCurrentState = "request_sent";
                                                        mProfileSendReqBtn.setText("Cancel Friend Request");

                                                        mProfileDeclineReqBtn.setVisibility(View.INVISIBLE);
                                                        mProfileDeclineReqBtn.setEnabled(false);
                                                    }
                                                });

                                        mProfileSendReqBtn.setEnabled(true);
//                                        Toast.makeText(ProfileActivity.this, "Request Sent Successfully.", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                            else{
                                mProfileSendReqBtn.setEnabled(true);

                                mProfileDeclineReqBtn.setVisibility(View.INVISIBLE);
                                mProfileDeclineReqBtn.setEnabled(false);
//                                Toast.makeText(ProfileActivity.this, "Failed Sending Request", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }

                // ---------------- CANCEL FRIEND REQEUST STATE-----------------
                if (mCurrentState.equals("request_sent")) {

                    mFriendReqDatabase.child(mCurrentUser.getUid()).child(userId).removeValue()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    mFriendReqDatabase.child(userId).child(mCurrentUser.getUid()).removeValue()
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    mProfileSendReqBtn.setEnabled(true);
                                                    mCurrentState = "not_friends";
                                                    mProfileSendReqBtn.setText("Send Friend Request");

                                                    mProfileDeclineReqBtn.setVisibility(View.INVISIBLE);
                                                    mProfileDeclineReqBtn.setEnabled(false);
                                                }
                                            });
                                }
                            });

                }
                // ---------------REQUEST RECEIVED STATE ---------------------
                if (mCurrentState.equals("request_received")) {
                    String curData = DateFormat.getDateTimeInstance().format(new Date());
                    mFriendDatabase.child(mCurrentUser.getUid()).child(userId).setValue(curData)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    mFriendDatabase.child(userId).child(mCurrentUser.getUid()).setValue(curData)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    // -----remove friend request data from firebase (same as above)---------

                                                    mFriendReqDatabase.child(mCurrentUser.getUid()).child(userId).removeValue()
                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                @Override
                                                                public void onSuccess(Void unused) {
                                                                    mFriendReqDatabase.child(userId).child(mCurrentUser.getUid()).removeValue()
                                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                @Override
                                                                                public void onSuccess(Void unused) {
                                                                                    mProfileSendReqBtn.setEnabled(true);
                                                                                    mCurrentState = "friends";
                                                                                    mProfileSendReqBtn.setText("Remove Friend");

                                                                                    mProfileDeclineReqBtn.setVisibility(View.INVISIBLE);
                                                                                    mProfileDeclineReqBtn.setEnabled(false);
                                                                                }
                                                                            });
                                                                }
                                                            });
                                                }
                                            });
                                }
                            });
                }

                // -------------------REMOVE FRIEND ------------------------
                if (mCurrentState.equals("friends")) {
                    mFriendDatabase.child(mCurrentUser.getUid()).child(userId).removeValue()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    mFriendDatabase.child(userId).child(mCurrentUser.getUid()).removeValue()
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    mProfileSendReqBtn.setEnabled(true);
                                                    mCurrentState = "not_friends";
                                                    mProfileSendReqBtn.setText("Send Friend Request");

                                                    mProfileDeclineReqBtn.setVisibility(View.INVISIBLE);
                                                    mProfileDeclineReqBtn.setEnabled(false);
                                                }
                                            });
                                }
                            });
                }

            }
        });

    }
}