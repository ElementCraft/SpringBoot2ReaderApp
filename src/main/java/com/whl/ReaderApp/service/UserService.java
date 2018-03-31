package com.whl.ReaderApp.service;

import com.whl.ReaderApp.domain.User;
import com.whl.ReaderApp.tools.JsonUtils;
import com.whl.ReaderApp.tools.RedisKey;
import com.whl.ReaderApp.tools.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import static com.whl.ReaderApp.tools.RedisKey.USER;

/**
 * @author yyy
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class UserService {

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    /**
     * 用户注册处理
     *
     * @param user 用户实体
     * @return 处理结果
     */
    public Mono<Result> reg(User user) {
        String redisKey = RedisKey.of(USER);
        String jsonUser = JsonUtils.toString(user);

        return redisTemplate.opsForHash().hasKey(redisKey, user.getAccount())
                .flatMap(bo -> {
                    if (bo) {
                        return Mono.just(Result.error(1, "账号已存在"));
                    } else {
                        return redisTemplate.opsForHash().put(redisKey, user.getAccount(), jsonUser)
                                .map(flag -> {
                                    if (flag) {
                                        return Result.ok();
                                    } else {
                                        return Result.error(2, "数据库Save失败");
                                    }
                                })
                                .switchIfEmpty(Mono.just(Result.error(2, "数据库Save失败")));
                    }
                });
    }

    /**
     * 用户登录处理
     *
     * @param user 用户实体
     * @return 处理结果
     */
    public Mono<Result<Object>> login(User user) {
        String redisKey = RedisKey.of(USER);

        return redisTemplate.opsForHash().hasKey(redisKey, user.getAccount())
                .filter(bo -> bo)
                .flatMap(bo -> redisTemplate.opsForHash().get(redisKey, user.getAccount())
                        .filter(json -> (json != null) && !json.toString().isEmpty())
                        .map(json -> {
                            User dbUser = JsonUtils.toObject(json.toString(), User.class);

                            if (dbUser != null && dbUser.getPassword().equals(user.getPassword())) {
                                return Result.ok();
                            } else {
                                return Result.error(2, "密码不正确");
                            }
                        })
                        .switchIfEmpty(Mono.just(Result.error(1, "账号不存在"))))
                .switchIfEmpty(Mono.just(Result.error(1, "账号不存在")));
    }
}