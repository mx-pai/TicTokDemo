# 开发中的问题排查与优化记录

这次开发里，主要踩了三个比较经典但又很“阴间体验”的坑：

* 上拉加载的抖动 & 列表定位错乱
* 点赞状态在列表页和详情页之间不同步
* 评论数据丢失、重复加载、闪烁

下面就按实际排查过程，把问题、原因和改造思路都梳理一遍，也算是给之后的自己留一份“避坑指南”。

---

## 问题一：上拉加载的“抖动”和列表位置错乱

### 现象描述

在实现上拉加载更多的时候，出现了非常严重的问题：

1. **加载抖动**：手指滑到底部时，`loadMore()` 像没长记性一样被疯狂重复调用，日志刷屏，实测直接多加载了好几页数据。
2. **位置错乱**：下拉刷新后，列表本该回到顶部，但经常“离家出走”，跳到底部或者莫名停在中间。

### 问题定位

可疑点在 `HomeFragment.kt` 的滚动监听中：

```kotlin
// 问题代码（原始实现）
binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (!recyclerView.canScrollVertically(1) && dy > 0) {
            viewModel.loadMore()  // 没有防抖判断！
        }
    }
})
```

同时配合 ViewModel 中的加载逻辑：

```kotlin
// MainViewModel.kt - 原始 loadMore()
fun loadMore() {
    viewModelScope.launch {
        currentPage++
        val result = repository.getFeed()
        // ... 直接追加数据
    }
}
```

**问题本质：**

* `onScrolled()` 是高频回调，只要在底部来回晃，`loadMore()` 就会被呼叫很多次；
* 缺少“正在加载中”的状态标记，导致并发请求，数据被重复追加；
* 刷新时直接替换整个列表数据，RecyclerView 失去了原有的锚点位置，因此出现“位置乱跳”。

### 修复方案：增加滚动监听以及增加 ViewModel 状态

#### 1. 滚动监听中增加防抖和状态判断（HomeFragment.kt）

```kotlin
binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (!recyclerView.canScrollVertically(1) && dy > 0) {
            // 增加三个关键判断：有更多数据、不在加载更多、不在整体加载中
            if (viewModel.hasMoreData.value != false && 
                viewModel.isLoadingMore.value != true && 
                viewModel.isLoading.value != true) {
                viewModel.loadMore()
            }
        }
    }
})
```
三个条件：
* `hasMoreData`：没有数据就别再 loadMore；
* `isLoadingMore`：当前正在执行 loadMore 时，不允许再进；
* `isLoading`：如果有整体“第一页加载中”的状态，也要避免同时进行两种加载。

#### 2. ViewModel 内维护独立的“加载更多”状态（MainViewModel.kt）

```kotlin
fun loadMore() {
    viewModelScope.launch {
        if (_hasMoreData.value == false || _isLoadingMore.value == true) {
            return@launch
        }
        _isLoadingMore.value = true
        kotlinx.coroutines.delay(1000)
        try {
            currentPage++
            Log.d("MainViewModel", "loadMore: currentPage=$currentPage")
            val result = repository.getFeed()
            if (result.isSuccess) {
                val newApiNotes = result.getOrDefault(emptyList())
                val mergeNotes = newApiNotes.map { newNote ->
                    val existingNote = localNotes.find {it.id == newNote.id}
                    if (existingNote != null) {
                        newNote.copy(
                            isLiked = existingNote.isLiked,
                            likes = existingNote.likes
                        )
                    } else {
                        newNote
                    }
                }
                    ...
    }
}
```
以前的实现是直接addAll，刷新的时候原地列表位置，这样刷新带来的另一个问题是，当loadmore时就会出现全部加载，
并且页面没有跟手，还要重新滑动，于是改成

```kotlin
 //addAll会出现刷新问题
val distinctNotes = mergeNotes.filter { newNote ->
    localNotes.none { it.id == newNote.id }
} if (distinctNotes.isNotEmpty()) {
    localNotes.addAll(distinctNotes)
    _notes.value = localNotes.toList()
} 
_hasMoreData.value = newApiNotes.size >= pageSize
```
加了distinctNotes数据的单独处理,这样就会数据接着下面来刷新,而不是替换整个列表,这样就可以保持列表的位置,而不是刷新后跳转到顶部

### 结果

* `loadMore()` 不再被疯狂触发，底部加载逻辑变得有节制、有状态；
* 下拉刷新之后，列表位置可控，不再出现错乱。

---

## 问题二：点赞状态跨页面不同步

### 场景描述
1. 在列表页点了个赞，进入详情页一看，发现还是“未点赞”状态；
2. 在详情页又点了赞，返回列表页，列表那边又没更新。

### 设计思路：把 Note 回传

这里采用的是 Android 官方推荐的 `ActivityResultLauncher` 方案：
**详情页负责产生带最新点赞状态的 `Note` 对象，列表页通过 ViewModel 统一更新。**

#### 1. 列表页：注册并启动详情页（HomeFragment.kt）

```kotlin
// 第133-141行：注册 ActivityResultLauncher
private val noteDetailLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        result.data?.getParcelableExtra<Note>("NOTE")?.let { updatedNote ->
            viewModel.updateNote(updatedNote)  // 关键：用返回的 Note 更新列表
        }
    }
}

// 第106-111行：启动详情页
val intent = android.content.Intent(activity, NoteDetailActivity::class.java).apply {
    putExtra("NOTE", latestNote)
}
noteDetailLauncher.launch(intent)  // 使用 launcher 启动
```

列表页完全不用关心点赞细节，只负责把最新的 Note 丢给 ViewModel 更新。

#### 2. 详情页：点赞时更新 Note 并通过 result 回传（NoteDetailActivity.kt）

