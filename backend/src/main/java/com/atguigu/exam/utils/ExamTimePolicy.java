package com.atguigu.exam.utils;

import java.time.LocalDateTime;

/**
 * 考试时长策略：判断一场考试是否已超时
 * duration 为空/非正数的试卷视为不限时
 */
public final class ExamTimePolicy {

    /** 交卷宽限分钟数：容纳页面加载、最后一题提交等耗时 */
    public static final int GRACE_MINUTES = 5;

    private ExamTimePolicy() {
    }

    /**
     * @param startTime       开始时间
     * @param durationMinutes 试卷时长（分钟），null 或 <=0 视为不限时
     * @param now             当前时间
     * @return true 表示已超过 时长+宽限
     */
    public static boolean isOvertime(LocalDateTime startTime, Integer durationMinutes, LocalDateTime now) {
        if (startTime == null || durationMinutes == null || durationMinutes <= 0 || now == null) {
            return false;
        }
        return now.isAfter(startTime.plusMinutes(durationMinutes + GRACE_MINUTES));
    }
}
