package com.whl.ReaderApp.routes;

import com.whl.ReaderApp.domain.Book;
import com.whl.ReaderApp.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.springframework.web.reactive.function.BodyExtractors.toMultipartData;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.badRequest;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

/**
 * 书籍相关route
 *
 * @author yyy
 */
@Component
public class BookRoute {

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Autowired
    private BookService bookService;

    @Bean
    RouterFunction<?> bookRoutes() {

        return nest(path("/api/book"),
                route(GET("/search/{word}"), this::search)
                        .andRoute(POST("/add"), this::add)
                        .andRoute(GET("/shop/{account}"), this::getShop)
                        .andRoute(POST("/shop/{account}/{bookName}/{author}/{score}"), this::addToShop)
                        .andRoute(DELETE("/shop/{account}/{bookName}/{author}"), this::delFromShop)
                        .andRoute(POST("/search/history/{account}/{word}"), this::addSearchHistory)
                        .andRoute(GET("/search/history/{account}"), this::getSearchHistory)
                        .andRoute(DELETE("/search/history/{account}"), this::delSearchHistory)
                        .andRoute(POST("/upload").and(accept(MediaType.MULTIPART_FORM_DATA)), this::upload)
        );
    }

    /**
     * 获取购物车数据
     *
     * @param request 请求
     * @return 响应
     */
    private Mono<ServerResponse> getShop(ServerRequest request) {
        String account = request.pathVariable("account");

        return Optional.of(account)
                .filter(o -> !o.isEmpty())
                .map(acc -> bookService.getShop(acc))
                .map(o -> o.flatMap(t -> ok().body(fromObject(t)))
                        .switchIfEmpty(badRequest().build()))
                .orElse(badRequest().build());
    }

    /**
     * 从购物车删除
     *
     * @param request 请求
     * @return 响应结果
     */
    private Mono<ServerResponse> delFromShop(ServerRequest request) {
        String account = request.pathVariable("account");
        String bookName = request.pathVariable("bookName");
        String author = request.pathVariable("author");

        return Optional.of(account)
                .filter(o -> !o.isEmpty())
                .map(acc -> bookService.delFromShop(acc, bookName, author))
                .map(o -> o.flatMap(t -> ok().body(fromObject(t)))
                        .switchIfEmpty(badRequest().build()))
                .orElse(badRequest().build());
    }

    /**
     * 添加到购物车
     *
     * @param request 请求
     * @return 响应结果
     */
    private Mono<ServerResponse> addToShop(ServerRequest request) {
        String account = request.pathVariable("account");
        String bookName = request.pathVariable("bookName");
        String author = request.pathVariable("author");
        Integer score = Integer.valueOf(request.pathVariable("score"));

        return Optional.of(account)
                .filter(o -> !o.isEmpty())
                .map(acc -> bookService.addToShop(acc, bookName, author, score))
                .map(o -> o.flatMap(t -> ok().body(fromObject(t)))
                        .switchIfEmpty(badRequest().build()))
                .orElse(badRequest().build());
    }

    /**
     * 清空用户搜索历史
     *
     * @param request 请求
     * @return 响应结果
     */
    private Mono<ServerResponse> delSearchHistory(ServerRequest request) {
        String account = request.pathVariable("account");

        return Optional.of(account)
                .filter(o -> !o.isEmpty())
                .map(acc -> bookService.delSearchHistory(acc))
                .map(o -> o.flatMap(t -> ok().body(fromObject(t)))
                        .switchIfEmpty(badRequest().build()))
                .orElse(badRequest().build());
    }

    /**
     * 获取用户搜索历史
     *
     * @param request 请求
     * @return 响应结果
     */
    private Mono<ServerResponse> getSearchHistory(ServerRequest request) {
        String account = request.pathVariable("account");

        return Optional.of(account)
                .filter(o -> !o.isEmpty())
                .map(acc -> bookService.getSearchHistory(acc))
                .map(o -> o.flatMap(t -> ok().body(fromObject(t)))
                        .switchIfEmpty(badRequest().build()))
                .orElse(badRequest().build());
    }

    /**
     * 保存用户搜索历史
     *
     * @param request 请求
     * @return 响应结果
     */
    private Mono<ServerResponse> addSearchHistory(ServerRequest request) {
        String account = request.pathVariable("account");
        String keyword = request.pathVariable("word");

        return Optional.of(account)
                .filter(o -> !o.isEmpty())
                .map(acc -> bookService.addSearchHistory(acc, keyword))
                .map(o -> o.flatMap(t -> ok().body(fromObject(t)))
                        .switchIfEmpty(badRequest().build()))
                .orElse(badRequest().build());

    }

    /**
     * 上传封面
     *
     * @param request 请求
     * @return 响应结果
     */
    private Mono<ServerResponse> upload(ServerRequest request) {
        return request.body(toMultipartData())
                .filter(data -> !data.isEmpty())
                .flatMap(bookService::upload)
                .flatMap(o -> ok().body(fromObject(o)))
                .switchIfEmpty(badRequest().build());
    }

    /**
     * 新增书籍
     *
     * @param request 请求
     * @return 响应结果
     */
    private Mono<ServerResponse> add(ServerRequest request) {

        return request.bodyToMono(Book.class)
                .filter(book -> book.getName() != null)
                .filter(book -> book.getAuthor() != null)
                .filter(book -> book.getImgIcon() != null)
                .filter(book -> book.getBrief() != null)
                .filter(book -> book.getPrice() != null)
                .flatMap(bookService::add)
                .flatMap(o -> ok().body(fromObject(o)))
                .switchIfEmpty(badRequest().build());
    }

    /**
     * 搜索书籍
     *
     * @param request 请求
     * @return 响应结果
     */
    private Mono<ServerResponse> search(ServerRequest request) {
        String keyword = request.pathVariable("word");

        return Optional.of(keyword)
                .filter(o -> !o.isEmpty())
                .map(bookService::search)
                .map(o -> o.flatMap(t -> ok().body(fromObject(t)))
                        .switchIfEmpty(badRequest().build()))
                .orElse(badRequest().build());
    }
}
