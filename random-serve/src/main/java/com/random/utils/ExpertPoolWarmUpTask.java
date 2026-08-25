package com.random.utils;

import com.random.mapper.expert.ExpertInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务，每天2点预热全局专家缓存池
 * @author hyf
 * @since 2026/8/25
 */
@Slf4j
@Component
public class ExpertPoolWarmUpTask {
    @Autowired
    private ExpertInfoMapper expertInfoMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Scheduled(cron = "0 0 2 * * ?")
    public void warmUpAllPools(){
        log.info("开始预热全局专家缓存池..");
        // 获取所有条件组合
        List<Map<String, String>> combinations = expertInfoMapper.getDistinctCombinations();
        int total = 0;
        for(Map<String, String> comb : combinations){
            String apply_type = comb.get("apply_type");
            String technical_type = comb.get("technical_type");
            String level = comb.get("level");

            String poolKey = "pool:" + apply_type + ":" + technical_type + ":" +level;
            // 获取符合查询条件的30天内可抽取的专家id
            List<Long> ids = expertInfoMapper.getExtractableExpertIds(apply_type,technical_type,level);
            if(!ids.isEmpty()){
                // 转成String数组存入Redis Set
                String[] isArray = ids.stream().map(String::valueOf).toArray(String[]::new);
                stringRedisTemplate.opsForSet().add(poolKey,isArray);
                // 设置过期时间30天
                stringRedisTemplate.expire(poolKey,30, TimeUnit.DAYS);
                total += ids.size();
                log.info("预热池: {}, 数量: {}",poolKey,ids.size());
            }
        }
        log.info("专家池预热完成，专家总数: {}",total);

    }

    @PostConstruct
    public void init() {
        log.info("应用启动，立即执行一次缓存池预热...");
        warmUpAllPools();
    }

}
