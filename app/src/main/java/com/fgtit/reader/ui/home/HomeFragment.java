package com.fgtit.reader.ui.home;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.fgtit.reader.MainActivity;
import com.fgtit.reader.R;
import com.fgtit.reader.databinding.FragmentHomeBinding;
import com.fgtit.reader.service.ApiService;
import com.fgtit.reader.ui.slideshow.SlideshowViewModel;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @SuppressLint("SetTextI18n")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(getLayoutInflater(), container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        final TextView responseTextView = binding.textResponse;
        final Button startFpServiceButton = binding.startFp;
        final Button startCardServiceButton = binding.startCard;
        final RadioButton clockin = binding.clockin;
        final RadioButton clockout = binding.clockout;
        final RadioButton pickup = binding.pickup;
        final ImageView imageView = binding.scanImageView;

        textView.setText("Start Service");

        homeViewModel.getAuthMode().observe(getViewLifecycleOwner(), authMode -> {
            if (authMode != null) {
                if (authMode.equals("clockin")) {
                    clockin.setChecked(true);
                    clockout.setChecked(false);
                    pickup.setChecked(false);
                } else if (authMode.equals("clockout")) {
                    clockout.setChecked(true);
                    clockin.setChecked(false);
                    pickup.setChecked(false);
                } else if (authMode.equals("pickup")) {
                    pickup.setChecked(true);
                    clockin.setChecked(false);
                    clockout.setChecked(false);
                }
            }
        });

        clockin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(clockin.isChecked()) {
                    homeViewModel.setAuthMode("clockin");
                }
            }
        });

        clockout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(clockout.isChecked()) {
                    homeViewModel.setAuthMode("clockout");
                }
            }
        });

        pickup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pickup.isChecked()) {
                    homeViewModel.setAuthMode("pickup");
                }
            }
        });

        startFpServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)getActivity()).startFpService();
            }
        });

        startCardServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)getActivity()).startCardService();
            }
        });

        homeViewModel.getIsFpServiceRunning().observe(getViewLifecycleOwner(), isRunning -> {
            if (isRunning) {
                textView.setText("Place Finger on device");
                startFpServiceButton.setEnabled(false);
                startCardServiceButton.setEnabled(true);
                Glide.with(this).asGif().load(R.raw.scanner).into(imageView);
            }else{
                imageView.setImageDrawable(null);
                startFpServiceButton.setEnabled(true);
            }
        });

        homeViewModel.getIsCardServiceRunning().observe(getViewLifecycleOwner(), isRunning -> {
            if (isRunning) {
                textView.setText("Swipe card on device");
                startCardServiceButton.setEnabled(false);
                startFpServiceButton.setEnabled(true);
                Glide.with(this).asGif().load(R.raw.scanner).into(imageView);
            } else{
                imageView.setImageDrawable(null);
                startCardServiceButton.setEnabled(true);
            }
        });

        homeViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                textView.setText(user.getName() + " (" + user.getUsername() + ')');
                String imageUrl = user.getPhotoUrl();
                if(imageUrl != null && !imageUrl.isEmpty()){
                    Glide.with(getActivity()).load(imageUrl).into(imageView);
                }
                //homeViewModel.setFpServiceRunning(false);
            } else {
                if(Boolean.TRUE.equals(homeViewModel.getIsFpServiceRunning().getValue())
                        || Boolean.TRUE.equals(homeViewModel.getIsCardServiceRunning().getValue())){
                    Glide.with(this).asGif().load(R.raw.scanner).into(imageView);
                    textView.setText("Scanning...");
                }else{
                    textView.setText("Start Service");
                    imageView.setImageDrawable(null);
                }
            }
        });

        homeViewModel.getServerResponse().observe(getViewLifecycleOwner(), response -> {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            if(!preferences.getBoolean("show_response", true)){
                responseTextView.setText("");
                return;
            }

            if (response != null) {
                responseTextView.setText("Last Server Response: \n" + response);
            } else {
                textView.setText("");
            }
        });

        homeViewModel.getIsConnected().observe(getViewLifecycleOwner(), isConnected -> {
            startCardServiceButton.setEnabled(isConnected);
            startFpServiceButton.setEnabled(isConnected);
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}