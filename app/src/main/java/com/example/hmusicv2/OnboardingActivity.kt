package com.example.hmusicv2

import android.content.Intent
import android.os.Bundle
import android.os.Handler // Thư viện để đếm giờ
import android.os.Looper // Thư viện hỗ trợ Handler chạy trên luồng chính
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {

    // 👇 CÁC BIẾN CHO TÍNH NĂNG TỰ ĐỘNG TRƯỢT 👇
    private val SLIDE_DELAY_MS = 3000L // Thời gian mỗi trang hiện ra (3000ms = 3 giây)
    private val INITIAL_SLIDE_DELAY_MS = 3000L // Thời gian chờ trước khi bắt đầu trượt trang đầu tiên
    private lateinit var viewPager: ViewPager2
    private lateinit var onboardingAdapter: OnboardingAdapter
    private val sliderHandler = Handler(Looper.getMainLooper()) // Bộ đếm giờ

    // Nhiệm vụ (Runnable) sẽ được Handler gọi định kỳ
    private val sliderRunnable = object : Runnable {
        override fun run() {
            // 1. Tính toán trang tiếp theo
            val currentItem = viewPager.currentItem
            val nextItem = if (currentItem == onboardingAdapter.itemCount - 1) {
                0 // Nếu đang ở trang cuối, quay lại trang đầu
            } else {
                currentItem + 1 // Nếu không, nhảy sang trang tiếp theo
            }

            // 2. Chuyển sang trang tiếp theo mượt mà
            viewPager.setCurrentItem(nextItem, true)

            // 3. Lên lịch gọi lại chính mình sau một khoảng thời gian
            sliderHandler.postDelayed(this, SLIDE_DELAY_MS)
        }
    }
    // 👆 KẾT THÚC CÁC BIẾN CHO TÍNH NĂNG TỰ ĐỘNG TRƯỢT 👆

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        // Ánh xạ các view
        viewPager = findViewById(R.id.viewPager)
        val btnGetStarted = findViewById<CardView>(R.id.btnGetStarted)

        // 👇 1. TẠO DỮ LIỆU VÀ GẮN VÀO VIEW PAGER 👇
        // (Đây là chỗ sau này em sẽ đổi icon thành ảnh thật của em nhe!)
        val onboardingItems = listOf(
            OnboardingItem(
                // Hiện tại đang dùng icon mặc định của hệ thống
                android.R.drawable.ic_media_play,
                "Lắng nghe giai điệu",
                "Tận hưởng hàng triệu bài hát chất lượng cao từ các nghệ sĩ hàng đầu thế giới."
            ),
            OnboardingItem(
                // Sau này em đổi thành R.drawable.ten_anh_cua_em là xong!
                android.R.drawable.ic_menu_myplaces,
                "Tạo Playlist riêng",
                "Tạo và chia sẻ danh sách phát mang đậm phong cách cá nhân của bạn."
            ),
            OnboardingItem(
                android.R.drawable.ic_menu_share,
                "Kết nối bạn bè",
                "Chia sẻ những bản nhạc yêu thích và khám phá gu âm nhạc của bạn bè."
            )
        )

        onboardingAdapter = OnboardingAdapter(onboardingItems)
        viewPager.adapter = onboardingAdapter
        // 👆 KẾT THÚC 1. TẠO DỮ LIỆU VÀ GẮN VÀO VIEW PAGER 👆

        // 2. Bấm nút "Bắt đầu ngay" -> Chuyển sang màn hình Đăng ký
        btnGetStarted.setOnClickListener {
            // Khi người dùng bấm nút, mình dừng bộ đếm giờ lại
            sliderHandler.removeCallbacks(sliderRunnable)

            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // 👇 3. LẮNG NGHE LÒNG CHẠY APP ĐỂ BẬT/TẮT BỘ ĐẾM GIỜ 👇
    override fun onResume() {
        super.onResume()
        // Khi app được mở ra và hiện lên trước mắt người dùng
        sliderHandler.postDelayed(sliderRunnable, INITIAL_SLIDE_DELAY_MS) // Bắt đầu đếm giờ
    }

    override fun onPause() {
        super.onPause()
        // Khi người dùng thoát ra ngoài, hoặc app bị ẩn đi
        sliderHandler.removeCallbacks(sliderRunnable) // Dừng bộ đếm giờ để đỡ tốn pin và lỗi app
    }
    // 👆 KẾT THÚC 3. LẮNG NGHE LÒNG CHẠY APP 👆
}

// ==========================================
// CÁC LỚP HỖ TRỢ (Giữ nguyên như cũ nhe em!)
// ==========================================
data class OnboardingItem(val image: Int, val title: String, val description: String)

class OnboardingAdapter(private val items: List<OnboardingItem>) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.imgOnboarding)
        val title: TextView = view.findViewById(R.id.tvTitle)
        val desc: TextView = view.findViewById(R.id.tvDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        val item = items[position]
        // Chỗ này mình giữ nguyên code, nó sẽ tự động lấy ảnh mới khi em thay dữ liệu
        holder.image.setImageResource(item.image)
        holder.title.text = item.title
        holder.desc.text = item.description
    }

    override fun getItemCount(): Int {
        return items.size
    }
}