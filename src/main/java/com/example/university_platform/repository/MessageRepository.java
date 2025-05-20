package com.example.university_platform.repository;


import com.example.university_platform.entity.MessageEntity;
import com.example.university_platform.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    // Найти сообщения, где пользователь является отправителем или получателем
    List<MessageEntity> findBySenderOrReceiverOrderByTimestampDesc(UserEntity sender, UserEntity receiver);

    // Найти сообщения, отправленные указанным пользователем
    List<MessageEntity> findBySenderOrderByTimestampDesc(UserEntity sender);

    // Найти сообщения, полученные указанным пользователем
    List<MessageEntity> findByReceiverOrderByTimestampDesc(UserEntity receiver);

    List<MessageEntity> findAllByOrderByTimestampDesc();
}