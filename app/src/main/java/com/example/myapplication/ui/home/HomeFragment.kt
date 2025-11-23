package com.example.myapplication.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.data.service.ApiService
import com.example.myapplication.ui.home.adapter.NoteAdapter
import com.example.myapplication.R
import com.example.myapplication.utils.BASE_URL
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NoteAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSwipeRefresh()
        setupRecyclerView()
        setupTablelayout(view)
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout = view?.findViewById(R.id.swipeRefreshLayout)!!
        swipeRefreshLayout.setOnRefreshListener {
            loadData()
            swipeRefreshLayout.postDelayed({
                swipeRefreshLayout.isRefreshing = false
            }, 800)
        }
    }

    private fun setupTablelayout(view: View) {
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        val tabs = listOf("经验", "直播", "南京", "热点")
        for (title in tabs) {
            tabLayout?.addTab(tabLayout.newTab().setText(title))
        }
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    when (it.position) {
                        0 -> loadData("经验")
                        1 -> loadData("直播")
                        2 -> loadData("南京")
                        3 -> loadData("热点")
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) { }
            override fun onTabReselected(tab: TabLayout.Tab?) { }

        })
        loadData()
    }

    private fun loadData(category: String = "经验") {
        // 根据 category 加载不同数据
        swipeRefreshLayout.isRefreshing = true

        val myBaseUrl = BASE_URL
        val retrofit = Retrofit.Builder()
            .baseUrl(myBaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        lifecycleScope.launch {
            try {
                val response = apiService.getFeed()
                if (response.isSuccessful) {
                    val noteList = response.body()?.data?.list
                    if (noteList?.isNotEmpty() == true) {
                        adapter.updateData(noteList)
                    }
                } else {
                    Log.e("API", "请求失败: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("API", "请求失败: ${e.message}")
                e.printStackTrace()
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun setupRecyclerView() {
        view?.let { recyclerView = it.findViewById(R.id.recyclerView) }
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        recyclerView.layoutManager = layoutManager

        adapter = NoteAdapter(mutableListOf())
        recyclerView.adapter = adapter
    }
}