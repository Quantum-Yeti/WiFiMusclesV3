package me.theoria.wifimuscles.ui.fragment;

import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import me.theoria.wifimuscles.R;
import me.theoria.wifimuscles.ui.viewmodel.HomeViewModel;

public class OptionsFragment extends Fragment {

    private HomeViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_options, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        viewModel = new ViewModelProvider(requireActivity())
                .get(HomeViewModel.class);

        Button reset = view.findViewById(R.id.btnReset);
        Switch freeze = view.findViewById(R.id.switchFreeze);
        Switch debug = view.findViewById(R.id.switchDebug);

        reset.setOnClickListener(v -> viewModel.resetData());

        freeze.setOnCheckedChangeListener((b, checked) -> {
            // hook later into widgets freeze mode
        });

        debug.setOnCheckedChangeListener((b, checked) -> {
            // logging toggle later
        });
    }
}