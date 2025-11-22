package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NoteAdapter

    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupTablelayout()
        setupRecyclerView()
        setupBottomNavigation()
        setupSwipeRefresh()

    }



    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // 处理首页点击事件
                    requestData()
                    true
                }
                R.id.nav_hot -> {
                    // 处理热点点击事件
                    true
                }
                R.id.nav_profile -> {
                    // 处理我的点击事件
                    true
                }
                else -> false
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            requestData()

            swipeRefreshLayout.postDelayed({
                swipeRefreshLayout.isRefreshing = false
            }, 800)
        }
    }

    private fun setupTablelayout() {
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val tabs = listOf("直播", "经验", "南京", "热点")
        for (title in tabs) {
            tabLayout.addTab(tabLayout.newTab().setText(title))
        }
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        recyclerView.layoutManager = layoutManager

        adapter = NoteAdapter(mutableListOf())
        recyclerView.adapter = adapter
    }

    private fun requestData() {
        val myBaseUrl = "https://m1.apifoxmock.com/m1/7447441-7181548-default/"
        val retrofit = Retrofit.Builder()
            .baseUrl(myBaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        apiService.getFeed().enqueue(object : Callback<FeedResponse>{
            override fun onResponse(call: Call<FeedResponse>, response: Response<FeedResponse>) {
                if (response.isSuccessful) {

                    val noteList = response.body()?.data?.list ?: return
                    adapter.updateData(noteList)

                } else {
                    Log.e("API", "请求失败: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<FeedResponse?>, t: Throwable) {
                Log.e("API", "请求失败: ${t.message}")
                t.printStackTrace()
            }

        })
    }
}