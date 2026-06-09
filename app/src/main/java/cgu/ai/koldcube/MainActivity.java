package cgu.ai.koldcube;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import cgu.ai.koldcube.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    public static final int PAGE_TIMER = 0;
    public static final int PAGE_HISTORY = 1;
    public static final int PAGE_SETTINGS = 2;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.viewPager.setAdapter(new PagerAdapter(this));
        // Disable swipe — touch events belong to the timer
        binding.viewPager.setUserInputEnabled(false);
        binding.viewPager.setOffscreenPageLimit(1);
    }

    public void navigateTo(int page) {
        binding.viewPager.setCurrentItem(page, false);
    }

    private static class PagerAdapter extends FragmentStateAdapter {
        PagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case PAGE_HISTORY:
                    return new HistoryFragment();
                case PAGE_SETTINGS:
                    return new SettingsFragment();
                case PAGE_TIMER:
                default:
                    return new TimerFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
