package com.whl.ReaderApp.routes;

import com.whl.ReaderApp.domain.User;
import com.whl.ReaderApp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.BodyInserters.fromObject;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.badRequest;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

/**
 * 用户相关route
 *
 * @author yyy
 */
@Component
public class UserRoute {

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserService userService;

    @Bean
    RouterFunction<?> userRoutes() {

        return nest(path("/api/user"),
                route(POST("/register"), this::reg)
                        .andRoute(POST("/login"), this::login)
        );
    }

    /**
     * 用户注册处理
     *
     * @param request 请求
     * @return 响应结果
     */
    private Mono<ServerResponse> reg(ServerRequest request) {

        return request.bodyToMono(User.class)
                .filter(user -> user.getAccount() != null)
                .filter(user -> user.getPassword() != null)
                .flatMap(userService::reg)
                .flatMap(o -> ok().body(fromObject(o)))
                .switchIfEmpty(badRequest().build());
    }

    /**
     * 用户登录处理
     *
     * @param request 请求
     * @return 响应结果
     */
    private Mono<ServerResponse> login(ServerRequest request) {
        return request.bodyToMono(User.class)
                .filter(user -> user.getAccount() != null)
                .filter(user -> user.getPassword() != null)
                .flatMap(userService::login)
                .flatMap(o -> ok().body(fromObject(o)))
                .switchIfEmpty(badRequest().build());
    }
}
