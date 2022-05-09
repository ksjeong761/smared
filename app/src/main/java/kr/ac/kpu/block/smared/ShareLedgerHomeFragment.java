package kr.ac.kpu.block.smared;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import kr.ac.kpu.block.smared.databinding.FragmentShareBinding;

public class ShareFragment extends Fragment {
    private FormattedLogger logger = new FormattedLogger();
    private FragmentShareBinding viewBinding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentShareBinding.inflate(inflater, container, false);

        // 기본으로 표시될 화면은 공유 가계부 기록 화면이다.
        switchFragment(new ShareLedgerRegFragment());

        // 상단 NavigationView 조작으로 다른 Fragment 화면을 보여준다.
        viewBinding.sharenavi.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                // 공유 가계부 기록 화면
                case R.id.lednavi_input:
                    switchFragment(new ShareLedgerRegFragment());
                    break;

                // 공유 가계부 확인 화면
                case R.id.lednavi_output:
                    switchFragment(new ShareLedgerViewFragment());
                    break;

                // 공유 가계부 통계 화면
                case R.id.lednavi_statistic:
                    switchFragment(new ShareLedgerStatFragment());
                    break;
                    
                default:
                    return false;
            }

            return true;
        });

        return viewBinding.getRoot();
    }

    public void switchFragment(Fragment fragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.shareledger, fragment);
        transaction.commit();
    }
}