package com.xxx.server.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxx.server.mapper.WeixinDictionaryMapper;
import com.xxx.server.pojo.WeixinDictionary;
import com.xxx.server.service.IWeixinDictionaryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lc
 * @since 2022-07-16
 */
@Service
public class WeixinDictionaryServiceImpl extends ServiceImpl<WeixinDictionaryMapper, WeixinDictionary> implements IWeixinDictionaryService {

    @Cacheable(key = "'dic' + #p0.dicGroup + #p0.dicCode + #p0.dicKey", cacheNames = "dic")
    public List<WeixinDictionary> query(WeixinDictionary weixinDictionary){
        return list(Wrappers.lambdaQuery(WeixinDictionary.class)
                .eq(StrUtil.isNotEmpty(weixinDictionary.getDicGroup()),WeixinDictionary::getDicGroup, weixinDictionary.getDicGroup())
                .eq(StrUtil.isNotEmpty(weixinDictionary.getDicCode()),WeixinDictionary::getDicCode, weixinDictionary.getDicCode())
                .eq(StrUtil.isNotEmpty(weixinDictionary.getDicKey()),WeixinDictionary::getDicKey, weixinDictionary.getDicKey()));
    }

    @CacheEvict(cacheNames = "dic", allEntries = true)
    public boolean saveOrUpdateBatch(List<WeixinDictionary> weixinDictionaries){
         return super.saveOrUpdateBatch(weixinDictionaries);
    }
}
