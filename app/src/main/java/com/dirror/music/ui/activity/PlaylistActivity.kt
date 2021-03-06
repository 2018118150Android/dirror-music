package com.dirror.music.ui.activity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dirror.music.MyApplication
import com.dirror.music.R
import com.dirror.music.adapter.DetailPlaylistAdapter
import com.dirror.music.databinding.ActivityPlaylistBinding
import com.dirror.music.music.netease.Playlist
import com.dirror.music.music.netease.PlaylistUtil
import com.dirror.music.music.standard.SongPicture
import com.dirror.music.music.standard.data.SOURCE_DIRROR
import com.dirror.music.music.standard.data.SOURCE_NETEASE
import com.dirror.music.music.standard.data.StandardPlaylistData
import com.dirror.music.music.standard.data.StandardSongData
import com.dirror.music.ui.dialog.PlaylistDialog
import com.dirror.music.util.*
import com.dirror.music.util.GlideUtil
import com.google.gson.Gson
import jp.wasabeef.glide.transformations.BlurTransformation

/**
 * 歌单 Activity
 * 最新要求：兼容 网易和 QQ
 */
class PlaylistActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLAYLIST_SOURCE = "int_playlist_source"
        const val EXTRA_LONG_PLAYLIST_ID = "int_playlist_id"
    }

    private lateinit var binding: ActivityPlaylistBinding

    private lateinit var musicBroadcastReceiver: MusicBroadcastReceiver // 音乐广播接收

    private var detailPlaylistAdapter = DetailPlaylistAdapter(ArrayList(), this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
        initView()
        initListener()
    }

    private fun initData() {
        val intentFilter = IntentFilter() // Intent 过滤器
        intentFilter.addAction("com.dirror.music.MUSIC_BROADCAST") // 只接收 "com.dirror.foyou.MUSIC_BROADCAST" 标识广播
        musicBroadcastReceiver = MusicBroadcastReceiver() //
        registerReceiver(musicBroadcastReceiver, intentFilter) // 注册接收器
    }

    private fun initView() {
        // 屏幕适配
        (binding.titleBar.layoutParams as ConstraintLayout.LayoutParams).apply {
            topMargin = getStatusBarHeight(window, this@PlaylistActivity)
        }
        (binding.includePlay.root.layoutParams as ConstraintLayout.LayoutParams).apply {
            bottomMargin = getNavigationBarHeight(this@PlaylistActivity)
        }
        // 获取歌单来源
        val playlistSource = intent.getIntExtra(EXTRA_PLAYLIST_SOURCE, 0)
        // 获取歌单 id
        val playlistId = intent.getLongExtra(EXTRA_LONG_PLAYLIST_ID, -1)

        binding.lottieLoading.repeatCount = -1
        binding.lottieLoading.playAnimation()

        initPlaylist(playlistSource, playlistId)

        var rvPlaylistScrollY = 0
        binding.rvPlaylist.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            rvPlaylistScrollY += oldScrollY
            if (rvPlaylistScrollY < 0) {
                if (binding.titleBar.text == "歌单") {
                    binding.titleBar.setTitleBarText(binding.tvName.text.toString())
                }
            } else {
                binding.titleBar.setTitleBarText("歌单")
            }
        }

    }

    private fun initListener() {
        binding.includePlay.root.setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
            overridePendingTransition(
                R.anim.anim_slide_enter_bottom,
                R.anim.anim_no_anim
            )
        }
        binding.includePlay.ivPlay.setOnClickListener {
            MyApplication.musicBinderInterface?.changePlayState()
        }
        binding.includePlay.ivPlaylist.setOnClickListener {
            PlaylistDialog(this).show()
        }

        /**
         * 全部播放
         * 播放第一首歌
         */
        binding.clNav.setOnClickListener {
            if (detailPlaylistAdapter.itemCount != 0) {
                detailPlaylistAdapter.playFirst()
            }
            // toast(detailPlaylistAdapter.itemCount.toString())
        }

    }

    private fun initPlaylist(source: Int, id: Long) {
        when (source) {
            SOURCE_NETEASE -> {
                // 加载歌单信息
                initPlaylistInfo(id)
                // 加载歌单歌曲
                initPlaylistSongs(id) {
                    initRecycleView(it)
                }
            }
            SOURCE_DIRROR -> {
                val url = "https://moriafly.xyz/dirror-music/json/music.json"
                MagicHttp.OkHttpManager().newGet(url, {
                    val playlistData = Gson().fromJson(it, StandardPlaylistData::class.java)
                    binding.tvName.text = playlistData.name
                    binding.tvDescription.text = playlistData.description
                    initRecycleView(playlistData.songs)
                }, {

                })
            }
        }
    }

    /**
     * 初始化歌单信息
     */
    private fun initPlaylistInfo(id: Long) {
        PlaylistUtil.getPlaylistInfo(id) {
            it.coverImgUrl?.let { url ->
                GlideUtil.load(url) { bitmap ->
                    runOnMainThread {
                        binding.ivCover.setImageBitmap(bitmap)
                        Glide.with(MyApplication.context)
                            .load(bitmap)
                            .placeholder(binding.ivBackground.drawable)
                            .apply(RequestOptions.bitmapTransform(BlurTransformation(50, 10)))
                            .into(binding.ivBackground)
                    }
                }

            }
            runOnMainThread {
                binding.tvName.text = it.name
                binding.tvDescription.text = it.description
            }
        }
    }

    inner class MusicBroadcastReceiver: BroadcastReceiver() {
        // 接收
        override fun onReceive(context: Context, intent: Intent) {
            refreshLayoutPlay()
            refreshPlayState()
        }
    }

    private fun refreshPlayState() {
        if (MyApplication.musicBinderInterface?.getPlayState() == true) {
            binding.includePlay.ivPlay.setImageResource(R.drawable.ic_bq_control_pause)
        } else {
            binding.includePlay.ivPlay.setImageResource(R.drawable.ic_bq_control_play)
        }
    }

    override fun onStart() {
        super.onStart()
        refreshLayoutPlay()
        refreshPlayState()
    }

    /**
     * 刷新下方播放框
     * 可能导致 stick 丢失
     */
    private fun refreshLayoutPlay() {
        MyApplication.musicBinderInterface?.getNowSongData()?.let { standardSongData ->
            binding.includePlay.tvName.text = standardSongData.name
            binding.includePlay.tvArtist.text = standardSongData.artists?.let { parseArtist(it) }
            SongPicture.getSongPicture(standardSongData, SongPicture.TYPE_LARGE) {
                binding.includePlay.ivCover.setImageBitmap(it)
            }
        }
    }

    private fun initPlaylistSongs(id: Long, success: (ArrayList<StandardSongData>) -> Unit) {
        Playlist.getPlaylist(id, {
            success.invoke(it)
        }, {

        })
    }

    @SuppressLint("SetTextI18n")
    private fun initRecycleView(songList: ArrayList<StandardSongData>) {
        runOnMainThread {
            binding.clLoading.visibility = View.GONE
            binding.rvPlaylist.layoutManager =  LinearLayoutManager(this@PlaylistActivity)
            // 改变全局变量
            detailPlaylistAdapter = DetailPlaylistAdapter(songList, this@PlaylistActivity)
            binding.rvPlaylist.adapter = detailPlaylistAdapter
            binding.tvPlayAll.text = "播放全部(${songList.size})"
            binding.lottieLoading.pauseAnimation()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 解绑
        unregisterReceiver(musicBroadcastReceiver)
    }

}