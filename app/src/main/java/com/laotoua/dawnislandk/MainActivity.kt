package com.laotoua.dawnislandk

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.laotoua.dawnislandk.databinding.ActivityMainBinding
import com.laotoua.dawnislandk.util.DawnDatabase
import com.laotoua.dawnislandk.util.Forum
import com.laotoua.dawnislandk.util.QuickAdapter
import com.laotoua.dawnislandk.util.ReadableTime
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

        initToolbar()
        initResources()
        setUpForumDrawer()
    }


    private fun initToolbar() {
        /**
         * 标题栏组件初始化
         */
//        set

        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(
                GravityCompat.START
            )
        }

        binding.toolbar.setOnClickListener {

            // TODO refresh click
            Timber.d("Handle toolbar click")
        }
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)

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

        binding.toolbar.setNavigationOnClickListener {
            when (sharedVM.currentFragment.value) {
                "ThreadFragment" -> binding.drawerLayout.openDrawer(GravityCompat.START)
                "ReplyFragment" -> supportFragmentManager.popBackStack()
                "ImageViewerFragment" -> supportFragmentManager.popBackStack()
                else -> Timber.e("Unhandled navigation action")
            }

        }

        sharedVM.currentFragment.observe(this, Observer {
            updateToolBar(it)
        })

        // TODO setting it always collapsing as it contains many errors
//        binding.dawnAppbar.setExpanded(false)
    }

    private fun initResources() {
        /**
         * 初始化
         */
        val db = Room.databaseBuilder(
            applicationContext,
            DawnDatabase::class.java, "dawnDB"
        )
            .fallbackToDestructiveMigration()
            .build()
        sharedVM.setDb(db)
        forumVM.setDb(db.forumDao())

        // Time
        ReadableTime.initialize(this)

        forumVM.loadFromDB()
        forumVM.getForums()
    }

    // left forum drawer
    private fun setUpForumDrawer() {
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
            sharedVM.setForumNameMapping(forumVM.getForumNameMapping())
        })

        forumVM.loadFail.observe(this, Observer {
            if (it == true) {
                Timber.i("Failed to load data...")
                mAdapter.loadMoreModule.loadMoreFail()
            }
        })
    }


    private fun updateToolBar(currentFrag: String) {
        when (currentFrag) {
            "ImageViewerFragment" -> {
                binding.collapsingToolbar.title = "大图模式"
                binding.collapsingToolbar.subtitle = ""
//                binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)

                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
//                supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
            }
            "ReplyFragment" -> {
                val forumName =
                    sharedVM.getForumDisplayName(sharedVM.selectedThreadList.value!!.fid ?: "")
                binding.collapsingToolbar.title = "A岛  • $forumName"
                binding.collapsingToolbar.subtitle =
                    ">>No. " + sharedVM.selectedThreadList.value?.id
//                binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
//                supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
            }
            "ThreadFragment" -> {

                binding.collapsingToolbar.title = sharedVM.selectedForum.value?.name ?: "时间线"
                binding.collapsingToolbar.subtitle = "adnmb.com"

//                binding.toolbar.setNavigationIcon(R.drawable.ic_menu)

//                supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }

            else -> {
//                    binding.toolbar.visibility = View.VISIBLE
            }
        }

    }
}
