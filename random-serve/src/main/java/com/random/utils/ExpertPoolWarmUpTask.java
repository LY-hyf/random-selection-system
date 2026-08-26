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
 * 定时任务，每天2点先删除旧缓存池再预热全局专家缓存池
 * 基于抽取记录统计热门组合 + 只预热这些组合；如果抽取记录太少，回退到全量专家表
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
        log.info("开始智能预热全局专家缓存池（基于抽取记录统计）..");
        // 1. 统计最近 30 天最热门的 Top 20 组合
        List<Map<String, Object>> hotCombos = expertInfoMapper.getHotCombinations(30, 20);

        if (hotCombos == null || hotCombos.isEmpty()) {
            // 如果没有抽取记录，回退到全量组合
            log.warn("无抽取记录，回退到全量组合预热");
            warmUpAllPools();
            return;
        }
        int total = 0;
        for(Map<String, Object> combo : hotCombos){
            String applyType = (String) combo.get("apply_type");
            String techType = (String) combo.get("technical_type");
            String level = (String) combo.get("level");
            Long count = (Long) combo.get("extract_count");
            // 构建缓存池的key
            String poolKey = "pool:" + applyType + ":" + techType + ":" + level;
            // 先删除旧池子，防止残留
            stringRedisTemplate.delete(poolKey);
            // 再查新数据并写入,获取符合查询条件的30天内可抽取的专家id
            List<Long> ids = expertInfoMapper.getExtractableExpertIds(applyType,techType,level);
            if(!ids.isEmpty()){
                // 转成String数组存入Redis Set
                String[] isArray = ids.stream().map(String::valueOf).toArray(String[]::new);
                // add方法添加
                stringRedisTemplate.opsForSet().add(poolKey,isArray);
                // 设置过期时间30天
                stringRedisTemplate.expire(poolKey,30, TimeUnit.DAYS);
                total += ids.size();
                log.info("预热热门池: {}, 数量: {}, 历史抽取次数: {}", poolKey, ids.size(), count);
            }
        }
        log.info("智能预热完成，共预热 {} 个组合，总数: {}", hotCombos.size(), total);

    }

    @PostConstruct
    public void init() {
        log.info("应用启动，立即执行一次缓存池预热...");
        warmUpAllPools();
    }

}
