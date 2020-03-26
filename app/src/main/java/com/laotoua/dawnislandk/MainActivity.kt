package com.laotoua.dawnislandk

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.laotoua.dawnislandk.databinding.ActivityMainBinding
import com.laotoua.dawnislandk.util.Forum
import com.laotoua.dawnislandk.util.QuickAdapter
import com.laotoua.dawnislandk.viewmodels.ForumViewModel
import com.laotoua.dawnislandk.viewmodels.SharedViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var forumVM: ForumViewModel
    private lateinit var sharedVM: SharedViewModel
    private val TAG = "MainAct"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedVM = ViewModelProvider(this).get(SharedViewModel::class.java)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        forumVM = ViewModelProvider(this).get(ForumViewModel::class.java)
        setUpForumDrawer()

        supportFragmentManager.beginTransaction().add(R.id.fragmentContainer, ThreadFragment())
            .commit()
    }


    private fun setUpForumDrawer() {
        val mAdapter = QuickAdapter(R.layout.forum_list_item)
        binding.forumContainer.layoutManager = LinearLayoutManager(this)
        binding.forumContainer.adapter = mAdapter

        mAdapter.loadMoreModule!!.isEnableLoadMore = false
        // item click
        mAdapter.setOnItemClickListener { adapter, _, position ->
            sharedVM.setForum(adapter.getItem(position) as Forum)
        }

        forumVM.forumList.observe(this, Observer {
            mAdapter.replaceData(it)
//            mAdapter.loadMoreModule!!.loadMoreComplete()
            Log.i(TAG, "Forum Adapter now have ${mAdapter.data.size} forums")

        })

        // TODO refresh
    }
}
