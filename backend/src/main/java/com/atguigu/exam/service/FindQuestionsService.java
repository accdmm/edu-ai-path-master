package com.atguigu.exam.service;

import java.util.List;

public interface FindQuestionsService {

    List<String> findQuestions(String name, String difficult);

}