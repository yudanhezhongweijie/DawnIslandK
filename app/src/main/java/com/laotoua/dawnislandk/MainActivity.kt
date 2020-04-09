package com.laotoua.dawnislandk

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.laotoua.dawnislandk.databinding.ActivityMainBinding
import com.laotoua.dawnislandk.entities.Forum
import com.laotoua.dawnislandk.util.QuickAdapter
import com.laotoua.dawnislandk.viewmodels.ForumViewModel
import com.laotoua.dawnislandk.viewmodels.SharedViewModel
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val forumVM: ForumViewModel by viewModels()
    private val sharedVM: SharedViewModel by viewModels()

    private val mAdapter = QuickAdapter(R.layout.forum_list_item)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.i("sharedVM instance: $sharedVM")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        initStatusBar()

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
        forumVM.loadFromDB()
        forumVM.getForums()

        binding.forumContainer.layoutManager = LinearLayoutManager(this)
        binding.forumContainer.adapter = mAdapter
        mAdapter.loadMoreModule.isEnableLoadMore = false
        // item click
        mAdapter.setOnItemClickListener { adapter, _, position ->
            val target = adapter.getItem(position) as Forum
            Timber.i("Selected Forum at pos $position")
            sharedVM.setForum(target)
            sharedVM.setFragment("ThreadFragment")
            binding.drawerLayout.closeDrawers()

        }

        forumVM.forumList.observe(this, Observer {
            Timber.i("Loaded ${mAdapter.data.size} forums")
            mAdapter.setList(it)
            sharedVM.setForum(it[0])
            sharedVM.setForumNameMapping(forumVM.getForumNameMapping())
        })

        forumVM.loadFail.observe(this, Observer {
            if (it == true) {
                Timber.i("Failed to load data...")
                mAdapter.loadMoreModule.loadMoreFail()
            }
        })
    }

}
