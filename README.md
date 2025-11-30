# 抖音简版「经验」频道 - Android 客户端

## 项目简介

这是一个用 Kotlin 实现的「抖音经验频道」简版 Demo，主要完成了：

- 双列瀑布流浏览经验卡片（支持动态高度适配）
- 卡片点赞交互（首页 + 详情页）
- ApiFox Mock 数据驱动的网络图片展示
- 下拉刷新 + 上拉加载更多
- 详情页图片轮播 + 评论列表 + 评论发布（本地存储）
- 布局在单列/双列间动态切换

> 说明：本仓库用于字节跳动工程训练营作业练习，代码中部分实现仍偏「Demo 级」，但已经实现了从数据 → 列表 → 详情 → 评论的完整链路。

---

## 功能特性

### 1. 瀑布流布局（双列 + 动态高度）

- 使用 `StaggeredGridLayoutManager(2, VERTICAL)` 实现双列瀑布流。
- 后端 Mock 数据提供 `coverWidth/coverHeight`，在 `NoteAdapter` 中根据比例设置 `ConstraintLayout` 的 `dimensionRatio`，实现封面高度自适配。
- 列表滑动流畅，图片不会被拉伸或压缩。

实际代码位置：

- `HomeFragment.kt` → `setupRecyclerView()`
- `NoteAdapter.kt` → `ImageView.loadWithRatio(...)`

### 2. 经验卡片 UI 组件

卡片展示内容：

- 封面图：网络图片，圆角处理，有占位图
- 标题：两行截断，超出显示省略号
- 用户信息：头像+用户名
- 点赞区域：心形图标 + 点赞数

对应实现：

- 布局：`app/src/main/res/layout/item_note.xml`
- 适配器：`NoteAdapter.kt` 中的 `NoteViewHolder.bind()`
- 图片加载与圆角/圆形：`NoteAdapter.kt` 中的 `loadWithRatio` / `loadCircular`

### 3. 点赞交互功能

- 首页卡片：
  - 点击心形图标切换实心/空心。
  - 点赞数即时 +1 / -1。
  - 状态保存在 `MainViewModel.localNotes` 中，分页加载/刷新时会用本地状态覆盖网络返回值，防止点赞状态被重置。

- 详情页底部点赞：
  - 操作 `currentNote`，用 `copy()` 更新点赞状态，退出详情时通过 `setResult()` 把最新 `Note` 回传给首页。
  - `HomeFragment` 通过 `ActivityResultContracts.StartActivityForResult` 接收 `NOTE`，调用 `viewModel.updateNote(updatedNote)` 同步列表状态。

相关代码：

- `MainViewModel.toggleLike(...)`
- `HomeFragment` 中 `noteDetailLauncher` 回调
- `NoteDetailActivity.toggleLike()` + `finishWithResult()`

### 4. 数据刷新机制（下拉刷新 + 上拉加载）

- 下拉刷新：
  - 使用 `SwipeRefreshLayout` 包裹 RecyclerView。
  - `setOnRefreshListener` 调用 `viewModel.loadFirstPage()`。
  - `MainViewModel.isLoading` 控制刷新动画。

- 上拉加载更多：
  - 在 `RecyclerView` 上添加 `OnScrollListener`，判断 `!canScrollVertically(1) && dy > 0` 时触发 `loadMore()`。
  - `MainViewModel` 维护 `currentPage/pageSize/hasMoreData/isLoadingMore`，与 `FeedRepository` 协作加载下一页数据并去重。

代码位置：

- `HomeFragment.setupSwipeRefresh()`
- `HomeFragment.setupRecyclerView()` 中的 `addOnScrollListener`
- `MainViewModel.loadFirstPage()` / `loadMore()`

### 5. 数据配置管理 + Mock 数据

- 统一配置 Mock API 地址： 后续可以做调整，目前是URL地址一致，后续可以调整后端接口获取真实数据
  `app/src/main/java/com/example/myapplication/utils/Constants.kt`
  ```kotlin
  object Constants {
      const val BASE_NOTE_URL = "https://m1.apifoxmock.com/m1/7447441-7181548-default/"
      const val BASE_NOTE_COMMENT_URL = "https://m1.apifoxmock.com/m1/7447441-7181548-default/"
  }
  ```

- Retrofit + GsonConverter 接入：

  `ApiService.kt`
  ```kotlin
  interface ApiService {
      @GET("note")
      suspend fun getFeed(): Response<FeedResponse>

      @GET("feed")
      suspend fun getComments(
          @Query("noteId") noteId: Int,
          @Query("cursor") cursor: String? = null
      ): Response<CommentResponse>
  }
  ```

- 数据模型：`Note.kt` / `Comment.kt`（Parcelable + Gson 注解）

### 6. 详情页：图片轮播

- 使用 `ViewPager2` 展示多图，支持左右滑动。
- 顶部小圆点指示器显示当前图片位置。
- 图片点击放大（预留接口，未实现）。

相关代码：

- `NoteDetailActivity.setupViewPager()`
- `ImageGalleryAdapter.kt`

### 7. 评论系统

#### 7.1 评论列表展示

- 每条评论显示头像、用户名、时间、内容、点赞数。
- 支持嵌套回复（回复的评论会显示在主评论下方）。
- 数据优先级：**本地缓存 > 网络缓存 > 网络请求**。

核心实现：`NoteDetailActivity.loadComments()`