```kotlin
// 负责设置 result 并关闭页面
private fun finishWithResult() {
    setResult(Activity.RESULT_OK, Intent().putExtra("NOTE", currentNote))
    finish()
}

// 点赞逻辑：更新 currentNote，并立即通过 setResult 把最新对象传出去
private fun toggleLike() {
    currentNote = currentNote.copy(
        isLiked = !currentNote.isLiked,
        likes = if (currentNote.isLiked) currentNote.likes - 1 else currentNote.likes + 1
    )
    binding.ivLike.setImageResource(
        if (currentNote.isLiked) R.drawable.heart_filled else R.drawable.heart
    )
    setResult(Activity.RESULT_OK, Intent().putExtra("NOTE", currentNote))  // 立刻回传新状态
}
```

这里必须设置：

* `Note` 必须是 `Parcelable`，方便在 Intent 中传递；
* 无论是返回还是中途切回列表，列表拿到的都是“最新版本”的 Note。

#### 3. ViewModel：用 id 更新对应的 Note（MainViewModel.kt）

```kotlin
// 第123-127行：根据 id 更新单条 Note
fun updateNote(updatedNote: Note) {
    _notes.value = _notes.value?.map { note ->
        if (note.id == updatedNote.id) updatedNote else note
    }
}
```

这样列表页、详情页、其他观察者全都只认 `_notes` 这一份数据源。

### 结果

* 哪个页面点的赞不重要，最后状态都统一到 ViewModel；
* UI 不用管理状态，一切以 `Note` 对象为准；
* 没有额外网络请求，纯本地状态同步。

---

## 问题三：评论数据丢失与重复加载

### 问题分享

评论这块的问题稍微复杂一点：

1. 发送评论后，评论区域只有自己的评论；
2. 刚发的评论，看着是成功了，退出来再进一遍——评论没了；
3. 每次进入详情页都重新拉取评论，列表闪烁，非常影响阅读体验。

因为Comments都是调用Mock数据，所以每次进入详情页都重新拉取评论，列表闪烁，非常影响阅读体验。
问题本质上是：**没有可靠的“本地真相”，一切都绑死在网络结果上。**

### 解决方法

思路是：增加一层本地缓存，避免每次进入详情页都重新拉取评论。

因此设计了不同的数据来源：

1. 单例化Repository 进行内存缓存
2. 经过翻阅文档, SharedPreferences 这种轻量级的键值对存储方式, 适合现在的评论存储
3. 网络数据，一次获取加载

#### Repository 内存缓存（CommentRepository.kt）

```kotlin
// 第11行：全局评论缓存
private val commentCache = mutableMapOf<Int, CommentData>()

// 第14-16行：优先返回缓存
suspend fun getComments(noteId: Int, cursor: String? = null): Result<CommentData> {
    if (commentCache.containsKey(noteId)) {
        return Result.success(commentCache[noteId] ?: CommentData(0, null, emptyList()))
    }
    // 缓存未命中才走网络
    // ...网络请求代码...
}
```

同一个 `noteId` 再次进入详情页时，如果内存里已经有数据，就可以秒级返回，避免重复请求和闪烁。

#### SharedPreferences 持久化（NoteDetailActivity.kt）

```kotlin
// 第58行：笔记独立存储文件，避免互相污染
sharedPreferences = getSharedPreferences("comments_${currentNote.id}", MODE_PRIVATE)

// 本地存储
private fun saveCommentsToLocal() {
    val json = gson.toJson(localComments)
    sharedPreferences.edit().putString("comments", json).apply()
}

// 本地加载
private fun loadCommentsFromLocal() {
    val json = sharedPreferences.getString("comments", "[]")
    val type = object : TypeToken<List<Comment>>() {}.type
    val savedComments = gson.fromJson<List<Comment>>(json, type) ?: emptyList()
    localComments.clear()
    localComments.addAll(savedComments)
}
```

#### 合并显示逻辑（NoteDetailActivity.kt）

最终在 `loadComments()` 里把三层数据合并在一起, 核心实现时`buildAllComments()`

```kotlin
private fun buildAllComments(): List<Comment> {
    val allComments = mutableListOf<Comment>()
    allComments.addAll(serverComments)
    val serverIds = serverComments.map { it.id }.toSet()
    // 本地缓存里的评论，不在服务器列表里的，才添加到顶部
    localComments.forEach { localComment ->
        if (!serverIds.contains(localComment.id)) {
            allComments.add(0, localComment)
        }
    }
    return allComments
}
```

发送评论时的逻辑则是“从 UI 往下推”：

```kotlin
private fun sendComment() {
    val newComment = Comment(...)
    localComments.add(0, newComment)  // 立即插入到列表顶部
    saveCommentsToLocal()  // 异步保存到 SharedPreferences
    commentRepository.addCommentToCache(currentNote.id, newComment)  // 更新内存缓存
        ...
    updateCommentDisplay()  // 刷新 UI
}
```

### 总结

1. 进入详情页：

    * 先用本地数据“占位”，保证列表即时有内容；
    * 再用网络数据补充、合并、去重。

2. 发送评论：

    * UI 立即更新，用户能立刻看到自己刚发的内容, 并且加载最上方；
    * 同时写入 SharedPreferences + Repository 缓存，保证之后再进页面也还在。

# 项目反思

这次开发让我深刻体会到，功能能跑通只是第一步，
真正的难点在于各种corner case和细节打磨。比如上拉加载的防抖，
不仅要判断是否在加载，还要考虑是否还有更多数据、是否正在刷新等多个状态联动；
再比如跨页面通信，看似简单，但要处理好返回键、生命周期等边界情况。
