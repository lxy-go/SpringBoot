package com.wdjr.springboot.repositry;

import com.wdjr.springboot.bean.Book;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface BookRepositry extends ElasticsearchRepository<Book,Integer> {

    public List<Book> findByBookNameLike(String bookName);


}
