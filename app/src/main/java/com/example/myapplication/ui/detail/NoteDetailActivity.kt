package com.example.myapplication.ui.detail

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.data.model.Note
import com.example.myapplication.data.repository.CommentRepository
import com.example.myapplication.databinding.ActivityNoteDetailBinding
import com.example.myapplication.ui.detail.adapter.CommentAdapter
import com.example.myapplication.ui.detail.adapter.ImageGalleryAdapter
import com.example.myapplication.ui.home.adapter.loadCircular
import kotlinx.coroutines.launch
import android.view.View
import com.example.myapplication.R
import kotlin.collections.emptyList

class NoteDetailActivity : AppCompatActivity(){
    private lateinit var binding: ActivityNoteDetailBinding
    private lateinit var imageAdapter: ImageGalleryAdapter
    private lateinit var commentAdapter: CommentAdapter
    private val commentRepository = CommentRepository()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("NoteDetailActivity", "onCreate")

//        WindowCompat.setDecorFitsSystemWindows(window, false)
//        window.statusBarColor = android.graphics.Color.TRANSPARENT

        binding = ActivityNoteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val note = intent.getParcelableExtra<Note>("NOTE_DATA")

        if (note == null) {
            Toast.makeText(this, "数据错误", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        initAdapters()
        val galleryImages = note.images?.takeIf { it.isNotEmpty() } ?: listOf(note.cover)
        setupViewPager(galleryImages)
        setupUI(note)
        setupListeners()
        loadComments()
    }

    private fun initAdapters() {
        imageAdapter = ImageGalleryAdapter()
        commentAdapter = CommentAdapter()

        binding.rvComments.apply {
            layoutManager = LinearLayoutManager(this@NoteDetailActivity)
            adapter = commentAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupViewPager(images: List<String>) {
        binding.viewPagerImages.adapter = imageAdapter

        binding.viewPagerImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.tvImageIndicator.text = "${position + 1}/${images.size}"
            }
        })
        imageAdapter.submitList(images)
        binding.tvImageIndicator.text = "1/${images.size}"
    }

    private fun setupUI(note: Note) {
        binding.tvUsername.text = note.userName
        binding.tvContent.text = note.title
        binding.ivAvatar.loadCircular(note.avatar)

        binding.tvTimeAndTag.text = "刚刚 · #${note.title.take(4)}"

        binding.ivLike.setImageResource(
            if (note.isLiked) R.drawable.heart_filled else R.drawable.heart
        )
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            finish()
        }
        binding.ivLike.setOnClickListener {
            toggleLike()
        }
        binding.ivShare.setOnClickListener {
            //shareNote()
            Toast.makeText(this, "分享", Toast.LENGTH_SHORT).show()
        }
        binding.ivCollect.setOnClickListener {
            //collectNote()
            Toast.makeText(this, "收藏", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleLike() {
        val note = intent.getParcelableExtra<Note>("NOTE_DATA") ?: return

        note.isLiked = !note.isLiked
        binding.ivLike.setImageResource(
            if (note.isLiked) R.drawable.heart_filled else R.drawable.heart
        )

    }

    private fun loadComments() {
        val noteId = intent.getParcelableExtra<Note>("NOTE_DATA")?.id ?: return

        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            val result = commentRepository.getComments(noteId = noteId)

            binding.progressBar.visibility = View.GONE
            result.onSuccess { commentData ->
                val comments = commentData.list
                binding.tvCommentCount.text = "共 ${comments.size} 条评论"
                commentAdapter.submitList(comments)
            }.onFailure { error ->
                Toast.makeText(this@NoteDetailActivity,
                    "加载评论失败: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}