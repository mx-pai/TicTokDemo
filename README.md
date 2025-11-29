# 抖音简版「经验」频道 - Android 客户端

## 项目简介

这是一个用 Kotlin 实现的「抖音经验频道」简版 Demo，主要完成了：

- 双列瀑布流浏览经验卡片（支持动态高度适配）
- 卡片点赞交互（首页 + 详情页）
- Mock 数据驱动的网络图片展示
- 下拉刷新 + 上拉加载更多
- 详情页图片轮播 + 评论列表 + 评论发布（本地存储）
- 布局在单列/双列间动态切换

项目代码位于 `app/src/main/java/com/example/myapplication` 下。

> 说明：本仓库用于课程作业练习，代码中部分实现仍偏「Demo 级」，但已经实现了从数据 → 列表 → 详情 → 评论的完整链路。

---

## 功能特性对照作业要求

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
- 用户信息：头像（圆形）+ 用户名
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

- 统一配置 Mock API 地址：

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
          @Query("cursor") cursor: String? = null,
          @Query("limit") limit: Int = 20
      ): Response<CommentResponse>
  }
  ```

- `NetworkUtils` 内部有 `Retrofit` 实例缓存，支持多 baseUrl。

---

## 加分项实现

### 1. 图片预加载 + Glide 缓存

- 在首页 `NoteAdapter` 中，对详情页 gallery 可能用到的图片提前预加载：

  ```kotlin
  val galleryImages = note.images ?: listOf(note.cover)
  val ctx = binding.root.context
  galleryImages.forEach { url ->
      Glide.with(ctx)
          .load(url)
          .diskCacheStrategy(DiskCacheStrategy.ALL)
          .preload()
  }
  ```

- 详情页 ViewPager 再次 `load(url)` 时命中内存/磁盘缓存，避免明显的刷新与卡顿。

### 2. 布局灵活性：单列/双列切换

- 在 `HomeFragment` 中实现双击第一个 Tab（「经验」）切换单列/双列布局：

  ```kotlin
  private var isSingleColumn = false
  private var lastHomeTabClickTime = 0L
  private val doubleTapTimeout = 300L

  private fun handleTabClick(tab: TabLayout.Tab) {
      val now = SystemClock.elapsedRealtime()
      if (tab.position == 0) {
          if (now - lastHomeTabClickTime <= doubleTapTimeout) {
              setupLayoutManager() // 切换布局
              lastHomeTabClickTime = 0L
          } else {
              lastHomeTabClickTime = now
              viewModel.loadFirstPage()
          }
      } else {
          viewModel.loadFirstPage()
      }
  }
  ```

- `setupLayoutManager()` 根据 `isSingleColumn` 切换 `LinearLayoutManager` 和 `StaggeredGridLayoutManager`。

---

## 详情页 & 评论系统（扩展实现）

> 这一部分是我在作业要求上自定义的扩展，用于练习评论列表、本地缓存、回复折叠等场景。

### 1. 详情页内容与图片轮播

- 顶部 `ViewPager2` 展示多张图片，配合 `ImageGalleryAdapter`。
- 如果 `Note.images` 为空，则使用 `cover` 构造单图列表。
- 文本区域显示内容、时间 + 话题标签。

代码位置：

- `NoteDetailActivity.setupViewPager(...)`
- `ImageGalleryAdapter.kt`

### 2. 评论列表 + 本地缓存

- 数据来源：
  - 服务器 Mock：`CommentRepository.getComments(noteId)`
  - 本地：`SharedPreferences("comments_$noteId")` 存储 JSON

- 合并逻辑（本地优先，不覆盖服务器）：

  `NoteDetailActivity.buildAllComments()`:
  ```kotlin
  private fun buildAllComments(): List<Comment> {
      val all = mutableListOf<Comment>()
      all.addAll(localComments)
      val localIds = localComments.map { it.id }.toSet()
      serverComments.forEach { c ->
          if (!localIds.contains(c.id)) {
              all.add(c)
          }
      }
      return all
  }
  ```

- 显示时再根据 `parentCommentId` 进行「父 + 子」顺序展开，让回复紧跟在被回复评论后面，并在文案中带「回复 XXX：内容」提示。

### 3. 评论发送 & 回复

- 点击底部输入框输入内容，点击发送后：
  - 构造本地 `Comment`，包含 `replyToUsername` 和 `parentCommentId`（如果是回复）。
  - 加入 `localComments` 列表，序列化为 JSON 写入 SharedPreferences。
  - 调用 `updateCommentDisplay()`，重新计算展示列表并提交给 `CommentAdapter`。

- 回复别人的评论：
  - 在 `CommentAdapter` 的 `tvReply` 上设置回调，把当前被回复的评论传回 `NoteDetailActivity`。
  - Activity 中把 `replyToComment` 记录下来，同时修改输入框 hint 为「回复 XXX」。
  - 发送时带上 `replyToUsername` 和 `parentCommentId`，然后作为子评论折叠展示。

---

## 项目结构（简要）

```text
app/src/main/java/com/example/myapplication/
│
├── data/
│   ├── model/
│   │   └── NoteModel.kt          # Note / Comment / FeedResponse / CommentResponse
│   ├── repository/
│   │   ├── FeedRepository.kt     # 经验列表数据源
│   │   └── CommentRespository.kt # 评论数据源（单例 + 内存缓存）
│   └── service/
│       └── ApiService.kt         # Retrofit 接口
│
├── ui/
│   ├── main/
│   │   ├── MainActivity.kt       # 容器 Activity，底部导航
│   │   └── MainViewModel.kt      # 首页 Feed ViewModel
│   ├── home/
│   │   ├── HomeFragment.kt       # 「经验」频道主界面
│   │   └── adapter/
│   │       └── NoteAdapter.kt    # 经验卡片 Adapter，含图片预加载
│   ├── detail/
│   │   ├── NoteDetailActivity.kt # 笔记详情 + 评论逻辑
│   │   └── adapter/
│   │       ├── CommentAdapter.kt # 评论列表 Adapter（点赞/回复）
│   │       └── ImageGalleryAdapter.kt # 图片轮播 Adapter
│   └── splash/
│       └── SplashActivity.kt     # 启动页
│
└── utils/
    ├── Constants.kt              # Base URL / 常量
    └── NotworkUtils.kt           # Retrofit 单例/多 baseUrl 缓存
