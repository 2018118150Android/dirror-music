package com.dirror.music.ui.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import com.dirror.music.MyApplication
import com.dirror.music.R
import com.dirror.music.databinding.DialogPlayMoreBinding
import com.dirror.music.ui.activity.FeedbackActivity

class PlayerMenuMoreDialog : Dialog {

    private var binding: DialogPlayMoreBinding = DialogPlayMoreBinding.inflate(layoutInflater)

    constructor(context: Context) : this(context, 0)

    constructor(context: Context, themeResId: Int) : super(context, R.style.style_default_dialog) {
        setContentView(binding.root)
        // 设置显示位置
        window?.setGravity(Gravity.BOTTOM)
        // 设置大小
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        // editView.setText("你好")
        // setCanceledOnTouchOutside(false)
    }

    private var speed = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        speed = MyApplication.musicBinderInterface?.getSpeed() ?: 1f


        refreshPitch()


        // 歌曲信息
        binding.itemSongInfo.setOnClickListener {
            SongInfoDialog(context).apply {
                MyApplication.musicBinderInterface?.getNowSongData()?.let { it1 -> setSongData(it1) }
                show()
            }
            // 自己消失
            dismiss()
        }

        // 降噪
        binding.switchNoiseSuppressor.setOnCheckedChangeListener { buttonView, isChecked ->
            // 开启降噪
            val audioSession = MyApplication.musicBinderInterface?.getAudioSessionId()?:0
            // toast("${audioSession}")
            // loge("你好audio: ${audioSession}")
            // AudioEffect.noiseSuppressor(audioSession, isChecked)
            // AudioEffect.automaticGainControl(audioSession, true)
        }

        binding.itemNoiseSuppressor.setOnClickListener {
            binding.switchNoiseSuppressor.isChecked = !binding.switchNoiseSuppressor.isChecked
        }

        binding.ivIncreasePitch.setOnClickListener {
            MyApplication.musicBinderInterface?.increasePitchLevel()
            refreshPitch()
        }

        binding.ivDecreasePitch.setOnClickListener {
            MyApplication.musicBinderInterface?.decreasePitchLevel()
            refreshPitch()
        }

        binding.itemSpeed.setOnClickListener {
            MyApplication.musicBinderInterface?.setSpeed(1f)
        }

//        clDialog.setOnClickListener {
//            dismiss()
//        }

        // 反馈
        binding.itemFeedback.setOnClickListener {
            val intent = Intent(MyApplication.context, FeedbackActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // 从 Content 跳转 Activity 要加 FLAG
            MyApplication.context.startActivity(intent)
            // 隐藏 Dialog
            dismiss()
        }


    }

    /**
     * 刷新 Pitch
     */
    private fun refreshPitch() {
        binding.tvPitch.text = MyApplication.musicBinderInterface?.getPitchLevel().toString()
    }
}