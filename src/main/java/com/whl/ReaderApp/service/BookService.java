package com.whl.ReaderApp.service;

import com.whl.ReaderApp.domain.Book;
import com.whl.ReaderApp.tools.JsonUtils;
import com.whl.ReaderApp.tools.RedisKey;
import com.whl.ReaderApp.tools.Result;
import com.whl.ReaderApp.tools.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.whl.ReaderApp.tools.RedisKey.BOOK;
import static com.whl.ReaderApp.tools.RedisKey.BOOK_CHILD;

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
     * 新增书籍
     *
     * @param book 书籍实体
     * @return 结果
     */
    public Mono<Result<Object>> add(Book book) {
        String redisKey = RedisKey.of(BOOK);
        String redisChildKey = RedisKey.of(BOOK_CHILD, book.getName(), book.getAuthor());
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
            String ext = StringUtils.getFilenameExtension(part.filename()).toLowerCase();

            if (!"jpg".equals(ext) && !"gif".equals(ext) && !"png".equals(ext) && !"bmp".equals(ext)) {
                return Mono.just(Result.error(3, "不允许上传该格式的文件"));
            }

            String fileName = ZonedDateTime.now().toEpochSecond()
                    + "_" + Utils.randomString(6) + "." + ext;

            String filePath = "upload" + File.separator + fileName;

            // 目录
            File dir = new File("upload");
            if(!dir.exists()){
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
}