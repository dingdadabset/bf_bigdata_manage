package com.dga.metadata.service;

import com.dga.metadata.entity.DataTheme;
import com.dga.metadata.repository.DataThemeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class MetadataReferenceDataInitializer {

    @Autowired
    private DataThemeRepository dataThemeRepository;

    @PostConstruct
    public void init() {
        if (!dataThemeRepository.findByStatusOrderBySortOrderAscThemeNameAsc("ACTIVE").isEmpty()) {
            return;
        }
        createTheme("交易主题", "订单、支付、退款等交易数据", 10);
        createTheme("用户主题", "账号、用户画像、行为等用户数据", 20);
        createTheme("运营主题", "活动、渠道、增长等运营分析数据", 30);
        createTheme("风控主题", "风险识别、审计、安全相关数据", 40);
    }

    private void createTheme(String name, String description, int sortOrder) {
        DataTheme theme = new DataTheme();
        theme.setThemeName(name);
        theme.setDescription(description);
        theme.setSortOrder(sortOrder);
        theme.setStatus("ACTIVE");
        dataThemeRepository.save(theme);
    }
}
