package com.raccoongang.newedx

import android.os.Bundle
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import androidx.fragment.app.Fragment
import com.raccoongang.core.presentation.global.viewBinding
import com.raccoongang.dashboard.presentation.DashboardFragment
import com.raccoongang.discovery.presentation.DiscoveryFragment
import com.raccoongang.newedx.adapter.MainNavigationFragmentAdapter
import com.raccoongang.newedx.databinding.FragmentMainBinding
import com.raccoongang.profile.presentation.profile.ProfileFragment

class MainFragment : Fragment(R.layout.fragment_main) {

    private val binding by viewBinding(FragmentMainBinding::bind)

    private lateinit var adapter: MainNavigationFragmentAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewPager()

        binding.bottomNavView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.fragmentHome -> binding.viewPager.setCurrentItem(0, false)
                R.id.fragmentDashboard -> binding.viewPager.setCurrentItem(1, false)
                R.id.fragmentPrograms -> binding.viewPager.setCurrentItem(2, false)
                R.id.fragmentProfile -> binding.viewPager.setCurrentItem(3, false)
            }
            true
        }
    }

    private fun initViewPager() {
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPager.offscreenPageLimit = 4
        adapter = MainNavigationFragmentAdapter(this).apply {
            addFragment(DiscoveryFragment())
            addFragment(DashboardFragment())
            addFragment(InDevelopmentFragment())
            addFragment(ProfileFragment())
        }
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false
    }
}