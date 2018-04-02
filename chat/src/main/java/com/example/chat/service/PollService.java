package com.example.chat.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.example.chat.base.Constant;
import com.example.chat.bean.ChatMessage;
import com.example.chat.events.MessageInfoEvent;
import com.example.chat.manager.UserManager;
import com.example.chat.util.LogUtil;
import com.example.commonlibrary.rxbus.RxBusManager;
import com.example.commonlibrary.utils.CommonLogger;

import java.util.List;
import java.util.concurrent.TimeUnit;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * 项目名称:    TestChat
 * 创建人:        陈锦军
 * 创建时间:    2016/11/13      17:00
 * QQ:             1981367757
 */

public class PollService extends Service {

        private Disposable disposable;

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
                return null;
        }


        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
                int time;
                if (intent != null) {
                        time = intent.getIntExtra(Constant.TIME, 10);
                } else {
                        time = 10;
                }
                Observable.interval(time, TimeUnit.SECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<Long>() {
                                @Override
                                public void onSubscribe(Disposable d) {
                                        disposable=d;
                                }

                                @Override
                                public void onNext(Long aLong) {
                                        dealWork();
                                }

                                @Override
                                public void onError(Throwable e) {
                                        if (e!=null) {
                                                CommonLogger.e("定时任务出错"+e.getMessage());
                                        }
                                }

                                @Override
                                public void onComplete() {

                                }
                        });
                return super.onStartCommand(intent, START_FLAG_RETRY, startId);
        }

        private void dealWork() {
                LogUtil.e("拉取单聊消息");
                BmobQuery<ChatMessage> query = new BmobQuery<>();
                if (UserManager.getInstance().getCurrentUser() != null) {
                        query.addWhereEqualTo(Constant.TAG_TO_ID, UserManager.getInstance().getCurrentUserObjectId());
                } else {
                        return;
                }
                query.addWhereEqualTo(Constant.TAG_MESSAGE_SEND_STATUS, Constant.SEND_STATUS_SUCCESS);
                query.addWhereEqualTo(Constant.TAG_MESSAGE_READ_STATUS, Constant.READ_STATUS_UNREAD);
//                按升序进行排序
                query.setLimit(50);
                query.order("createdAt");
                query.findObjects(new FindListener<ChatMessage>() {
                        @Override
                        public void done(List<ChatMessage> list, BmobException e) {
                                if (e == null) {
                                        LogUtil.e("1拉取单聊消息成功");
                                        if (list != null && list.size() > 0) {
                                                MessageInfoEvent messageInfoEvent=new MessageInfoEvent(MessageInfoEvent.TYPE_PERSON);
                                                messageInfoEvent.setChatMessageList(list);
                                                RxBusManager.getInstance().post(messageInfoEvent);
                                        }
                                }else {
                                        LogUtil.e("拉取单聊消息失败");
                                        LogUtil.e("在服务器上查询聊天消息失败：" +e.toString());
                                }
                        }
                });
        }


        @Override
        public void onDestroy() {
                if (disposable != null) {
                        disposable.dispose();
                }
                super.onDestroy();

        }
}
