package com.whl.ReaderApp.service;

import com.whl.ReaderApp.domain.Book;
import com.whl.ReaderApp.tools.JsonUtils;
import com.whl.ReaderApp.tools.RedisKey;
import com.whl.ReaderApp.tools.Result;
import com.whl.ReaderApp.tools.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.whl.ReaderApp.tools.RedisKey.*;

/**
 * @author yyy
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class BookService {

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    /**
     * 搜索书籍
     *
     * @param keyword 关键字
     * @return 书籍数组
     */
    public Mono<List<Book>> search(String keyword) {
        String redisKey = RedisKey.of(BOOK);

        return redisTemplate.opsForHash().values(redisKey).collectList()
                .map(list -> list.stream()
                        .map(obj -> JsonUtils.toObject(obj.toString(), Book.class))
                        .filter(Objects::nonNull)
                        .filter(book -> book.getName().contains(keyword) || book.getAuthor().contains(keyword))
                        .collect(Collectors.toList())
                );
    }

    /**
     * 查询书籍
     *
     * @param name   书名
     * @param author 作者
     * @return
     */
    public Mono<Book> findOne(String name, String author) {
        String childKey = RedisKey.of(BOOK_CHILD, name, author);

        return findOneByChildKey(childKey);
    }

    /**
     * 查询书籍
     *
     * @param childKey 子KEY
     * @return
     */
    public Mono<Book> findOneByChildKey(String childKey) {
        String redisKey = RedisKey.of(BOOK);

        return redisTemplate.opsForHash().get(redisKey, childKey)
                .map(o -> JsonUtils.toObject(o.toString(), Book.class))
                .switchIfEmpty(Mono.empty());
    }

    /**
     * 新增书籍
     *
     * @param book 书籍实体
     * @return 结果
     */
    public Mono<Result<Object>> add(Book book) {
        String name = book.getName().trim();
        String author = book.getAuthor().trim();
        String imgUrl = book.getImgIcon();
        String brief = book.getBrief();
        Long price = book.getPrice();

        String redisKey = RedisKey.of(BOOK);
        String redisChildKey = RedisKey.of(BOOK_CHILD, name, author);

        if (name.isEmpty()) {
            return Mono.just(Result.error(3, "书名不能为空"));
        } else if (author.isEmpty()) {
            return Mono.just(Result.error(4, "作者不能为空"));
        } else if (imgUrl.isEmpty()) {
            return Mono.just(Result.error(5, "图片路径有误"));
        } else if (brief.isEmpty()) {
            return Mono.just(Result.error(6, "请填写简介"));
        } else if (price <= 0) {
            return Mono.just(Result.error(7, "书本价格有误"));
        }

        String jsonBook = JsonUtils.toString(book);

        return redisTemplate.opsForHash().hasKey(redisKey, redisChildKey)
                .flatMap(bo -> {
                    if (bo) {
                        return Mono.just(Result.error(1, "该书籍已存在"));
                    } else {
                        return redisTemplate.opsForHash().put(redisKey, redisChildKey, jsonBook)
                                .filter(Boolean::booleanValue)
                                .map(o -> Result.ok())
                                .log(redisChildKey)
                                .switchIfEmpty(Mono.just(Result.error(2, "数据库异常")));
                    }
                })
                .switchIfEmpty(Mono.just(Result.error(2, "数据库异常")));
    }

    /**
     * 上传封面文件
     *
     * @param multiValueMap 表单
     * @return 处理结果
     */
    public Mono<Result<?>> upload(MultiValueMap<String, Part> multiValueMap) {

        Map<String, Part> parts = multiValueMap.toSingleValueMap();
        if (parts.containsKey("file")) {
            FilePart part = (FilePart) parts.get("file");
            String ext = StringUtils.getFilenameExtension(part.filename());

            if (ext != null) {
                ext = ext.toLowerCase();
            }

            if (!"jpg".equals(ext) && !"gif".equals(ext) && !"png".equals(ext) && !"bmp".equals(ext)) {
                return Mono.just(Result.error(3, "不允许上传该格式的文件"));
            }

            String fileName = ZonedDateTime.now().toEpochSecond()
                    + "_" + Utils.randomString(6) + "." + ext;

            String filePath = "upload" + File.separator + fileName;

            // 目录
            File dir = new File("upload");
            if (!dir.exists()) {
                dir.mkdir();
            }

            File file = new File(filePath);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return part.transferTo(file).thenReturn(Result.ok(filePath));
        }

        return Mono.just(Result.error(1, "上传文件异常"));
    }

    /**
     * 添加用户搜索历史
     *
     * @param acc     帐号
     * @param keyword 搜索关键词
     * @return 是否成功
     */
    public Mono<Result<Object>> addSearchHistory(String acc, String keyword) {
        String redisKey = RedisKey.of(BOOK_SEARCH_HISTORY, acc);

        return redisTemplate.opsForZSet().add(redisKey, keyword, Instant.now().toEpochMilli())
                .flatMap(bo -> Mono.just(Result.ok()))
                .switchIfEmpty(Mono.just(Result.error(2, "数据库连接异常")));
    }

    /**
     * 查找用户搜索历史
     *
     * @param acc 用户帐号
     * @return 搜索历史数组
     */
    public Mono<List<String>> getSearchHistory(String acc) {
        String redisKey = RedisKey.of(BOOK_SEARCH_HISTORY, acc);
        Range range = Range.of(Range.Bound.inclusive(0L), Range.Bound.inclusive(-1L));

        return redisTemplate.opsForZSet().range(redisKey, range).collectList();
    }

    /**
     * 清空用户搜索历史
     *
     * @param acc 用户账号
     * @return 结果
     */
    public Mono<Result<Object>> delSearchHistory(String acc) {
        String redisKey = RedisKey.of(BOOK_SEARCH_HISTORY, acc);

        return redisTemplate.opsForZSet().delete(redisKey)
                .map(bo -> Result.ok())
                .switchIfEmpty(Mono.just(Result.error(1, "数据库连接异常")));
    }

    /**
     * 添加到购物车
     *
     * @param acc      用户账号
     * @param bookName 书名
     * @param author   作者
     * @return 结果
     */
    public Mono<Result<Object>> addToShop(String acc, String bookName, String author) {
        String redisKey = RedisKey.of(BOOK_SHOP, acc);
        String value = RedisKey.of(bookName, author);

        return redisTemplate.opsForZSet().add(redisKey, value, 1)
                .flatMap(bo -> Mono.just(Result.ok()))
                .switchIfEmpty(Mono.just(Result.error(2, "数据库连接异常")));
    }

    /**
     * 从购物车删除
     *
     * @param acc      用户账号
     * @param bookName 书名
     * @param author   作者
     * @return 结果
     */
    public Mono<Result<Object>> delFromShop(String acc, String bookName, String author) {
        String redisKey = RedisKey.of(BOOK_SHOP, acc);
        String value = RedisKey.of(bookName, author);

        return redisTemplate.opsForZSet().remove(redisKey, value)
                .flatMap(bo -> Mono.just(Result.ok()))
                .switchIfEmpty(Mono.just(Result.error(2, "数据库连接异常")));
    }

    /**
     * 查询购物车
     *
     * @param acc 账号
     * @return
     */
    public Mono<List<DefaultTypedTuple<Book>>> getShop(String acc) {
        String redisKey = RedisKey.of(BOOK_SHOP, acc);
        Range range = Range.of(Range.Bound.inclusive(0L), Range.Bound.inclusive(-1L));

        return redisTemplate.opsForZSet().rangeWithScores(redisKey, range).flatMap(o -> {
            DefaultTypedTuple<String> old = (DefaultTypedTuple<String>) o;
            return findOneByChildKey(old.getValue())
                    .map(book -> new DefaultTypedTuple(book, old.getScore()))
                    .switchIfEmpty(Mono.empty());
        }).collectList().filter(Objects::nonNull);
    }
}