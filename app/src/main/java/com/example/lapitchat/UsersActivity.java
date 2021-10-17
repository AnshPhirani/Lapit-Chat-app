package com.example.lapitchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private RecyclerView mUserList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        mToolbar = findViewById(R.id.users_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("All Users");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mUserList = findViewById(R.id.users_list);
        mUserList.setHasFixedSize(true);
        mUserList.setLayoutManager(new LinearLayoutManager(this));

    }

    @Override
    protected void onStart() {
        super.onStart();

        Query keyQuery = FirebaseDatabase.getInstance().getReference().child("Users").limitToFirst(20);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");

       FirebaseRecyclerOptions<Users> options= new FirebaseRecyclerOptions.Builder<Users>()
               .setLifecycleOwner(UsersActivity.this)
               .setIndexedQuery(keyQuery, databaseReference, Users.class)
               .build();

        FirebaseRecyclerAdapter<Users, UserViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Users, UserViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull UserViewHolder holder, int position, @NonNull Users model) {
                Log.d("ansh", "position " + position);
                    holder.setName(model.getName());
                    holder.setStatus(model.getStatus());
                    holder.setUserImage(model.getThumb_image(), getApplicationContext());

                    String userId = getRef(position).getKey();
                    holder.mView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent profileIntent = new Intent(UsersActivity.this, ProfileActivity.class);
                            profileIntent.putExtra("user_id", userId);
                            startActivity(profileIntent);
                        }
                    });
            }

            @NonNull
            @Override
            public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_single_layout, parent, false);
                return new UserViewHolder(view);
            }
        };

        mUserList.setAdapter(firebaseRecyclerAdapter);
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder{

        View mView;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setName(String name) {
            TextView userName = mView.findViewById(R.id.username);
            Log.d("ansh", "setName: " + name);
            userName.setText(name);
        }

        public void setStatus(String status) {
            TextView user_status = mView.findViewById(R.id.user_status);
            user_status.setText(status);
        }

        public void setUserImage(String thumb_image, Context context) {
            CircleImageView user_thumbnail = mView.findViewById(R.id.user_image);
            Picasso.get().load(thumb_image).placeholder(R.drawable.baseline_account_circle_black_20).into(user_thumbnail);
        }
    }

}