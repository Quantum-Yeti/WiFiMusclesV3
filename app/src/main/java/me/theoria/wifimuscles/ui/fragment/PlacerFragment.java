package me.theoria.wifimuscles.ui.fragment;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import me.theoria.wifimuscles.databinding.FragmentPlacerBinding;
import me.theoria.wifimuscles.ui.viewmodel.HomeViewModel;

public class PlacerFragment extends Fragment {

    private FragmentPlacerBinding binding;
    private HomeViewModel viewModel;

    private float x = 300;
    private float y = 300;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentPlacerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        setupWalking();
        observeSignal();
    }

    // simulate walking (replace later with sensors if you want)
    private void setupWalking() {

        binding.placerView.setOnTouchListener((v, e) -> {

            x = e.getX();
            y = e.getY();

            binding.placerView.setPlayerPosition(x, y);

            return true;
        });
    }

    private void observeSignal() {

        viewModel.getRssi().observe(getViewLifecycleOwner(), rssiText -> {

            try {
                String cleaned = rssiText.replaceAll("[^0-9-]", "");
                int rssi = Integer.parseInt(cleaned);

                binding.placerView.addSample(rssi);

            } catch (Exception ignored) {}
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}