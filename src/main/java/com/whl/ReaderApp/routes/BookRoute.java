package com.whl.ReaderApp.routes;

import com.whl.ReaderApp.domain.Book;
import com.whl.ReaderApp.domain.User;
import com.whl.ReaderApp.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.Optional;

import static org.springframework.web.reactive.function.BodyExtractors.toDataBuffers;
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
                .andRoute(POST("/upload").and(accept(MediaType.MULTIPART_FORM_DATA)), this::upload)
        );
    }

    /**
     * 上传封面
     *
     * @param request 请求
     * @return 响应结果
     */
    private Mono<ServerResponse> upload(ServerRequest request) {
        return request.body(toMultipartData())
                .filter(data-> !data.isEmpty())
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
                .filter(user -> user.getName() != null)
                .filter(user -> user.getAuthor() != null)
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
