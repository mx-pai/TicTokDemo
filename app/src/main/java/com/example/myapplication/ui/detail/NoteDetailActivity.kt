package com.example.myapplication.ui.detail

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
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
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.data.model.Comment
import com.example.myapplication.ui.main.MainViewModel

class NoteDetailActivity : AppCompatActivity(){
    private lateinit var binding: ActivityNoteDetailBinding
    private lateinit var imageAdapter: ImageGalleryAdapter
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var currentNote: Note
    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }
    private val commentRepository = CommentRepository()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("NoteDetailActivity", "onCreate")

//        WindowCompat.setDecorFitsSystemWindows(window, false)
//        window.statusBarColor = android.graphics.Color.TRANSPARENT

        binding = ActivityNoteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentNote = intent.getParcelableExtra<Note>("NOTE") ?: run {
            Toast.makeText(this, "数据错误", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val note = intent.getParcelableExtra<Note>("NOTE")

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
        commentAdapter = CommentAdapter(
            onLikeClick = { comment ->
                viewModel.targetCommentLike(comment)
            }
        )

        binding.rvComments.apply {
            layoutManager = LinearLayoutManager(this@NoteDetailActivity)
            adapter = commentAdapter
            isNestedScrollingEnabled = false
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupViewPager(images: List<String>) {
        binding.viewPagerImages.adapter = imageAdapter

        binding.viewPagerImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                "${position + 1}/${images.size}".also { binding.tvImageIndicator.text = it }
            }
        })
        imageAdapter.submitList(images)
        binding.tvImageIndicator.text = "1/${images.size}"
    }

    @SuppressLint("SetTextI18n")
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
            finishWithResult()
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
        currentNote = currentNote.copy(
            isLiked = !currentNote.isLiked,
            likes = if (currentNote.isLiked) currentNote.likes - 1 else currentNote.likes + 1
        )

        binding.ivLike.setImageResource(
            if (currentNote.isLiked) R.drawable.heart_filled else R.drawable.heart
        )

        setResult(Activity.RESULT_OK, Intent().putExtra("NOTE", currentNote))

    }

    @SuppressLint("SetTextI18n")
    private fun loadComments() {
        //val noteId = intent.getParcelableExtra<Note>("NOTE")?.id ?: return 后续完善
        val noteId = currentNote.id

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

    override fun onBackPressed() {
        finishWithResult()
    }

    private fun finishWithResult() {
        setResult(Activity.RESULT_OK, Intent().putExtra("NOTE", currentNote))
        finish()
    }

}
