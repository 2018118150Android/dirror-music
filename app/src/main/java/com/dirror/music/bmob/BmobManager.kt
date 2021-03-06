package com.dirror.music.bmob

import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.SaveListener
import com.dirror.music.util.toast

/**
 * @author 24568
 */
class BmobManager {

    /**
     * 上传反馈数据
     * @param feedback
     * @param contact
     */
    fun uploadFeedback(feedback: String?, contact: String?, success: () -> Unit, failure: () -> Unit) {
        val feedbackData = FeedbackData()
        feedbackData.feedback = feedback
        feedbackData.contact = contact
        feedbackData.save(object : SaveListener<String>() {
            override fun done(objectId: String?, ex: BmobException?) {
                if (ex == null) {
                    success.invoke()
                } else {
                    failure.invoke()
                }
            }
        })
    }

}