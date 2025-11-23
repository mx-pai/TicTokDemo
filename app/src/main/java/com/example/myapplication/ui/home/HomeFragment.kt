package com.example.myapplication.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.StaggeredGridLayoutManager
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
    ): View? {
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
        viewModel.notes.observe(viewLifecycleOwner) { notes ->
            adapter.updateData(notes)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }

        setupSwipeRefresh()
        setupRecyclerView()
        setupTablelayout(view)
        viewModel.loadNotes()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadNotes()
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
                        0 -> viewModel.loadNotes()
                        1 -> viewModel.loadNotes() //使用默认Note
                        2 -> viewModel.loadNotes()
                        3 -> viewModel.loadNotes()
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) { }
            override fun onTabReselected(tab: TabLayout.Tab?) { }

        })
        viewModel.loadNotes()
    }


    private fun setupRecyclerView() {
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        binding.recyclerView.layoutManager = layoutManager
        adapter = NoteAdapter(mutableListOf())
        binding.recyclerView.adapter = adapter
    }
}