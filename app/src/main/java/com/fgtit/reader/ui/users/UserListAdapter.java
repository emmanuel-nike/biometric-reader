package com.fgtit.reader.ui.users;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fgtit.reader.R;
import com.fgtit.reader.models.User;

import java.util.ArrayList;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.RecyclerViewViewHolder> {

    ArrayList<User> userArrayList;
    MyAdapterListener deleteClickListener;

    private Context context;

    public UserListAdapter(MyAdapterListener deleteClickListener){
        this.userArrayList = new ArrayList<>();
        this.deleteClickListener = deleteClickListener;
    }

    @NonNull
    @Override
    public RecyclerViewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.users_list_item, parent, false);
        return new RecyclerViewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewViewHolder viewHolder, int position) {
        User user = userArrayList.get(position);

        viewHolder.usernameView.setText(user.getUsername());
        viewHolder.fullNameView.setText(user.getName());
        if(user.getPhotoUrl() != null){
            Glide.with(context).load(user.getPhotoUrl()).into(viewHolder.userImageView);
        }
        //viewHolder.userTypeView.setText(user.getUserType());
    }

    @Override
    public int getItemCount() {
        return userArrayList.size();
    }

    public void updateUserList(final ArrayList<User> userArrayList) {
        this.userArrayList.clear();
        this.userArrayList = userArrayList;
        notifyDataSetChanged();
    }

    class RecyclerViewViewHolder extends RecyclerView.ViewHolder {
        TextView fullNameView;
        ImageView userImageView;
        TextView usernameView;
        TextView userTypeView;
        ImageButton deleteButton;

        public RecyclerViewViewHolder(@NonNull View itemView) {
            super(itemView);
            fullNameView = itemView.findViewById(R.id.fullName);
            usernameView = itemView.findViewById(R.id.username);
            userImageView = itemView.findViewById(R.id.profileImage);
            //userTypeView = itemView.findViewById(R.id.userType);
            deleteButton = itemView.findViewById(R.id.deleteUserButton);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteClickListener.iconImageViewOnClick(v, getAdapterPosition());
                }
            });
        }
    }

    public interface MyAdapterListener {

        void iconImageViewOnClick(View v, int position);
    }
}
