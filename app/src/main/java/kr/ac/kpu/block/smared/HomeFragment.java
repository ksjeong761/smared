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

// 기본 값으로 가계부 기록 화면을 보여주며 가계부 확인, 통계로 전환이 가능한 화면이다.
public class HomeFragment extends Fragment {
    Fragment fragment;

    // 하단 NavigationView 조작으로 다른 Fragment 화면을 보여준다.
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                // 가계부 기록 화면
                case R.id.lednavi_input:
                    fragment = new LedgerRegFragment();
                    switchFragment(fragment);
                    return true;

                // 가계부 확인 화면
                case R.id.lednavi_output:
                    fragment = new LedgerViewFragment();
                    switchFragment(fragment);
                    return true;

                // 통계 화면
                case R.id.lednavi_statistic:
                    fragment = new LedgerStatFragment();
                    switchFragment(fragment);
                    return true;
                default:
                    return false;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Fragment를 화면에 출력하기 위해 뷰를 생성한다.
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        // [Refactoring] switchFragment 함수 사용으로 코드 압축 고려
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        LedgerRegFragment fragment = new LedgerRegFragment();
        fragmentTransaction.add(R.id.ledger, fragment);
        fragmentTransaction.commit();

        // 하단 NavigationView 조작으로 다른 Fragment 화면을 보여준다.
        BottomNavigationView navigation = v.findViewById(R.id.lednavi);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        return v;
    }

    public void switchFragment(Fragment fragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.ledger, fragment);
        transaction.commit();
    }
}