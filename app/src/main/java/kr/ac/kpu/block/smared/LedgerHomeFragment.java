package kr.ac.kpu.block.smared;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import kr.ac.kpu.block.smared.databinding.FragmentTabHomeBinding;

public class TabHomeFragment extends Fragment {
    private FormattedLogger logger = new FormattedLogger();
    private FragmentTabHomeBinding viewBinding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentTabHomeBinding.inflate(inflater, container, false);

        // 기본으로 표시될 화면은 가계부 기록 화면이다.
        switchFragment(new LedgerRegFragment());

        // 상단 NavigationView 조작으로 다른 Fragment 화면을 보여준다.
        viewBinding.lednavi.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                // 가계부 기록 화면
                case R.id.lednavi_input:
                    switchFragment(new LedgerRegFragment());
                    break;

                // 가계부 확인 화면
                case R.id.lednavi_output:
                    switchFragment(new LedgerViewFragment());
                    break;

                // 가계부 통계 화면
                case R.id.lednavi_statistic:
                    switchFragment(new LedgerStatFragment());
                    break;

                default:
                    return false;
            }

            return true;
        });

        return viewBinding.getRoot();
    }

    private void switchFragment(Fragment fragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.ledger, fragment);
        transaction.commit();
    }
}