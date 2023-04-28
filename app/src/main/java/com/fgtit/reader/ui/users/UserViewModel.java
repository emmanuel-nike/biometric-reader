package com.fgtit.reader.ui.users;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fgtit.reader.models.User;
import com.fgtit.reader.service.DBService;

import java.util.ArrayList;

public class UserViewModel extends AndroidViewModel {

    DBService dbService;
    MutableLiveData<ArrayList<User>> userLiveData;
    ArrayList<User> userArrayList;
    MutableLiveData<String> searchQuery;

    public UserViewModel(Application application) {
        super(application);
        userLiveData = new MutableLiveData<>();
        searchQuery = new MutableLiveData<>();
        dbService = new DBService(getApplication().getApplicationContext());
        update();
    }

    public MutableLiveData<ArrayList<User>> getUserMutableLiveData(){
        return userLiveData;
    }

    public void update(){
        populateList();
        userLiveData.setValue(userArrayList);
    }

    public void searchUser(String query){
        searchQuery.setValue(query);
        populateList();
        userLiveData.setValue(userArrayList);
    }

    private void populateList(){
        String sQuery = searchQuery.getValue();
        userArrayList = sQuery != null ? dbService.getAllUsers(sQuery) : dbService.getAllUsers();
    }

    public void deleteUser(int position){
        dbService.deleteUser(userLiveData.getValue().get(position).getUsername());
        update();
    }
}
