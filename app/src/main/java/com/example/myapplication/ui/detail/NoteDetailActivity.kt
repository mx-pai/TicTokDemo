package com.example.myapplication.ui.detail

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class NoteDetailActivity : AppCompatActivity(){
    private lateinit var binding: ActivityNoteDetailBinding
    private lateinit var imageAdapter: ImageGalleryAdapter
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var currentNote: Note

    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }
    private val commentRepository = CommentRepository
    private val localComments = mutableListOf<Comment>()
    private var serverComments: List<Comment> = emptyList()
    private val gson = Gson()
    private lateinit var sharedPreferences: SharedPreferences
    private var replyToComment: Comment? = null

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

        sharedPreferences = getSharedPreferences("comments_${currentNote.id}", MODE_PRIVATE)
        val note = intent.getParcelableExtra<Note>("NOTE")

        if (note == null) {
            Toast.makeText(this, "数据错误", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        initAdapters()
        val galleryImages = buildList {
            note.cover?.let { add(it) }
            note.images?.filter { it.isNotBlank() }?.let { addAll(it) }
        }
        setupViewPager(galleryImages)
        setupUI(note)
        setupListeners()
        loadComments()
        setupCommentInput()
    }

    private fun initAdapters() {
        imageAdapter = ImageGalleryAdapter()
        commentAdapter = CommentAdapter(
            onLikeClick = { updated ->
                val all = commentAdapter.currentList.toMutableList()
                val idx = all.indexOfFirst { it.id == updated.id }
                if (idx != -1) {
                    all[idx] = updated
                    commentAdapter.submitList(all)
                }

                val localIdx = localComments.indexOfFirst { it.id == updated.id }
                if (localIdx != -1) {
                    localComments[localIdx] = updated
                    saveCommentsToLocal()
                }
            },
            onReplyClick = { comment ->
                replyToComment = comment
                binding.etComment.hint = "回复 ${comment.userName}"
                binding.etComment.requestFocus()
                binding.layoutExpandedInput.visibility = View.VISIBLE
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

    private fun setupCommentInput() {

        binding.etComment.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.layoutExpandedInput.visibility = View.VISIBLE
                binding.etExpandedComment.setText(binding.etComment.text.toString())
                binding.etExpandedComment.setSelection(binding.etComment.text.length)
            }
        }

        binding.ivSend.setOnClickListener {
            sendComment()
        }

        // 同步两个输入框的文本
        binding.etComment.addTextChangedListener(object :
            android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int,
                                           count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before:
            Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (binding.layoutExpandedInput.visibility == View.VISIBLE)
                {
                    val newText = s?.toString() ?: ""
                    if (binding.etExpandedComment.text.toString() !=
                        newText) {
                        binding.etExpandedComment.setText(newText)

                        binding.etExpandedComment.setSelection(newText.length)
                    }
                }
            }
        })

        binding.etExpandedComment.addTextChangedListener(object :
            android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int,
                                           count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before:
            Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val newText = s?.toString() ?: ""
                if (binding.etComment.text.toString() != newText) {
                    binding.etComment.setText(newText)
                    binding.etComment.setSelection(newText.length)
                }
            }
        })

        binding.nestedScrollView.setOnTouchListener { _, _ ->
            if (binding.layoutExpandedInput.visibility == View.VISIBLE) {
                binding.layoutExpandedInput.visibility = View.GONE
                binding.etComment.clearFocus()
                replyToComment = null
                binding.etComment.hint = "说点什么..."
            }
            false
        }

    }

    private fun sendComment() {
        val content = binding.etComment.text.toString().trim()
        if (content.isEmpty()) {
            Toast.makeText(this, "请输入评论内容", Toast.LENGTH_SHORT).show()
            return
        }

        //示例
        val newComment = Comment(
            id = -System.currentTimeMillis(),
            userName = "当前用户",
            avatar = "https://api.dicebear.com/7.x/miniavs/png",
            content = content,
            timestamp = "刚刚",
            location = "北京",
            likes = 0,
            isLiked = false,
            replyToUsername = replyToComment?.userName,
            parentCommentId = replyToComment?.id,
            replies = emptyList()
        )

        localComments.add(0, newComment)
        saveCommentsToLocal()

        commentRepository.addCommentToCache(currentNote.id, newComment)

        binding.etComment.text.clear()
        binding.etComment.hint = "说点什么..."
        replyToComment = null

        binding.layoutExpandedInput.visibility = View.GONE
        binding.etComment.clearFocus()
        updateCommentCount()
        Toast.makeText(this, "评论发送成功", Toast.LENGTH_SHORT).show()
    }

    private fun updateCommentCount() {
        val allComments = buildDisplayComments()
        binding.tvCommentCount.text = "共 ${allComments.size} 条评论"
        commentAdapter.submitList(allComments)
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
        val noteId = currentNote.id

        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            val result = commentRepository.getComments(noteId)

            binding.progressBar.visibility = View.GONE
            result.onSuccess { commentData ->
                serverComments = commentData.list
                val allComments = buildDisplayComments()
                binding.tvCommentCount.text = "共 ${allComments.size} 条评论"
                commentAdapter.submitList(allComments)
                commentRepository.addLocalCommentToCache(noteId, allComments)
            }.onFailure { error ->
                Toast.makeText(this@NoteDetailActivity, "加载评论失败", Toast.LENGTH_SHORT).show()
                val allComments = buildDisplayComments()
                binding.tvCommentCount.text = "共 ${allComments.size} 条评论"
                commentAdapter.submitList(allComments)
            }
        }

    }

    private fun saveCommentsToLocal() {
        val json = gson.toJson(localComments)
        sharedPreferences.edit().putString("COMMENTS", json).apply()
    }


    private fun buildAllComments(): List<Comment> {
        val allComments = mutableListOf<Comment>()
        allComments.addAll(serverComments)
        val serverIds = serverComments.map { it.id }.toSet()
        localComments.forEach { localComment ->
            if (!serverIds.contains(localComment.id)) {
                allComments.add(0, localComment)
            }
        }
        return allComments
    }

    private fun buildDisplayComments(): List<Comment> {
        val all = buildAllComments()

        val parents = all.filter { it.parentCommentId == null }

        val childrenByParent = all
            .filter { it.parentCommentId != null }
            .groupBy { it.parentCommentId }

        val result = mutableListOf<Comment>()
        parents.forEach { parent ->
            result.add(parent)
            result.addAll(childrenByParent[parent.id] ?: emptyList())
        }
        return result
    }

    override fun onBackPressed() {
        finishWithResult()
    }

    private fun finishWithResult() {
        setResult(Activity.RESULT_OK, Intent().putExtra("NOTE", currentNote))
        finish()
    }

}
