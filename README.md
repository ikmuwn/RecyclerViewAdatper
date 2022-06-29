# RecyclerViewAdapter
- Adapter, Holder 작성 DiffUtils 적용에 발생하는 보일러플레이트 제거
- Draggable, Infinite loop scroll 적용 간소화
- [RecyclerViewFragment.kt](https://github.com/ikmuwn/Mock-android/blob/master/app/src/main/java/kim/uno/mock/ui/recyclerview/fragment/RecyclerViewFragment.kt)
- [Mock-android](https://github.com/ikmuwn/Mock-android)

### use
- RecyclerViewAdapter
  ```kotlin
  val adapter: RecyclerViewAdapter = MockAdapter()
  recyclerView.adapter = MockAdapter()
  ```

- MockAdapter.kt
  ```kotlin
  class MockAdapter : RecyclerViewAdapter() {

      override fun onCreateHolder(viewType: Int): ViewHolder<*> {
          // viewType에 따라 multifier holder 처리하는 위치
          // return when(viewType)
          return MockHolder(adapter = this)
      }

  }
  ```

- MockHolder.kt
  ```kotlin
  class MockHolder(adapter: RecyclerViewAdapter) :
      RecyclerViewAdapter.ViewHolder<MockEntity>(adapter, R.layout.mock_holder) {

      private val binding = MockHolderBinding.bind(itemView)

      override fun onBindView(item: MockEntity, position: Int) {
          super.onBindView(item, position)
          binding.item = item
      }

  }
  ```

### Adapter.kt 추가하는 대신 RecyclerViewAdapter.Builder 사용
- `MockAdatper.kt` → `RecyclerViewAdapter.Builder().build()`
  ```kotlin
  val adapter = RecyclerViewAdapter.Builder()
      .addHolder(holder = MockHolder::class)
      .build()

  recyclerView.adapter = adapter
  ```

### 같은 방식으로 Infinite loop 적용
- `RecyclerViewAdapter` → `InfiniteRecyclerAdapter`
  ```kotlin
  val adapter = InfiniteRecyclerAdapter.Builder()
      .addHolder(holder = MockHolder::class)
      .build()

  recyclerView.adapter = adapter
  ```

### 같은 방식으로 Draggable 적용 
- `RecyclerViewAdapter` → `DraggableRecyclerAdapter`
  ```kotlin
  val adapter = DraggableRecyclerViewAdapter.Builder()
      .addHolder(holder = DraggableMockHolder::class)
      .build()

  recyclerView.adapter = adapter
  ```

- DraggableMockHolder.kt `Draggable` 인터페이스를 상속받음
  ```kotlin
  class DraggableMockHolder(adapter: RecyclerViewAdapter) :
      RecyclerViewAdapter.ViewHolder<MockEntity>(adapter, R.layout.mock_holder),
      DragHelperCallback.Draggable {

      private val binding = MockHolderBinding.bind(itemView)

      override fun onBindView(item: MockEntity, position: Int) {
          super.onBindView(item, position)
          binding.item = item

          // drag 시작 트리거를 row의 롱 클릭으로 지정
          binding.root.setOnLongClickListener {
              if (adapter is DraggableRecyclerAdapter) {
                  adapter.startDrag(this@DraggableMockHolder)
              }
              true
          }
      }

      // drag 가능여부. 같은 ViewHolder 중에서도 drag enabled를 제어할 수 있음
      override fun isDragEnabled() = true

      // drag 시작과 끝을 알 수 있음
      override fun onDragStateChanged(isSelected: Boolean) {
          super.onDragStateChanged(isSelected)
          binding.root.alpha = if (isSelected) 0.5f else 1f
      }

  }
  ```

### Set items
- notifyDataSetChange { } block을 사용하면 자동으로 DiffUtill 애니메이션이 적용됨
  ```kotlin
  fun mockList(mockList: List<MockEntity>) {

      // block을 통해 DiffUtil 동작
      adapter.notifyDataSetChange {
          it.clear()
          it.addAll(items = mockList)
      }
  }
  ```
  
### Set items - @ItemDiff, @ContentsDiff
- 아이템 객체의 instance가 달라지지 않는 경우에는 이 annotation 지정없이 편하게 DiffUtil 애니메이션이 동작하겠지만
- instance 비교를 신뢰할 수 없는 경우에는 주요 필드에 적용하여 원하는 콜백을 받을 수 있음
  ```kotlin
  data class MockEntity(
      @PrimaryKey(autoGenerate = true)
      @RecyclerViewAdapter.ItemDiff
      var id: Long = 0,
      @RecyclerViewAdapter.ContentsDiff
      var message: String?,
      val postTime: Long
  )
  ```
- DiffUtil은 annotation 별로 `onBindView` 호출이 분기된다
- `@ItemDiff` 해당 position의 item이 다른 경우 `onBindView(item, position)`
- `@ContentsDiff` 해당 position의 item은 같지만 contents 필드가 다른경우 `onBindView(item, position, payloads)`
  ```kotlin
  class MockHolder(adapter: RecyclerViewAdapter) :
      RecyclerViewAdapter.ViewHolder<MockEntity>(adapter, R.layout.mock_holder) {

      private val binding = MockHolderBinding.bind(itemView)

      override fun onBindView(item: MockEntity, position: Int) {
          super.onBindView(item, position)
          binding.item = item
      }

      // return 값으로 false가 반환되는 경우는 @ContentsDiff에 대한 예외로 판단하며
      // 이 경우에만 onBindView(item, position) 함수가 호출함
      override fun onBindView(item: MockEntity, position: Int, payloads: ArrayList<String>): Boolean {
          if (payloads.contains("message")) {
              binding.message.text = item.message
              return true
          }

          return false
      }

  }
  ```
