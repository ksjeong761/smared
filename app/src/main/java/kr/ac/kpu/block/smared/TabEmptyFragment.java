package kr.ac.kpu.block.smared;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import kr.ac.kpu.block.smared.databinding.FragmentTabEmptyBinding;

public class TabEmptyFragment extends Fragment {
    FragmentTabEmptyBinding viewBinding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentTabEmptyBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }
}