package kr.ac.kpu.block.smared;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import kr.ac.kpu.block.smared.databinding.FragmentShareBinding;

public class ShareFragment extends Fragment{
    private FragmentShareBinding viewBinding;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = item -> {
        switch (item.getItemId()) {
            case R.id.lednavi_input:
                switchFragment(new ShareLedgerRegFragment());
                break;
            case R.id.lednavi_output:
                switchFragment(new ShareLedgerViewFragment());
                break;
            case R.id.lednavi_statistic:
                switchFragment(new ShareLedgerStatFragment());
                break;
            default:
                return false;
        }

        return true;
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        switchFragment(new ShareLedgerRegFragment());

        viewBinding = FragmentShareBinding.inflate(inflater, container, false);
        viewBinding.sharenavi.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        return viewBinding.getRoot();
    }

    public void switchFragment(Fragment fragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.shareledger, fragment);
        transaction.commit();
    }
}