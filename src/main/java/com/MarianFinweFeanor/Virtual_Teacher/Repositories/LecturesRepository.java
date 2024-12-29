package com.MarianFinweFeanor.Virtual_Teacher.Repositories;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Lectures;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LecturesRepository extends JpaRepository<Lectures, Long> {

}
