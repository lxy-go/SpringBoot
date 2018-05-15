package com.wdjr.springboot.repository;

import com.wdjr.springboot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

//继承jpaRepository
public interface UserRepository extends JpaRepository<User,Integer> {

}
