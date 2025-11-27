package com.example.myapplication.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.myapplication.data.model.Note
import com.example.myapplication.ui.home.adapter.NoteAdapter
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.ui.main.MainViewModel
import com.google.android.material.tabs.TabLayout


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: NoteAdapter
    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        setupSwipeRefresh()
        setupRecyclerView()
        setupTablelayout(view)
        observeData()
        viewModel.loadFirstPage()
    }

    private fun observeData() {
        viewModel.notes.observe(viewLifecycleOwner) { notes ->
            adapter.submitList(notes)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }
        viewModel.isLoadingMore.observe(viewLifecycleOwner) { isLoadingMore ->
            binding.progressBar.visibility = if (isLoadingMore) View.VISIBLE else View.GONE
        }
        viewModel.hasMoreData.observe(viewLifecycleOwner) { hasMoreData ->
            Log.d("HomeFragment", "hasMoreData: $hasMoreData")
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadFirstPage()
        }
    }

    private fun setupTablelayout(view: View) {
        val tabs = listOf("经验", "直播", "南京", "热点")
        for (title in tabs) {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(title))
        }
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let{
                    when(it.position){
                        0 -> viewModel.loadFirstPage() //使用默认Note
                        1 -> viewModel.loadFirstPage()
                        2 -> viewModel.loadFirstPage()
                        3 -> viewModel.loadFirstPage()
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) { }
            override fun onTabReselected(tab: TabLayout.Tab?) { }

        })
    }


    private fun setupRecyclerView() {
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.itemAnimator = null
        adapter = NoteAdapter(
            onLikeClick = {note ->
                viewModel.toggleLike(note)
            },
            onNoteClick = {note ->
                val intent = android.content.Intent(activity,
                    com.example.myapplication.ui.detail.NoteDetailActivity::class.java)
                intent.putExtra("NOTE_DATA", note)
                startActivity(intent)
            },
        )
        binding.recyclerView.adapter = adapter

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (!recyclerView.canScrollVertically(1) && dy > 0) {
                    if (viewModel.hasMoreData.value != false &&
                        viewModel.isLoadingMore.value != true) {
                        viewModel.loadMore()
                    }
                }
            }
        })
    }

}
