# 演示视频脚本（3-5分钟）

## 开场（0:00-0:15）

**画面**: App启动页 → 进入主界面
**旁白**:
"大家好，这是我开发的高仿抖音经验频道App，采用Kotlin语言，基于MVVM架构。接下来我将演示核心功能并讲解实现思路。"

## 第一部分：瀑布流与交互（0:15-1:00）

### 场景1：瀑布流浏览
**画面**: 手指滑动列表，展示不同高度的卡片
**旁白**:
"首先看瀑布流布局，使用StaggeredGridLayoutManager实现双列布局。每张卡片根据图片宽高比动态计算高度，保持原始比例。这是通过ConstraintLayout的dimensionRatio属性实现的。"
**代码展示**:
```kotlin
// NoteAdapter.kt
fun ImageView.loadWithRatio(url: String, width: Int, height: Int) {
    val ratio = "$width:$height"
    (layoutParams as ConstraintLayout.LayoutParams).dimensionRatio = ratio
    // ...
}
```

### 场景2：点赞交互
**画面**: 点击几个点赞按钮，爱心图标变色，数字变化
**旁白**:
"点击点赞图标会触发toggleLike函数，通过ViewModel更新数据，LiveData自动刷新列表。这里有个难点：详情页点赞后需要同步到列表页。"
**代码展示**:
```kotlin
// MainViewModel.kt
fun toggleLike(targetNote: Note) {
    val newNote = oldNote.copy(
        isLiked = !oldNote.isLiked,
        likes = if (oldNote.isLiked) oldNote.likes - 1 else oldNote.likes + 1
    )
    _notes.value = currentList
}
```

## 第二部分：刷新与加载（1:00-1:30）

### 场景3：下拉刷新
**画面**: 顶部下拉，出现刷新动画，数据更新
**旁白**:
"SwipeRefreshLayout实现下拉刷新，触发loadFirstPage重新加载第一页数据。我这里模拟了分页加载，每页16条。"

### 场景4：上拉加载更多
**画面**: 滑动到底部，自动加载更多内容
**旁白**:
"RecyclerView的OnScrollListener监听滚动位置，到底部时触发loadMore。这里实现了去重合并，避免重复数据。"

## 第三部分：详情页与评论（1:30-2:15）

### 场景5：进入详情页
**画面**: 点击卡片 → 进入详情页 → 左右滑动图片
**旁白**:
"点击卡片启动NoteDetailActivity，用ViewPager2实现图片轮播。点赞状态通过ActivityResult回传，保持同步。"

### 场景6：评论系统
**画面**: 
- 点击评论输入框，弹出扩展工具栏（@、图片、表情）
- 点击@按钮，选择用户
- 输入评论并发送
- 回复某条评论

**旁白**:
"评论系统实现了@用户、回复功能，数据保存在本地SharedPreferences。发送的评论立即显示，离线也能看。"
**代码展示**:
```kotlin
// NoteDetailActivity.kt
private fun sendComment() {
    val newComment = Comment(
        id = timestampId,
        userName = "当前用户",
        replyToUsername = replyToComment?.userName,
        content = content
    )
    localComments.add(0, newComment)
    saveCommentsToLocal() // 本地持久化
}
```

## 第四部分：布局切换（2:15-2:45）

### 场景7：单列/双列切换
**画面**: 双击第一个Tab，布局从双列变为单列，再双击变回双列
**旁白**:
"这是加分项：双击第一个Tab可以切换单列和双列布局。状态保存在SharedPreferences，下次打开记住选择。核心就是更换RecyclerView的LayoutManager。"
**代码展示**:
```kotlin
// HomeFragment.kt
private fun setupLayoutManager() {
    isSingleColumn = !isSingleColumn
    val newLayoutManager = if (isSingleColumn) {
        LinearLayoutManager(requireContext())
    } else {
        StaggeredGridLayoutManager(2, VERTICAL)
    }
    binding.recyclerView.layoutManager = newLayoutManager
}
```

## 第五部分：技术总结（2:45-3:00）

**画面**: 代码结构图
**旁白**:
"项目采用MVVM架构，数据层用Repository模式，UI层用ViewBinding。三个技术难点：瀑布流高度自适应、跨页面状态同步、评论本地持久化，都通过合理设计解决了。"

## 结尾（3:00-3:05）

**画面**: App主界面
**旁白**:
"这就是完整的抖音经验频道实现，感谢观看！代码已上传GitHub，欢迎Star。"

---

## 录制建议

1. **屏幕录制**: 使用Android Studio的Screen Record功能
2. **代码高亮**: 使用IDE的Presentation Mode
3. **光标放大**: 使用Mouse Pointer Spotlight工具
4. **语速**: 每分钟150-180字
5. **画质**: 1080p 30fps以上
