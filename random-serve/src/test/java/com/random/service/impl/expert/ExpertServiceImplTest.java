package com.random.service.impl.expert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.random.context.BaseContext;
import com.random.exception.BaseException;
import com.random.mapper.dict.SysDictMapper;
import com.random.mapper.expert.ExpertExtractRecordMapper;
import com.random.mapper.expert.ExpertImportRecordMapper;
import com.random.mapper.expert.ExpertInfoMapper;
import com.random.pojo.dto.expert.ExtractRequest;
import com.random.pojo.entity.expert.ExpertInfo;
import com.random.pojo.vo.expert.ExtractResultVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpertServiceImplTest {

    @Mock
    private ExpertInfoMapper expertInfoMapper;

    @Mock
    private ExpertExtractRecordMapper expertExtractRecordMapper;

    @Mock
    private ExpertImportRecordMapper expertImportRecordMapper;

    @Mock
    private SysDictMapper sysDictMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private ExpertServiceImpl expertService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Map<String, String> fakeRedis = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(1L);
        fakeRedis.clear();
        ReflectionTestUtils.setField(expertService, "objectMapper", objectMapper);

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenAnswer(inv -> fakeRedis.get(inv.getArgument(0)));
        doAnswer(inv -> {
            fakeRedis.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(valueOps).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void extract_结果集大于5时抽取5位() {
        when(expertInfoMapper.getExtractableExperts(any(), any(), any())).thenReturn(experts(10));

        ExtractResultVO result = expertService.extract(request());

        assertEquals(5, result.getExperts().size());
        assertFalse(result.getIsFromCache());
        assertNotNull(result.getBatchNo());
        verify(expertExtractRecordMapper, times(5)).insert(any());
    }

    @Test
    void extract_结果集不足5位时全部抽取() {
        when(expertInfoMapper.getExtractableExperts(any(), any(), any())).thenReturn(experts(3));

        ExtractResultVO result = expertService.extract(request());

        assertEquals(3, result.getExperts().size());
        verify(expertExtractRecordMapper, times(3)).insert(any());
    }

    @Test
    void extract_结果集为空时抛异常() {
        when(expertInfoMapper.getExtractableExperts(any(), any(), any())).thenReturn(new ArrayList<>());

        assertThrows(BaseException.class, () -> expertService.extract(request()));
    }

    @Test
    void extract_相同条件二次抽取命中缓存() {
        when(expertInfoMapper.getExtractableExperts(any(), any(), any())).thenReturn(experts(5));

        ExtractResultVO first = expertService.extract(request());
        assertFalse(first.getIsFromCache());

        ExtractResultVO second = expertService.extract(request());
        assertTrue(second.getIsFromCache());
        // 命中缓存，不应重复插入
        verify(expertExtractRecordMapper, times(5)).insert(any());
    }

    private ExtractRequest request() {
        ExtractRequest r = new ExtractRequest();
        r.setApplyType("medical");
        r.setTechnicalType("clinical_medicine");
        r.setLevel("senior");
        return r;
    }

    private List<ExpertInfo> experts(int n) {
        List<ExpertInfo> list = new ArrayList<>();
        for (long i = 1; i <= n; i++) {
            ExpertInfo e = new ExpertInfo();
            e.setId(i);
            e.setName("专家" + i);
            e.setApplyType("medical");
            e.setTechnicalType("clinical_medicine");
            e.setLevel("senior");
            list.add(e);
        }
        return list;
    }
}
