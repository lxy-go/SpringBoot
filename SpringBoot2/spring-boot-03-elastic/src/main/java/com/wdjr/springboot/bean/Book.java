package com.wdjr.springboot.bean;

import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "wdjr",type="book")
public class Book {

    private Integer id;
    private String bookName;
    private String auto;


    public Book() {
        super();
    }

    public Book(Integer id, String bookName, String auto) {
        super();
        this.id = id;
        this.bookName = bookName;
        this.auto = auto;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getAuto() {
        return auto;
    }

    public void setAuto(String auto) {
        this.auto = auto;
    }

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", bookName='" + bookName + '\'' +
                ", auto='" + auto + '\'' +
                '}';
    }
}
