package com.laotoua.dawnislandk

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.laotoua.dawnislandk.databinding.ActivityMainBinding
import com.laotoua.dawnislandk.util.DawnDatabase
import com.laotoua.dawnislandk.util.Forum
import com.laotoua.dawnislandk.util.QuickAdapter
import com.laotoua.dawnislandk.util.ReadableTime
import com.laotoua.dawnislandk.viewmodels.ForumViewModel
import com.laotoua.dawnislandk.viewmodels.SharedViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var forumVM: ForumViewModel
    private val sharedVM: SharedViewModel by viewModels()
    private val TAG = "MainAct"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "sharedVM instance: $sharedVM")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        forumVM = ViewModelProvider(this).get(ForumViewModel::class.java)

        supportFragmentManager.beginTransaction().add(R.id.fragmentContainer, ThreadFragment())
            .commit()



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
        val mAdapter = QuickAdapter(R.layout.forum_list_item)
        binding.forumContainer.layoutManager = LinearLayoutManager(this)
        binding.forumContainer.adapter = mAdapter

        mAdapter.loadMoreModule!!.isEnableLoadMore = false
        // item click
        mAdapter.setOnItemClickListener { adapter, _, position ->
            sharedVM.setForum(adapter.getItem(position) as Forum)
            binding.drawerLayout.closeDrawers()
        }

        forumVM.forumList.observe(this, Observer {
            mAdapter.replaceData(it)
            Log.i(TAG, "Loaded ${mAdapter.data.size} forums")
            sharedVM.setForumMapping(forumVM.getForumNameMapping())
        })

        forumVM.loadFail.observe(this, Observer {
            if (it == true) {
                mAdapter.loadMoreModule!!.loadMoreFail()
            }
        })
        // TODO refresh click
    }
}
