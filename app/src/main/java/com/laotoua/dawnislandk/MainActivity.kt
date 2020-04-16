package com.laotoua.dawnislandk

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.laotoua.dawnislandk.databinding.ActivityMainBinding
import com.laotoua.dawnislandk.entities.Forum
import com.laotoua.dawnislandk.util.AppState
import com.laotoua.dawnislandk.util.QuickNodeAdapter
import com.laotoua.dawnislandk.viewmodels.CommunityViewModel
import com.laotoua.dawnislandk.viewmodels.SharedViewModel
import kotlinx.coroutines.launch
import timber.log.Timber


class MainActivity : AppCompatActivity(), QuickNodeAdapter.ForumClickListener {

    private lateinit var binding: ActivityMainBinding
    private val communityVM: CommunityViewModel by viewModels()
    private val sharedVM: SharedViewModel by viewModels()

    private val mAdapter = QuickNodeAdapter(this)

    init {
        // load Resources
        lifecycleScope.launch { loadResources() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initStatusBar()

        // TODO: orientation cause activity recreate, currently will force communities reload
        setUpForumDrawer()
    }

    private fun initStatusBar() {
        /**
         * 新的状态栏透明方案
         */

        window.clearFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        )
        //设置布局能够延伸到状态栏(StatusBar)和导航栏(NavigationBar)里面
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        //设置状态栏(StatusBar)颜色透明
        window.statusBarColor = Color.TRANSPARENT
    }

    // left forum drawer
    private fun setUpForumDrawer() {

        // TODO added repository
        communityVM.loadFromDB()
        communityVM.getCommunities()

        binding.forumContainer.layoutManager = LinearLayoutManager(this)
        binding.forumContainer.adapter = mAdapter

        communityVM.communityList.observe(this, Observer {
            mAdapter.setData(it)
            Timber.i("Loaded ${it.size} communities to Adapter")
            // TODO: set default forum
            sharedVM.setForum(it[0].forums[0])
            sharedVM.setForumNameMapping(communityVM.getForumNameMapping())
        })

        communityVM.loadFail.observe(this, Observer {
            if (it == true) {
                Toast.makeText(this, "无法读取板块列表...", Toast.LENGTH_LONG).show()
            }
        })
    }

    // Forum Click
    override fun onForumClick(forum: Forum) {
        Timber.i("Clicked on Forum ${forum.name}")
        sharedVM.setForum(forum)
        sharedVM.setFragment("ThreadFragment")
        binding.drawerLayout.closeDrawers()
    }


    // initialize Global resources
    private suspend fun loadResources() {
        AppState.loadCookies()
    }
}
