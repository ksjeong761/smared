package kr.ac.kpu.block.smared;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import kr.ac.kpu.block.smared.databinding.FragmentTabEmptyBinding;

// 다른 프래그먼트를 덧대어 그리기 위한 빈 프래그먼트이다.
// 비어있지 않은 프래그먼트를 기본 화면으로 사용하면 UI가 중첩되어 나타나게 된다.
public class TabEmptyFragment extends Fragment {
    private FormattedLogger logger = new FormattedLogger();
    private FragmentTabEmptyBinding viewBinding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentTabEmptyBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }
}