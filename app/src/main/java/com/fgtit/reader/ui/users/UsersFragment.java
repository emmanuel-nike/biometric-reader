package com.fgtit.reader.ui.users;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
//import android.arch.lifecycle.ViewModelProvider;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fgtit.reader.R;
import com.fgtit.reader.databinding.FragmentUsersBinding;
import com.fgtit.reader.models.User;
import com.fgtit.reader.ui.home.HomeViewModel;

import java.util.ArrayList;

public class UsersFragment extends Fragment {

    private FragmentUsersBinding binding;
    private UserViewModel viewModel;
    private RecyclerView recyclerView;
    private SearchView searchView;
    private TextView textEmptyView;

    public UsersFragment() {
        super(R.layout.fragment_users);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUsersBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        recyclerView = binding.usersListView;
        searchView = binding.searchView;
        textEmptyView = binding.textEmpty;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        recyclerView.setHasFixedSize(true);

        viewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { viewModel.searchUser(query); return false; }

            @Override
            public boolean onQueryTextChange(String newText) { viewModel.searchUser(newText); return false; }
        });

        final UserListAdapter recyclerViewAdapter = new UserListAdapter(new UserListAdapter.MyAdapterListener() {
            @Override
            public void iconImageViewOnClick(View v, int position) {
                Log.d("UsersFragment", "iconImageViewOnClick: " + position);
                AlertDialog.Builder alert = new AlertDialog.Builder(requireActivity(), R.style.Theme_AppCompat_Light_Dialog);

                alert.setTitle("Delete entry");
                alert.setMessage("Are you sure you want to delete users with this username?");
                alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        if(which == DialogInterface.BUTTON_POSITIVE){
                            Log.d("UsersFragment", "onClick: " + position);
                            viewModel.deleteUser(position);
                        }
                    }
                });
                alert.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // close dialog
                        dialog.cancel();
                    }
                });
                alert.show();
            }
        });
        recyclerView.setAdapter(recyclerViewAdapter);

        //binding.usersListView.setClickable(false);


        viewModel.getUserMutableLiveData().observe(requireActivity(), new Observer<ArrayList<User>>() {
            @Override
            public void onChanged(ArrayList<User> users) {
                if(users.size() == 0) {
                    textEmptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    textEmptyView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
                recyclerViewAdapter.updateUserList(users);
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}