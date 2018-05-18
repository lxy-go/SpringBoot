package com.wdjr.springboot;

import com.wdjr.springboot.bean.Article;
import com.wdjr.springboot.bean.Book;
import com.wdjr.springboot.repositry.BookRepositry;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringBoot03ElasticApplicationTests {

    @Autowired
    JestClient jestClient;

    @Autowired
    BookRepositry bookRepositry;

    @Test
    public void testindex(){

        Book book1 = new Book(2, "金瓶梅", "王尼玛");
        Book book2 = new Book(3, "金刚经", "方丈");
        Book book3 = new Book(4, "乾坤大挪移", "张无忌");
        Book book4 = new Book(5, "影分身之术", "鸣人");
        for (Book book : Arrays.asList(book1, book2, book3, book4)) {
            bookRepositry.index(book);
        }
        ;
    }

    @Test
    public void testSearch(){
        for (Book book : bookRepositry.findByBookNameLike("金")) {
            System.out.println(book);
        }

    }

    @Test
    public void contextLoads() {
        //1、给Es中索引（保存）一个文档
        Article article = new Article();
        article.setId(2);
        article.setTitle("好消息");
        article.setAutor("zhangsan");
        article.setContent("Hello World");
        //构建一个索引功能
        Index index = new Index.Builder(article).index("wdjr").type("article").build();

        try {
            //执行
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    @Test
    public void search(){
        //查询表达式
        String json = "{\n" +
                "    \"query\" : {\n" +
                "        \"match\" : {\n" +
                "            \"content\" : \"Hello\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        //构建搜索操作
        Search search = new Search.Builder(json).addIndex("wdjr").addType("article").build();

        //执行
        try {
            SearchResult result = jestClient.execute(search);
            System.out.println(result.getJsonString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
