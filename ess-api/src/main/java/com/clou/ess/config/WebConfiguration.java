package com.clou.ess.config;

import com.clou.ess.interceptor.TokenInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Web相关配置
 */
@EnableAsync
@EnableCaching
@EnableScheduling
@EnableEncryptableProperties
@Configuration
public class WebConfiguration extends WebMvcConfigurerAdapter {

    // 排除Token验证URL
    @Value("${web.exclude-path}")
    private String[] excludePath;

    // Token验证拦截器
    @Autowired
    private TokenInterceptor tokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor).addPathPatterns("/**").excludePathPatterns(excludePath);
        super.addInterceptors(registry);
    }

    /**
     * Jackson 消息处理器
     *
     * @param mapper
     * @return
     */
    @Bean
    @Order(1)
    @ConditionalOnClass(ObjectMapper.class)
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(ObjectMapper mapper) {
        // 配置自定义NULL值处理
        mapper.setSerializerFactory(mapper.getSerializerFactory().withSerializerModifier(new MyBeanSerializerModifier()));
        return new MappingJackson2HttpMessageConverter(mapper);
    }

    @Bean(name = "taskScheduler")
    public TaskScheduler taskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

}
