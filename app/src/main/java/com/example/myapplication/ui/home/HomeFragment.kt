package com.example.myapplication.ui.home

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.myapplication.data.model.Note
import com.example.myapplication.ui.home.adapter.NoteAdapter
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.ui.main.MainViewModel
import com.google.android.material.tabs.TabLayout


@Suppress("DEPRECATION")
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: NoteAdapter
    private lateinit var viewModel: MainViewModel

    private var isSingleColumn = false
    private var lastHomeTabClickTime = 0L
    private val doubleTabTimeOut = 300L

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

        isSingleColumn = requireContext().getSharedPreferences("app_prefs", 0)
            .getBoolean("single_column_mode", false)

        setupSwipeRefresh()
        setupRecyclerView()
        setupTableLayout(view)
        setupLayoutToggle()
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

    private fun setupLayoutToggle() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let{
                    handleTabClick(it)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) { }
            override fun onTabReselected(tab: TabLayout.Tab?) {
                tab?.let{ handleTabClick(it) }
            }
        })
    }

    private fun handleTabClick(tab: TabLayout.Tab) {
        val position = tab.position
        val now = System.currentTimeMillis()

        if (position == 0 ) {
            if (now - lastHomeTabClickTime < doubleTabTimeOut) {
                setupLayoutManager()
                lastHomeTabClickTime = 0L
            } else {
                lastHomeTabClickTime = now
            }
        } else if (position == 1) {
            viewModel.loadFirstPage() //暂时显示第一页，其他不显示
        }
    }

    private fun setupLayoutManager() {
        isSingleColumn = !isSingleColumn
        val newLayoutManager = if (isSingleColumn) {
            LinearLayoutManager(requireContext())
        } else {
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL).apply {
                gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
            }
        }

        binding.recyclerView.layoutManager = newLayoutManager
        viewModel.notes.value?.let { adapter.submitList(it) }

        Toast.makeText(
            requireContext(),
            "切换到${if (isSingleColumn) "单列" else "双列"}布局",
            Toast.LENGTH_SHORT).show()
    }

    private fun setupTableLayout(view: View) {
        val tabs = listOf("经验", "直播", "南京", "热点")
        for (title in tabs) {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(title))
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = if (isSingleColumn) {
            LinearLayoutManager(requireContext())
        } else {
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL).apply {
                gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
            }
        }
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.itemAnimator = null
        adapter = NoteAdapter(
            onLikeClick = {note ->
                viewModel.toggleLike(note)
            },
            onNoteClick = {note ->
                val latestNote = viewModel.notes.value?.firstOrNull { it.id == note.id } ?: note
                val intent = android.content.Intent(activity,
                    com.example.myapplication.ui.detail.NoteDetailActivity::class.java).apply {
                    putExtra("NOTE", latestNote)
                }
                noteDetailLauncher.launch(intent)
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

    private val noteDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getParcelableExtra<Note>("NOTE")?.let { updatedNote ->
                viewModel.updateNote(updatedNote)
            }
        }
    }

}