```kotlin
lifecycleScope.launch {
    binding.progressBar.visibility = View.VISIBLE
    
    // 1. 先加载本地缓存（SharedPreferences）
    loadCommentsFromLocal()
    
    // 2. 再加载网络数据
    val result = commentRepository.getComments(noteId)
    binding.progressBar.visibility = View.GONE
    
    result.onSuccess { commentData ->
        serverComments = commentData.list
        val allComments = buildDisplayComments() // 本地+网络+回复嵌套
        binding.tvCommentCount.text = "共 ${allComments.size} 条评论"
        commentAdapter.submitList(allComments)
        commentRepository.addLocalCommentToCache(noteId, allComments)
    }.onFailure {
        val allComments = buildDisplayComments()
        binding.tvCommentCount.text = "共 ${allComments.size} 条评论"
        commentAdapter.submitList(allComments)
    }
}
```

#### 7.2 评论发布

- 底部输入框支持：
  - @用户（触发用户列表选择）
  - 表情 / 图片（预留 Toast 提示）
  - 发送按钮

- 发送流程（`NoteDetailActivity.sendComment()`）：
  1. 构造 `Comment`（含 `replyToUsername` 等字段）。
  2. 立即添加到 `localComments` 并刷新 UI。
  3. 异步写入 `SharedPreferences`。
  4. 添加到 `CommentRepository` 的内存缓存（单例 object）。

```kotlin
private fun sendComment() {
    val content = binding.etComment.text.toString().trim()
    if (content.isEmpty()) {
        Toast.makeText(this, "请输入评论内容", Toast.LENGTH_SHORT).show()
        return
    }
    
    val newComment = Comment(
        id = -System.currentTimeMillis(), // 本地临时 ID
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
    
    val allComments = buildDisplayComments()
    binding.tvCommentCount.text = "共 ${allComments.size} 条评论"
    commentAdapter.submitList(allComments)
    
    Toast.makeText(this, "评论发送成功", Toast.LENGTH_SHORT).show()
}
```

#### 7.3 回复功能

点击评论的「回复」按钮 → 记录 `replyToComment` → 输入框 hint 显示「回复 @xxx」 → 发送时带上 `replyToUsername` 和 `parentCommentId`。

相关代码：`CommentAdapter` 中的 `onReplyClick` 回调。

### 8. 布局灵活性：动态切换单列/双列

- 通过「双击第一个 Tab」触发切换。
- 切换时更换 `RecyclerView` 的 `LayoutManager`：
  - 双列：`StaggeredGridLayoutManager(2, VERTICAL)`
  - 单列：`LinearLayoutManager`
- 使用 `SharedPreferences` 记住用户偏好，下次打开自动应用。

实现位置：`HomeFragment.setupLayoutToggle()` / `setupLayoutManager()`

---

## 项目结构

```
app/src/main/java/com/example/myapplication/
│
├── data/                          # 数据层
│   ├── model/
│   │   ├── NoteModel.kt          # Note/Comment 数据类（Parcelable + Gson）
│   ├── repository/
│   │   ├── FeedRepository.kt     # 首页 Feed 接口
│   │   └── CommentRepository.kt  # 评论区接口 + 内存缓存（单例 object）
│   └── service/
│       └── ApiService.kt         # Retrofit API 定义
│
├── ui/                           # UI 层
│   ├── home/
│   │   ├── HomeFragment.kt       # 首页（瀑布流 + Tab + 下拉/上拉刷新）
│   │   └── adapter/
│   │       └── NoteAdapter.kt    # Note 列表适配器 + 图片加载扩展函数
│   ├── detail/
│   │   ├── NoteDetailActivity.kt # 详情页（图片轮播 + 评论 + 点赞）
│   │   └── adapter/
│   │       ├── CommentAdapter.kt # 评论适配器（支持嵌套回复）
│   │       └── ImageGalleryAdapter.kt
│   ├── main/
│   │   ├── MainActivity.kt       # MainActivity（容器，含底部导航）
│   │   └── MainViewModel.kt      # 首页 ViewModel（状态管理 + 点赞逻辑）
│   └── splash/
│       └── SplashActivity.kt     # 启动页
│
└── utils/                        # 工具类
    ├── Constants.kt              # API 地址常量
    └── NetworkUtils.kt           # Retrofit 单例构建
```

---

## 安装与运行

### 运行步骤

1. **Clone 项目**
   ```bash
   git clone https://github.com/mx-pai/TicTokDemo.git
   cd TicTokDemo
   ```

2. **用 Android Studio 打开**
   
   File → Open → 选择项目根目录，等待 Gradle 同步完成。

3. **配置 Mock 数据**

   默认使用 Apifox Mock 接口，无需配置。如果想换地址，修改：

   ```kotlin
   // app/src/main/java/com/example/myapplication/utils/Constants.kt
   object Constants {
       const val BASE_NOTE_URL = "https://your-mock-api.com/"
       const val BASE_NOTE_COMMENT_URL = "https://your-mock-api.com/"
   }
   ```

4. **运行到设备或模拟器**
   
   点击 Android Studio 的「Run」按钮，或命令行：
   
   ```bash
   ./gradlew installDebug
   ```

---

## 演示视频 + 开发总结

- **演示视频**：[Bilibili 链接](https://www.bilibili.com/video/BV1fBSTBVEmk)
- **问题分析总结**：`tech_summary.md`

---

## License

MIT License - 仅供学习与作业练习使用。

---

<p align="center">Made with ❤️ and Kotlin</p>
