package com.laotoua.dawnislandk

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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

        initResources()
        setUpForumDrawer()

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
            Timber.i("Selected Forum at pos $position")
            sharedVM.setForum(adapter.getItem(position) as Forum)
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
        // TODO refresh click
    }
}