```

---

## 开发中遇到的主要技术问题与优化思路（摘要）

1. **瀑布流中图片高度不一致导致的布局错乱**
   - 问题：使用固定高度会拉伸图片或留白，影响体验。
   - 优化：后端返回图片原始宽高，前端通过 `ConstraintLayout.dimensionRatio` 动态适配高度，并在 Adapter 中做宽高比计算。

2. **分页加载后点赞状态丢失**
   - 问题：`loadMore()` 时直接用新列表覆盖旧列表导致用户点赞状态被覆盖。
   - 优化：在 `MainViewModel` 中维护 `localNotes`，每次请求新数据时用本地数据中的 `isLiked/likes` 覆盖新数据，并根据 `id` 去重。

3. **评论本地缓存与网络数据合并**
   - 问题：需要在同一列表中展示本地新发评论和网络 Mock 评论，且多次进入详情不丢本地数据。
   - 优化：使用 SharedPreferences 存本地评论 JSON；在 `NoteDetailActivity` 中通过 `localComments + serverComments` 合并；`CommentRepository` 使用内存缓存避免重复请求。

后续如果有时间，会继续朝更标准的 MVVM + Repository + Room 架构重构，把当前 Activity 中的部分状态迁移到 ViewModel 中，以提升可维护性和可测试性。

---

## 运行说明

1. 在 `Constants.kt` 中配置 Mock 服务地址（Apifox 或本地 FastAPI）。
2. 用 Android Studio 打开工程，Sync Gradle。
3. 连接模拟器或真机，运行 `app` 模块。
4. 进入「经验」频道体验瀑布流、点赞、刷新、布局切换和评论功能。

演示视频中建议：

- 先展示整体体验；
- 再讲清首页瀑布流 + 点赞 + 分页实现思路；
- 然后挑评论合并和布局切换这两个细节讲代码与设计权衡。

- 使用`ActivityResultLauncher`启动详情页
- 详情页返回时通过`setResult`传回更新后的Note对象
- ViewModel中`updateNote()`方法找到对应ID并更新
- LiveData自动通知观察者刷新UI

```kotlin
// 返回时更新
private val noteDetailLauncher = registerForActivityResult(...) { result ->
    result.data?.getParcelableExtra<Note>("NOTE")?.let { updatedNote ->
        viewModel.updateNote(updatedNote)
    }
}
```

**效果**: 任意页面点赞，所有页面状态实时同步

### 3. 评论数据本地持久化

**问题**: 网络请求失败时无法显示评论；发送的评论刷新后丢失

**解决方案**:

- 使用`SharedPreferences`存储评论JSON（简单场景适用）
- 每条笔记独立存储文件：`comments_${noteId}.xml`
- 加载时先读本地，再读网络，合并显示
- 发送评论时先存本地，再异步上传到服务器（Mock）

```kotlin
private fun loadCommentsFromLocal() {
    val json = sharedPreferences.getString("comments", "[]")
    val savedComments = gson.fromJson<List<Comment>>(json, type)
    localComments.addAll(savedComments)
}
```

**效果**: 离线可看评论，发送的评论立即显示不丢失

## 安装与运行

### 1. 克隆项目

```bash
git clone https://github.com/yourusername/tiktok-lite-experience.git
cd tiktok-lite-experience
```

### 2. 打开项目

- 使用Android Studio打开项目
- 等待Gradle同步完成

### 3. 运行

- 连接Android设备或启动模拟器
- 点击"Run"按钮运行应用

## 演示视频

[点击观看3分钟功能演示视频](https://www.bilibili.com/video/BV1xxxxx)

视频目录：

- 00:00-00:30 瀑布流浏览和点赞功能
- 00:30-01:00 下拉刷新和上拉加载
- 01:00-01:45 详情页和评论系统
- 01:45-02:30 单列/双列切换
- 02:30-03:00 代码结构讲解

## 未来优化方向

1. **数据库升级**: 使用Room替代SharedPreferences存储评论
2. **图片优化**: 支持WebP格式，减小图片体积
3. **懒加载**: 实现列表项懒加载，提升启动速度
4. **搜索功能**: 支持按关键词搜索笔记
5. **用户系统**: 接入登录和用户信息

## License

MIT License - 自由使用，欢迎Star和Fork

## 联系方式

- 项目作者: [你的名字]
- 邮箱: xuma5668@gmail.com
- GitHub: https://github.com/mx-pai

---

<p align="center">
  Made with ❤️ and Kotlin
</p>
